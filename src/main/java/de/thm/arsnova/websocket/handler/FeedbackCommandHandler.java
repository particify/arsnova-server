package de.thm.arsnova.websocket.handler;

import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

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
import de.thm.arsnova.websocket.message.GetFeedback;
import de.thm.arsnova.websocket.message.ResetFeedback;

@Component
public class FeedbackCommandHandler {
	private static final Logger logger = LoggerFactory.getLogger(FeedbackCommandHandler.class);

	private final RabbitTemplate messagingTemplate;
	private final FeedbackStorageService feedbackStorage;
	private final RoomService roomService;

	@Autowired
	public FeedbackCommandHandler(
			final RabbitTemplate messagingTemplate,
			final FeedbackStorageService feedbackStorage,
			final RoomService roomService
	) {
		this.messagingTemplate = messagingTemplate;
		this.feedbackStorage = feedbackStorage;
		this.roomService = roomService;
	}

	@EventListener
	public void handleLockFeedback(final AfterPatchEvent<Room> event) {
		if (event.getChanges().containsKey("settings")) {
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

	/*
	ToDo: Listen to a more specific event
	If feedback is getting locked for a room via HTTP PATCH, the specific event is currently not fired

	@EventListener(condition = "#event.stateName == 'settings'")
	public void handleLockFeedback(final StateChangeEvent<Room, Room.Settings> event) {
		final String roomId = event.getEntity().getId();
		if (event.getEntity().getSettings().isFeedbackLocked()) {
			final FeedbackStopped stompEvent = new FeedbackStopped();

			messagingTemplate.convertAndSend(
					"/topic/" + roomId + ".feedback.stream",
					stompEvent
			);
		} else {
			final FeedbackStarted stompEvent = new FeedbackStarted();

			messagingTemplate.convertAndSend(
					"/topic/" + roomId + ".feedback.stream",
					stompEvent
			);
		}
	}*/

	public void handle(final CreateFeedback command) {
		final String roomId = command.getPayload().getRoomId();
		final Room room = roomService.get(roomId, true);
		if (!room.getSettings().isFeedbackLocked()) {
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

	public void handle(final GetFeedback command) {
		final String roomId = command.getPayload().getRoomId();
		final Room room = feedbackStorage.findByRoomId(roomId);

		if (room != null) {

			final Feedback feedback = feedbackStorage.getByRoom(room);
			final int[] currentVals = feedback.getValues().stream().mapToInt(i -> i).toArray();

			final FeedbackChanged feedbackChanged = new FeedbackChanged();
			final FeedbackChangedPayload feedbackChangedPayload = new FeedbackChangedPayload();
			feedbackChangedPayload.setValues(currentVals);
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
		final Room room = roomService.get(roomId, true);
		feedbackStorage.cleanVotesByRoom(room, 0);

		final FeedbackReset event = new FeedbackReset();

		messagingTemplate.convertAndSend(
				roomId + ".feedback.stream",
				event
		);
	}

}
