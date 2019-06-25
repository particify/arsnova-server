package de.thm.arsnova.websocket.message;

import java.util.Arrays;

public class FeedbackChangedPayload implements WebSocketPayload {
	int[] values = new int[4];

	public int[] getValues() {
		return values;
	}

	public void setValues(int[] values) {
		this.values = values;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FeedbackChangedPayload that = (FeedbackChangedPayload) o;
		return Arrays.equals(values, that.values);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}
}
