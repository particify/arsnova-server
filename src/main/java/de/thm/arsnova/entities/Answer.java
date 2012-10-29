package de.thm.arsnova.entities;

import java.util.ArrayList;
import java.util.List;

/*
"type":"skill_question_answer",
"sessionId":"61d33ea2ec73acefbba898c3510325c9",
"questionId":"61d33ea2ec73acefbba898c351040280",
"answerText":"2500 $",
"user":"jhtr80"
}}

*/
public class Answer {
	
	private String _id;
	private String _rev;
	private String type;
	private String sessionId;
	private String questionId;
	private String answerText;
	private String user;

	public Answer() {
		this.type = "skill_question_answer";
	}
	
	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String get_rev() {
		return _rev;
	}

	public void set_rev(String _rev) {
		this._rev = _rev;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getQuestionId() {
		return questionId;
	}

	public void setQuestionId(String questionId) {
		this.questionId = questionId;
	}

	public String getAnswerText() {
		return answerText;
	}

	public void setAnswerText(String answerText) {
		this.answerText = answerText;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	@Override
	public String toString() {
		return "Answer type:'" + type + "'" +
				", session: " + sessionId + 
				", question: " + questionId +
				", answer: " + answerText + 
				", user: " + user;
	}

}
