package de.thm.arsnova.websocket.message;

public class FeedbackChanged extends WebSocketMessage<FeedbackChangedPayload> {
	public FeedbackChanged() {
		super(FeedbackChanged.class.getSimpleName());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FeedbackChanged that = (FeedbackChanged) o;
		return this.getPayload().equals(that.getPayload());
	}
}
