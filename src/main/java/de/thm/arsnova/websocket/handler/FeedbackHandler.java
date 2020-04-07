package de.thm.arsnova.websocket.handler;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import de.thm.arsnova.config.RabbitConfig;
import de.thm.arsnova.config.properties.MessageBrokerProperties;
import de.thm.arsnova.websocket.message.CreateFeedback;
import de.thm.arsnova.websocket.message.GetFeedback;
import de.thm.arsnova.websocket.message.ResetFeedback;

@Service
@EnableConfigurationProperties(MessageBrokerProperties.class)
@ConditionalOnProperty(
		name = RabbitConfig.RabbitConfigProperties.RABBIT_ENABLED,
		prefix = MessageBrokerProperties.PREFIX,
		havingValue = "true")
public class FeedbackHandler {
	private final FeedbackCommandHandler commandHandler;

	@Autowired
	public FeedbackHandler(final FeedbackCommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}

	@RabbitListener(containerFactory = "myRabbitListenerContainerFactory", queues = "feedback.command")
	public void receiveMessage(
			final CreateFeedback value
	) throws Exception {

		commandHandler.handle(
				value
		);

	}

	@RabbitListener(containerFactory = "myRabbitListenerContainerFactory", queues = "feedback.command.reset")
	public void receiveMessage(
			final ResetFeedback value
	) throws Exception {

		commandHandler.handle(
				value
		);

	}

	@RabbitListener(containerFactory = "myRabbitListenerContainerFactory", queues = "feedback.query")
	public void receiveMessage(
			final GetFeedback value
	) throws Exception {

		commandHandler.handle(
				value
		);

	}

}
