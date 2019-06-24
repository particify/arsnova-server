package de.thm.arsnova.websocket.handler;

import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import de.thm.arsnova.websocket.message.CreateFeedback;
import de.thm.arsnova.websocket.message.FeedbackChanged;
import de.thm.arsnova.websocket.message.FeedbackChangedPayload;
import de.thm.arsnova.websocket.message.GetFeedback;

@Component
public class FeedbackCommandHandler {

	HashMap<String, int[]> roomValues = new HashMap<>();

	private final SimpMessagingTemplate messagingTemplate;

	@Autowired
	public FeedbackCommandHandler(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	synchronized private int[] updateFeedbackForRoom(String roomId, int index) {
		int[] values = roomValues.getOrDefault(roomId, new int[4]);
		values[index]++;
		roomValues.put(roomId, values);
		return values;
	}

	public void handle(CreateFeedbackCommand command) {
		int updatedIndex = command.getPayload().getPayload().getValue();
		int[] newVals = updateFeedbackForRoom(command.getRoomId(), updatedIndex);

		FeedbackChanged feedbackChanged = new FeedbackChanged();
		FeedbackChangedPayload feedbackChangedPayload = new FeedbackChangedPayload();
		feedbackChangedPayload.setValues(newVals);
		feedbackChanged.setPayload(feedbackChangedPayload);

		messagingTemplate.convertAndSend(
				"/topic/" + command.getRoomId() + ".feedback.stream",
				feedbackChanged
		);
	}

	public void handle(GetFeedbackCommand command) {
		int[] currentVals = roomValues.getOrDefault(command.getRoomId(), new int[4]);

		FeedbackChanged feedbackChanged = new FeedbackChanged();
		FeedbackChangedPayload feedbackChangedPayload = new FeedbackChangedPayload();
		feedbackChangedPayload.setValues(currentVals);
		feedbackChanged.setPayload(feedbackChangedPayload);

		messagingTemplate.convertAndSend(
				"/topic/" + command.getRoomId() + ".feedback.stream",
				feedbackChanged
		);
	}



	public static class CreateFeedbackCommand {

		private String roomId;
		private CreateFeedback payload;

		public CreateFeedbackCommand(String roomId, CreateFeedback payload) {
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

		public GetFeedbackCommand(String roomId, GetFeedback payload) {
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

}
