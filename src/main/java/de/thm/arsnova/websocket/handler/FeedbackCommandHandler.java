package de.thm.arsnova.websocket.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.thm.arsnova.model.Feedback;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.service.FeedbackStorageService;
import de.thm.arsnova.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import de.thm.arsnova.websocket.message.CreateFeedback;
import de.thm.arsnova.websocket.message.CreateFeedbackPayload;
import de.thm.arsnova.websocket.message.FeedbackChanged;
import de.thm.arsnova.websocket.message.FeedbackChangedPayload;
import de.thm.arsnova.websocket.message.FeedbackReset;
import de.thm.arsnova.websocket.message.FeedbackStarted;
import de.thm.arsnova.websocket.message.FeedbackStatus;
import de.thm.arsnova.websocket.message.FeedbackStatusPayload;
import de.thm.arsnova.websocket.message.FeedbackStopped;
import de.thm.arsnova.websocket.message.GetFeedback;
import de.thm.arsnova.websocket.message.GetFeedbackStatus;
import de.thm.arsnova.websocket.message.ResetFeedback;
import de.thm.arsnova.websocket.message.StartFeedback;
import de.thm.arsnova.websocket.message.StopFeedback;

@Component
public class FeedbackCommandHandler {
	List<String> closedRooms = new ArrayList<>();

	private final SimpMessagingTemplate messagingTemplate;
	private final FeedbackStorageService feedbackStorage;
	private final RoomService roomService;

	@Autowired
	public FeedbackCommandHandler(
		final SimpMessagingTemplate messagingTemplate,
		final FeedbackStorageService feedbackStorage,
		final RoomService roomService
	) {
		this.messagingTemplate = messagingTemplate;
		this.feedbackStorage = feedbackStorage;
		this.roomService = roomService;
	}

	public void handle(final GetFeedbackStatusCommand command) {
		final FeedbackStatus event = new FeedbackStatus();
		final FeedbackStatusPayload payload = new FeedbackStatusPayload();
		payload.setClosed(closedRooms.contains(command.getRoomId()));
		event.setPayload(payload);

		messagingTemplate.convertAndSend(
				"/topic/" + command.getRoomId() + ".feedback.stream",
				event
		);
	}

	public void handle(final StartFeedbackCommand command) {
		closedRooms.remove(command.getRoomId());

		final FeedbackStarted event = new FeedbackStarted();

		messagingTemplate.convertAndSend(
				"/topic/" + command.getRoomId() + ".feedback.stream",
				event
		);
	}

	public void handle(final StopFeedbackCommand command) {
		closedRooms.add(command.getRoomId());

		final FeedbackStopped event = new FeedbackStopped();

		messagingTemplate.convertAndSend(
				"/topic/" + command.getRoomId() + ".feedback.stream",
				event
		);
	}

	public void handle(final CreateFeedbackCommand command) {
		final String roomId = command.getRoomId();
		if (!closedRooms.contains(roomId)) {
			final CreateFeedbackPayload p = command.getPayload().getPayload();
			final Room room = roomService.get(roomId, true);

			feedbackStorage.save(room, p.getValue(), p.getUserId());
			Feedback feedback = feedbackStorage.getByRoom(room);
			final int[] newVals = feedback.getValues().stream().mapToInt(i->i).toArray();

			final FeedbackChanged feedbackChanged = new FeedbackChanged();
			final FeedbackChangedPayload feedbackChangedPayload = new FeedbackChangedPayload();
			feedbackChangedPayload.setValues(newVals);
			feedbackChanged.setPayload(feedbackChangedPayload);

			messagingTemplate.convertAndSend(
					"/topic/" + command.getRoomId() + ".feedback.stream",
					feedbackChanged
			);
		}
	}

	public void handle(final GetFeedbackCommand command) {
		final String roomId = command.getRoomId();
		final Room room = roomService.get(roomId, true);
		Feedback feedback = feedbackStorage.getByRoom(room);
		final int[] currentVals = feedback.getValues().stream().mapToInt(i->i).toArray();

		final FeedbackChanged feedbackChanged = new FeedbackChanged();
		final FeedbackChangedPayload feedbackChangedPayload = new FeedbackChangedPayload();
		feedbackChangedPayload.setValues(currentVals);
		feedbackChanged.setPayload(feedbackChangedPayload);

		messagingTemplate.convertAndSend(
				"/topic/" + roomId + ".feedback.stream",
				feedbackChanged
		);
	}

	public void handle(final ResetFeedbackCommand command) {
		final String roomId = command.getRoomId();
		final Room room = roomService.get(roomId, true);
		feedbackStorage.cleanVotesByRoom(room, 0);

		final FeedbackReset event = new FeedbackReset();

		messagingTemplate.convertAndSend(
				"/topic/" + roomId + ".feedback.stream",
				event
		);
	}


	public static class GetFeedbackStatusCommand {

		private String roomId;
		private GetFeedbackStatus payload;

		public GetFeedbackStatusCommand(final String roomId, final GetFeedbackStatus payload) {
			this.roomId = roomId;
			this.payload = payload;
		}

		public GetFeedbackStatus getPayload() {
			return payload;
		}

		public String getRoomId() {
			return roomId;
		}
	}

	public static class StartFeedbackCommand {

		private String roomId;
		private StartFeedback payload;

		public StartFeedbackCommand(final String roomId, final StartFeedback payload) {
			this.roomId = roomId;
			this.payload = payload;
		}

		public StartFeedback getPayload() {
			return payload;
		}

		public String getRoomId() {
			return roomId;
		}
	}

	public static class StopFeedbackCommand {

		private String roomId;
		private StopFeedback payload;

		public StopFeedbackCommand(final String roomId, final StopFeedback payload) {
			this.roomId = roomId;
			this.payload = payload;
		}

		public StopFeedback getPayload() {
			return payload;
		}

		public String getRoomId() {
			return roomId;
		}
	}

	public static class CreateFeedbackCommand {

		private String roomId;
		private CreateFeedback payload;

		public CreateFeedbackCommand(final String roomId, final CreateFeedback payload) {
			this.roomId = roomId;
			this.payload = payload;
		}

		public CreateFeedback getPayload() {
			return payload;
		}

		public String getRoomId() {
			return roomId;
		}
	}

	public static class GetFeedbackCommand {

		private String roomId;
		private GetFeedback payload;

		public GetFeedbackCommand(final String roomId, final GetFeedback payload) {
			this.roomId = roomId;
			this.payload = payload;
		}

		public GetFeedback getPayload() {
			return payload;
		}

		public String getRoomId() {
			return roomId;
		}
	}

	public static class ResetFeedbackCommand {

		private String roomId;
		private ResetFeedback payload;

		public ResetFeedbackCommand(final String roomId, final ResetFeedback payload) {
			this.roomId = roomId;
			this.payload = payload;
		}

		public ResetFeedback getPayload() {
			return payload;
		}

		public String getRoomId() {
			return roomId;
		}
	}

}
