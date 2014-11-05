package de.thm.arsnova.events;

import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;

public class DeleteAnswerEvent extends NovaEvent {

	private static final long serialVersionUID = 1L;

	private final Question question;

	private final Session session;

	public DeleteAnswerEvent(Object source, Question question, Session session) {
		super(source);
		this.question = question;
		this.session = session;
	}

	@Override
	public void accept(NovaEventVisitor visitor) {
		visitor.visit(this);
	}

	public Question getQuestion() {
		return question;
	}

	public Session getSession() {
		return session;
	}

}
