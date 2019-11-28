package de.thm.arsnova.websocket.message;

public class FeedbackStatus extends WebSocketMessage<FeedbackStatusPayload> {
	public FeedbackStatus() {
		super(FeedbackStatus.class.getSimpleName());
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final FeedbackChanged that = (FeedbackChanged) o;
		return this.getPayload().equals(that.getPayload());
	}
}
