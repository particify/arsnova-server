package de.thm.arsnova.websocket.handler;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import de.thm.arsnova.config.RabbitConfig;
import de.thm.arsnova.config.properties.MessageBrokerProperties;
import de.thm.arsnova.websocket.message.CreateFeedback;
import de.thm.arsnova.websocket.message.ResetFeedback;

@Service
@EnableConfigurationProperties(MessageBrokerProperties.class)
@ConditionalOnProperty(
		name = RabbitConfig.RabbitConfigProperties.RABBIT_ENABLED,
		prefix = MessageBrokerProperties.PREFIX,
		havingValue = "true")
public class FeedbackHandler {
	public static final String CREATE_FEEDBACK_COMMAND_QUEUE_NAME = "feedback.command";
	public static final String CREATE_FEEDBACK_RESET_COMMAND_QUEUE_NAME = "feedback.command.reset";
	public static final String QUERY_FEEDBACK_COMMAND_QUEUE_NAME = "feedback.query";

	private final FeedbackCommandHandler commandHandler;

	public FeedbackHandler(final FeedbackCommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}

	@RabbitListener(
			containerFactory = "myRabbitListenerContainerFactory",
			queues = CREATE_FEEDBACK_COMMAND_QUEUE_NAME)
	public void receiveMessage(
			final CreateFeedback value
	) throws Exception {

		commandHandler.handle(
				value
		);

	}

	@RabbitListener(
			containerFactory = "myRabbitListenerContainerFactory",
			queues = CREATE_FEEDBACK_RESET_COMMAND_QUEUE_NAME)
	public void receiveMessage(
			final ResetFeedback value
	) throws Exception {

		commandHandler.handle(
				value
		);

	}

}
