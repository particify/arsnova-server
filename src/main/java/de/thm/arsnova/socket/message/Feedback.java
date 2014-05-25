package de.thm.arsnova.socket.message;

public class Feedback {

	private int value;

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "Feedback, value: " + value;
	}
}
