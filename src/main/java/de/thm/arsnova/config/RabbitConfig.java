package de.thm.arsnova.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;

@Configuration
public class RabbitConfig {
	private static final String roomAfterCreationEventExchangeName = "backend.room.aftercreationevent";
	private static final String roomAfterDeletionEventExchangeName = "backend.room.afterdeletionevent";
	private static final String roomAfterFullUpdateEventExchangeName = "backend.room.afterfullupdateevent";
	private static final String roomAfterPatchEventExchangeName = "backend.room.afterpatchevent";
	private static final String roomAfterUpdateEventExchangeName = "backend.room.afterupdateevent";

	@Bean
	@Autowired
	public ConnectionFactory connectionFactory(
			@TaskExecutorConfig.RabbitConnectionExecutor final TaskExecutor executor
	) {
		final CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost", 5672);
		connectionFactory.setUsername("guest");
		connectionFactory.setPassword("guest");
		connectionFactory.setExecutor(executor);
		connectionFactory.setVirtualHost("/");

		return connectionFactory;
	}

	@Bean
	@Autowired
	public List<Declarable> declareExchanges(final RabbitAdmin rabbitAdmin) {
		final FanoutExchange roomAfterCreationEventExchange = new FanoutExchange(roomAfterCreationEventExchangeName);
		final FanoutExchange roomAfterDeletionEventExchange = new FanoutExchange(roomAfterDeletionEventExchangeName);
		final FanoutExchange roomAfterFullUpdateEventExchange = new FanoutExchange(roomAfterFullUpdateEventExchangeName);
		final FanoutExchange roomAfterPatchEventExchange = new FanoutExchange(roomAfterPatchEventExchangeName);
		final FanoutExchange roomAfterUpdateEventExchange = new FanoutExchange(roomAfterUpdateEventExchangeName);

		rabbitAdmin.declareExchange(roomAfterCreationEventExchange);
		rabbitAdmin.declareExchange(roomAfterDeletionEventExchange);
		rabbitAdmin.declareExchange(roomAfterFullUpdateEventExchange);
		rabbitAdmin.declareExchange(roomAfterPatchEventExchange);
		rabbitAdmin.declareExchange(roomAfterUpdateEventExchange);

		return Arrays.asList(
				roomAfterCreationEventExchange,
				roomAfterDeletionEventExchange,
				roomAfterFullUpdateEventExchange,
				roomAfterPatchEventExchange,
				roomAfterUpdateEventExchange
		);
	}

	@Bean
	@Autowired
	public RabbitTemplate rabbitTemplate(
			final ConnectionFactory connectionFactory,
			final MessageConverter messageConverter) {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(messageConverter);
		return rabbitTemplate;
	}

	@Bean
	@Autowired
	public RabbitAdmin rabbitAdmin(final ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public MappingJackson2MessageConverter mappingJackson2MessageConverter() {
		final MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		return converter;
	}

	@Bean
	public DefaultMessageHandlerMethodFactory defaultMessageHandlerMethodFactory() {
		final DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
		factory.setMessageConverter(mappingJackson2MessageConverter());
		return factory;
	}
}
