package de.thm.arsnova.websocket.handler;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.thm.arsnova.config.RabbitConfig;
import de.thm.arsnova.config.properties.MessageBrokerProperties;
import de.thm.arsnova.event.AfterPatchEvent;
import de.thm.arsnova.model.Feedback;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.service.FeedbackStorageService;
import de.thm.arsnova.service.RoomService;
import de.thm.arsnova.websocket.message.CreateFeedback;
import de.thm.arsnova.websocket.message.CreateFeedbackPayload;
import de.thm.arsnova.websocket.message.FeedbackChanged;
import de.thm.arsnova.websocket.message.FeedbackChangedPayload;
import de.thm.arsnova.websocket.message.FeedbackReset;
import de.thm.arsnova.websocket.message.FeedbackStarted;
import de.thm.arsnova.websocket.message.FeedbackStopped;
import de.thm.arsnova.websocket.message.ResetFeedback;

@Component
@EnableConfigurationProperties(MessageBrokerProperties.class)
@ConditionalOnProperty(
		name = RabbitConfig.RabbitConfigProperties.RABBIT_ENABLED,
		prefix = MessageBrokerProperties.PREFIX,
		havingValue = "true")
public class FeedbackCommandHandler {
	private final RabbitTemplate messagingTemplate;
	private final FeedbackStorageService feedbackStorage;
	private final RoomService roomService;

	public FeedbackCommandHandler(
			final RabbitTemplate messagingTemplate,
			final FeedbackStorageService feedbackStorage,
			final RoomService roomService
	) {
		this.messagingTemplate = messagingTemplate;
		this.feedbackStorage = feedbackStorage;
		this.roomService = roomService;
	}

	/* ToDo: Listen to a more specific event */
	@EventListener
	public void handleLockFeedback(final AfterPatchEvent<Room> event) {
		if (event.getRequestedChanges().containsKey("settings")) {
			final String roomId = event.getEntity().getId();
			final Room.Settings settings = event.getEntity().getSettings();
			if (settings.isFeedbackLocked()) {
				final FeedbackStopped stompEvent = new FeedbackStopped();

				messagingTemplate.convertAndSend(
						"amq.topic",
						roomId + ".feedback.stream",
						stompEvent
				);

			} else {
				final FeedbackStarted stompEvent = new FeedbackStarted();

				messagingTemplate.convertAndSend(
						"amq.topic",
						roomId + ".feedback.stream",
						stompEvent
				);
			}
		}
	}

	public void handle(final CreateFeedback command) {
		final String roomId = command.getPayload().getRoomId();
		final Room loadedRoom = roomService.get(roomId, true);
		final Room room = new Room();
		room.setId(roomId);
		if (!loadedRoom.getSettings().isFeedbackLocked()) {
			final CreateFeedbackPayload p = command.getPayload();

			feedbackStorage.save(room, p.getValue(), p.getUserId());
			final Feedback feedback = feedbackStorage.getByRoom(room);
			final int[] newVals = feedback.getValues().stream().mapToInt(i -> i).toArray();

			final FeedbackChanged feedbackChanged = new FeedbackChanged();
			final FeedbackChangedPayload feedbackChangedPayload = new FeedbackChangedPayload();
			feedbackChangedPayload.setValues(newVals);
			feedbackChanged.setPayload(feedbackChangedPayload);

			messagingTemplate.convertAndSend(
					"amq.topic",
					roomId + ".feedback.stream",
					feedbackChanged
			);
		}
	}

	public void handle(final ResetFeedback command) {
		final String roomId = command.getPayload().getRoomId();
		final Room room = new Room();
		room.setId(roomId);
		feedbackStorage.cleanVotesByRoom(room, 0);

		final FeedbackReset event = new FeedbackReset();

		messagingTemplate.convertAndSend(
				"amq.topic",
				roomId + ".feedback.stream",
				event
		);
	}

}
