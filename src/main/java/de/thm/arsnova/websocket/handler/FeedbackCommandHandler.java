package de.thm.arsnova.websocket.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

	private static class UserFeedback {
		private String userId;
		private int value;

		UserFeedback(final String userId, final int value) {
			this.userId = userId;
			this.value = value;
		}

		public String getUserId() {
			return userId;
		}

		public void setUserId(final String userId) {
			this.userId = userId;
		}

		public int getValue() {
			return value;
		}

		public void setValue(final int value) {
			this.value = value;
		}
	}

	HashMap<String, List<UserFeedback>> roomValues = new HashMap<>();
	List<String> closedRooms = new ArrayList<>();

	private final SimpMessagingTemplate messagingTemplate;

	@Autowired
	public FeedbackCommandHandler(final SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	private synchronized void updateFeedbackForRoom(final String roomId, final UserFeedback userFeedback) {
		final List<UserFeedback> values = roomValues.getOrDefault(roomId, new ArrayList<>());
		values.removeIf(o -> (o.userId.equals(userFeedback.getUserId())));
		values.add(userFeedback);
		roomValues.put(roomId, values);
	}

	private synchronized void resetFeedbackForRoom(final String roomId) {
		roomValues.put(roomId, new ArrayList<>());
	}

	// This function is not threadsafe since others can update the feedback while this is computing it non-atomic.
	private int[] getFeedbackForRoom(final String roomId) {
		final List<UserFeedback> values = roomValues.getOrDefault(roomId, new ArrayList<>());
		final int[] retVal = new int[4];
		for (final UserFeedback f : values) {
			retVal[f.getValue()]++;
		}
		return retVal;
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
		if (!closedRooms.contains(command.getRoomId())) {
			final CreateFeedbackPayload p = command.getPayload().getPayload();
			final UserFeedback userFeedback = new UserFeedback(p.getUserId(), p.getValue());

			updateFeedbackForRoom(command.getRoomId(), userFeedback);
			final int[] newVals = getFeedbackForRoom(command.getRoomId());

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
		final int[] currentVals = getFeedbackForRoom(command.getRoomId());

		final FeedbackChanged feedbackChanged = new FeedbackChanged();
		final FeedbackChangedPayload feedbackChangedPayload = new FeedbackChangedPayload();
		feedbackChangedPayload.setValues(currentVals);
		feedbackChanged.setPayload(feedbackChangedPayload);

		messagingTemplate.convertAndSend(
				"/topic/" + command.getRoomId() + ".feedback.stream",
				feedbackChanged
		);
	}

	public void handle(final ResetFeedbackCommand command) {
		resetFeedbackForRoom(command.getRoomId());

		final FeedbackReset event = new FeedbackReset();

		messagingTemplate.convertAndSend(
				"/topic/" + command.getRoomId() + ".feedback.stream",
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
