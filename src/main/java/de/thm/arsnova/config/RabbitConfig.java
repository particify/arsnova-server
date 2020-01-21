package de.thm.arsnova.config;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;

import de.thm.arsnova.config.properties.MessageBrokerProperties;
import de.thm.arsnova.event.AmqpEventDispatcher;

@Configuration
public class RabbitConfig {
	private static final String RABBIT_ENABLED = "rabbitmq.enabled";
	private static final String RABBIT_MANAGE_DECLARATIONS = "rabbitmq.manage-declarations";

	@Bean
	@Autowired
	@ConditionalOnProperty(
			name = RABBIT_ENABLED,
			prefix = MessageBrokerProperties.PREFIX,
			havingValue = "true")
	public ConnectionFactory connectionFactory(
			@TaskExecutorConfig.RabbitConnectionExecutor final TaskExecutor executor,
			final MessageBrokerProperties messageBrokerProperties
	) {
		final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
				messageBrokerProperties.getRabbitmq().getHost(),
				messageBrokerProperties.getRabbitmq().getPort());
		connectionFactory.setUsername(messageBrokerProperties.getRabbitmq().getUsername());
		connectionFactory.setPassword(messageBrokerProperties.getRabbitmq().getPassword());
		connectionFactory.setVirtualHost(messageBrokerProperties.getRabbitmq().getVirtualHost());
		connectionFactory.setExecutor(executor);

		return connectionFactory;
	}

	@Bean
	@Autowired
	@ConditionalOnProperty(
			name = {RABBIT_ENABLED, RABBIT_MANAGE_DECLARATIONS},
			prefix = MessageBrokerProperties.PREFIX,
			havingValue = "true")
	public Declarables declarables(
			final MessageBrokerProperties messageBrokerProperties) {
		final Set<String> eventExchanges = messageBrokerProperties.getPublishedEvents().stream()
				.map(c -> AmqpEventDispatcher.makeQueueName(c.entityType, c.eventType)).collect(Collectors.toSet());
		return new Declarables(eventExchanges.stream()
				.map(FanoutExchange::new).collect(Collectors.toList()));
	}

	@Bean
	@Autowired
	@ConditionalOnProperty(
			name = RABBIT_ENABLED,
			prefix = MessageBrokerProperties.PREFIX,
			havingValue = "true")
	public RabbitTemplate rabbitTemplate(
			final ConnectionFactory connectionFactory,
			final MessageConverter messageConverter) {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(messageConverter);
		return rabbitTemplate;
	}

	@Bean
	@Autowired
	@ConditionalOnProperty(
			name = RABBIT_ENABLED,
			prefix = MessageBrokerProperties.PREFIX,
			havingValue = "true")
	public RabbitAdmin rabbitAdmin(final ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

	@Bean
	@ConditionalOnProperty(
			name = RABBIT_ENABLED,
			prefix = MessageBrokerProperties.PREFIX,
			havingValue = "true")
	public AmqpEventDispatcher eventToTopicPublisher(
			final RabbitTemplate rabbitTemplate,
			final MessageBrokerProperties messageBrokerProperties) {
		return new AmqpEventDispatcher(rabbitTemplate, messageBrokerProperties);
	}

	@Bean
	@ConditionalOnProperty(
			name = RABBIT_ENABLED,
			prefix = MessageBrokerProperties.PREFIX,
			havingValue = "true")
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	@ConditionalOnProperty(
			name = RABBIT_ENABLED,
			prefix = MessageBrokerProperties.PREFIX,
			havingValue = "true")
	public MappingJackson2MessageConverter mappingJackson2MessageConverter() {
		final MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		return converter;
	}

	@Bean
	@ConditionalOnProperty(
			name = RABBIT_ENABLED,
			prefix = MessageBrokerProperties.PREFIX,
			havingValue = "true")
	public DefaultMessageHandlerMethodFactory defaultMessageHandlerMethodFactory() {
		final DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
		factory.setMessageConverter(mappingJackson2MessageConverter());
		return factory;
	}
}
