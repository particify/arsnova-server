package de.thm.arsnova.event;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;

import de.thm.arsnova.config.properties.MessageBrokerProperties;
import de.thm.arsnova.model.Entity;

/**
 * AmqpEventDispatcher listens to application events, converts them to messages and sends them to RabbitMQ.
 * Events can be filtered via configuration to those needed by other services.
 * Use the 'backend.' prefix for internal events that should not be sent to frontend clients.
 *
 * @author Tom Käsler
 * @author Daniel Gerhardt
 */
public class AmqpEventDispatcher {
	@JsonFilter("amqpPropertyFilter")
	public static class AmqpPropertyFilter {
	}

	public static final String ROOM_DUPLICATION_EVENT_QUEUE_NAME = "backend.event.room.duplicated";

	private static final String PREFIX = "backend.event.";
	private static final Logger logger = LoggerFactory.getLogger(AmqpEventDispatcher.class);

	private final RabbitTemplate messagingTemplate;
	private final Map<String, Set> eventConfig;
	private final Map<String, ObjectMapper> objectMappers = new HashMap<>();

	@Autowired
	public AmqpEventDispatcher(
			final RabbitTemplate rabbitTemplate,
			final MessageBrokerProperties messageBrokerProperties) {
		messagingTemplate = rabbitTemplate;
		eventConfig = messageBrokerProperties.getPublishedEvents().stream().collect(Collectors.toMap(
				c -> c.entityType + "-" + c.eventType,
				c -> c.includedProperties));
	}

	public static String makeQueueName(final String entityType, final String eventType) {
		return PREFIX + entityType.toLowerCase() + "." + eventType.toLowerCase();
	}

	@EventListener
	public <T extends CrudEvent, E extends Entity> void dispatchEntityCrudEvent(final T event) {
		String eventType = event.getClass().getSimpleName();
		eventType = eventType.substring(0, eventType.length() - 5);
		final String entityType = event.getEntity().getSupertype().getSimpleName();
		final String key = entityType + "-" + eventType;
		logger.trace("Dispatching event ({}, {}) for AMQP.", eventType, entityType);
		final Set<String> properties = eventConfig.getOrDefault(key, Collections.emptySet());

		if (!properties.isEmpty()) {
			final String exchangeName = makeQueueName(entityType, eventType);
			try {
				final ObjectMapper mapper = createOrGetObjectMapper(exchangeName, properties);
				final byte[] jsonPayload = mapper.writeValueAsBytes(event.getEntity());
				logger.debug("AMQP event payload: {}", new String(jsonPayload));
				final Message message = MessageBuilder
						.withBody(jsonPayload)
						.setContentType(MediaType.APPLICATION_JSON_VALUE)
						.build();
				messagingTemplate.send(exchangeName, "", message);
			} catch (final JsonProcessingException e) {
				logger.error("Event serialization failed.", e);
			} catch (final AmqpException e) {
				logger.error("Could not send event to broker.", e);
			}
		}
	}

	@EventListener
	public void dispatchRoomDuplicationEvent(final RoomDuplicationEvent event) {
		messagingTemplate.convertAndSend(
				ROOM_DUPLICATION_EVENT_QUEUE_NAME,
				"",
				new RoomDuplicationMessage(event.getOriginalRoom().getId(), event.getDuplicateRoom().getId()));
	}

	private ObjectMapper createOrGetObjectMapper(final String exchangeName, final Set<String> properties) {
		if (objectMappers.keySet().contains(exchangeName)) {
			return objectMappers.get(exchangeName);
		}

		final ObjectMapper mapper = new ObjectMapper();
		final SimpleFilterProvider filterProvider = new SimpleFilterProvider();
		filterProvider.addFilter("amqpPropertyFilter", SimpleBeanPropertyFilter.filterOutAllExcept(properties));
		mapper.setFilterProvider(filterProvider);
		mapper.addMixIn(Entity.class, AmqpPropertyFilter.class);
		objectMappers.put(exchangeName, mapper);

		return mapper;
	}

	private class RoomDuplicationMessage {
		private String originalRoomId;
		private String duplicatedRoomId;

		private RoomDuplicationMessage(final String originalRoomId, final String duplicatedRoomId) {
			this.originalRoomId = originalRoomId;
			this.duplicatedRoomId = duplicatedRoomId;
		}

		public String getOriginalRoomId() {
			return originalRoomId;
		}

		public String getDuplicatedRoomId() {
			return duplicatedRoomId;
		}
	}
}
