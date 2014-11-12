package de.thm.arsnova.entities.transport;

import java.util.ArrayList;
import java.util.List;

public class InterposedQuestion {

	private String id;
	private String subject;
	private String text;
	private long timestamp;
	private boolean read;

	public static List<InterposedQuestion> fromList(List<de.thm.arsnova.entities.InterposedQuestion> questions) {
		ArrayList<InterposedQuestion> interposedQuestions = new ArrayList<InterposedQuestion>();
		for (de.thm.arsnova.entities.InterposedQuestion question : questions) {
			interposedQuestions.add(new InterposedQuestion(question));
		}
		return interposedQuestions;
	}

	public InterposedQuestion(de.thm.arsnova.entities.InterposedQuestion question) {
		this.id = question.get_id();
		this.subject = question.getSubject();
		this.text = question.getText();
		this.timestamp = question.getTimestamp();
		this.read = question.isRead();
	}

	public InterposedQuestion() {}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}
}
