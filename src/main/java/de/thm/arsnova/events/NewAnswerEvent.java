package de.thm.arsnova.events;

import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

public class NewAnswerEvent extends NovaEvent {

	private static final long serialVersionUID = 1L;

	private final Answer answer;

	private final User user;

	private final Question question;

	private final Session session;

	public NewAnswerEvent(Object source, Answer answer, User user, Question question, Session session) {
		super(source);
		this.answer = answer;
		this.user = user;
		this.question = question;
		this.session = session;
	}

	@Override
	public void accept(NovaEventVisitor visitor) {
		visitor.visit(this);
	}

	public Answer getAnswer() {
		return answer;
	}

	public User getUser() {
		return user;
	}

	public Question getQuestion() {
		return question;
	}

	public Session getSession() {
		return session;
	}

}
