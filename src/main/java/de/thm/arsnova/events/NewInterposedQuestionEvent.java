package de.thm.arsnova.events;

import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.Session;

public class NewInterposedQuestionEvent extends NovaEvent {

	private static final long serialVersionUID = 1L;

	private final Session session;
	private final InterposedQuestion question;

	public NewInterposedQuestionEvent(Object source, InterposedQuestion question, Session session) {
		super(source);
		this.question = question;
		this.session = session;
	}

	public Session getSession() {
		return session;
	}

	public InterposedQuestion getQuestion() {
		return question;
	}

	@Override
	public void accept(NovaEventVisitor visitor) {
		visitor.visit(this);
	}

}
