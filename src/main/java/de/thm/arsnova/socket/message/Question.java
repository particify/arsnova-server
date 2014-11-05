package de.thm.arsnova.socket.message;

public class Question {

	private final String _id;
	private final String variant;

	public Question(de.thm.arsnova.entities.Question question) {
		this._id = question.get_id();
		this.variant = question.getQuestionVariant();
	}

	public String get_id() {
		return _id;
	}

	public String getVariant() {
		return variant;
	}
}
