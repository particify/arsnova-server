package de.thm.arsnova.event;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.thm.arsnova.model.Entity;

/**
 * AmqpEventDispatcher listens to application events, converts them to messages and sends them to RabbitMQ.
 * Use the 'backend.' prefix for internal events that should not be sent to frontend clients.
 * Restrict messages to what is needed in other services as long as the arsnova-backend is owner of the functionality.
 */
@Component
public class AmqpEventDispatcher {
	@JsonFilter("amqpPropertyFilter")
	public static class AmqpPropertyFilter {
	}

	private static class MessagePublishingConfig {
		public String entityType;
		public String applicationEventType;
		public Set<String> attributes = new HashSet<>();
	}

	private static final Logger logger = LoggerFactory.getLogger(AmqpEventDispatcher.class);

	private final RabbitTemplate messagingTemplate;

	private List<MessagePublishingConfig> config = new ArrayList<>();

	@Autowired
	public AmqpEventDispatcher(final RabbitTemplate rabbitTemplate) {
		messagingTemplate = rabbitTemplate;
		final MessagePublishingConfig config = new MessagePublishingConfig();
		config.entityType = "Room";
		config.applicationEventType = "AfterCreationEvent";
		config.attributes.add("id");
		config.attributes.add("ownerId");
		this.config.add(config);
	}

	@EventListener
	public <T extends CrudEvent, E extends Entity> void dispatchEntityCrudEvent(final T event) {
		logger.trace("Dispatching event ({}) for AMQP.", event.getClass().getSimpleName());
		final String eventType = event.getClass().getSimpleName();
		final String entityType = event.getEntity().getClass().getSimpleName();

		for (final MessagePublishingConfig config : this.config) {
			if (config.applicationEventType.equals(eventType)) {
				final String exchangeName = "backend." + entityType.toLowerCase() + "." + eventType.toLowerCase();
				final ObjectMapper mapper = new ObjectMapper();
				final SimpleFilterProvider filterProvider = new SimpleFilterProvider();
				filterProvider.addFilter("amqpPropertyFilter",
						SimpleBeanPropertyFilter.filterOutAllExcept(config.attributes));
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
}
