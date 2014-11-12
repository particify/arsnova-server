package de.thm.arsnova.events;

import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.Session;

public class DeleteInterposedQuestionEvent extends NovaEvent {

	private static final long serialVersionUID = 1L;

	private final Session session;

	private final InterposedQuestion question;

	public DeleteInterposedQuestionEvent(Object source, Session session, InterposedQuestion question) {
		super(source);
		this.session = session;
		this.question = question;
	}

	@Override
	public void accept(NovaEventVisitor visitor) {
		// TODO Auto-generated method stub

	}

}
