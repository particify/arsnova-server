package de.thm.arsnova.entities;


public class Answer {
	
	private String _id;
	private String _rev;
	private String type;
	private String sessionId;
	private String questionId;
	private String answerText;
	private String answerSubject;
	private String user;
	private long timestamp;
	private int answerCount;

	public Answer() {
		this.type = "skill_question_answer";
	}
	
	public final String get_id() {
		return _id;
	}

	public final void set_id(String _id) {
		this._id = _id;
	}

	public final String get_rev() {
		return _rev;
	}

	public final void set_rev(final String _rev) {
		this._rev = _rev;
	}

	public final String getType() {
		return type;
	}

	public final void setType(final String type) {
		this.type = type;
	}

	public final String getSessionId() {
		return sessionId;
	}

	public final void setSessionId(final String sessionId) {
		this.sessionId = sessionId;
	}

	public final String getQuestionId() {
		return questionId;
	}

	public final void setQuestionId(final String questionId) {
		this.questionId = questionId;
	}

	public final String getAnswerText() {
		return answerText;
	}

	public final void setAnswerText(final String answerText) {
		this.answerText = answerText;
	}

	public final String getText() {
		return answerText;
	}

	public final void setText(final String answerText) {
		this.answerText = answerText;
	}

	
	public final String getAnswerSubject() {
		return answerSubject;
	}

	public final void setAnswerSubject(final String answerSubject) {
		this.answerSubject = answerSubject;
	}
	
	public final String getSubject() {
		return answerSubject;
	}

	public final void setSubject(final String answerSubject) {
		this.answerSubject = answerSubject;
	}

	public final String getUser() {
		return user;
	}

	public final void setUser(final String user) {
		this.user = user;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public final int getAnswerCount() {
		return answerCount;
	}

	public final void setAnswerCount(final int answerCount) {
		this.answerCount = answerCount;
	}

	@Override
	public final String toString() {
		return "Answer type:'" + type + "'"
				+ ", session: " + sessionId 
				+ ", question: " + questionId
				+ ", subject: " + answerSubject
				+ ", answerCount: " + answerCount
				+ ", answer: " + answerText
				+ ", user: " + user;
	}

}
