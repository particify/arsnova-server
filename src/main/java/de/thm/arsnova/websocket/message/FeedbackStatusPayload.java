package de.thm.arsnova.websocket.message;

import java.util.Objects;

public class FeedbackStatusPayload implements WebSocketPayload {
	boolean isClosed;

	public FeedbackStatusPayload() {
	}

	public FeedbackStatusPayload(final boolean isClosed) {
		this.isClosed = isClosed;
	}

	public boolean isClosed() {
		return isClosed;
	}

	public void setClosed(final boolean closed) {
		isClosed = closed;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final FeedbackStatusPayload that = (FeedbackStatusPayload) o;
		return isClosed == that.isClosed;
	}

	@Override
	public int hashCode() {
		return Objects.hash(isClosed);
	}
}
