package de.thm.arsnova.socket.message;

public class Feedback {

	private int value;
	private String sessionkey;

	public String getSessionkey() {
		return sessionkey;
	}

	public void setSessionkey(String keyword) {
		this.sessionkey = keyword;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "Feedback, sessionkey: " + sessionkey + ", value: " + value;
	}
}
