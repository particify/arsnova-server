package de.thm.arsnova.event;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

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

	private static final String PREFIX = "backend.event.";
	private static final Logger logger = LoggerFactory.getLogger(AmqpEventDispatcher.class);

	private final RabbitTemplate messagingTemplate;
	private final Map<String, Set> eventConfig;

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
		logger.trace("Dispatching event ({}) for AMQP.", event.getClass().getSimpleName());
		String eventType = event.getClass().getSimpleName();
		eventType = eventType.substring(0, eventType.length() - 5);
		final String entityType = event.getEntity().getClass().getSimpleName();
		final String key = entityType + "-" + eventType;
		final Set<String> properties = eventConfig.getOrDefault(key, Collections.emptySet());

		if (!properties.isEmpty()) {
			final String exchangeName = makeQueueName(entityType, eventType);
			final ObjectMapper mapper = new ObjectMapper();
			final SimpleFilterProvider filterProvider = new SimpleFilterProvider();
			filterProvider.addFilter("amqpPropertyFilter", SimpleBeanPropertyFilter.filterOutAllExcept(properties));
			mapper.setFilterProvider(filterProvider);
			mapper.addMixIn(Entity.class, AmqpPropertyFilter.class);
			try {
				final String jsonPayload = mapper.writeValueAsString(event.getEntity());
				logger.debug("AMQP event payload: {}", jsonPayload);
				messagingTemplate.convertAndSend(exchangeName, "", jsonPayload);
			} catch (final JsonProcessingException e) {
				logger.error("Event serialization failed.", e);
			} catch (final AmqpException e) {
				logger.error("Could not send event to broker.", e);
			}
		}
	}
}
