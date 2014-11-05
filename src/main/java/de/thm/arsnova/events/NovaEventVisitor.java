package de.thm.arsnova.events;

public interface NovaEventVisitor {

	void visit(NewInterposedQuestionEvent newInterposedQuestionEvent);

	void visit(NewQuestionEvent newQuestionEvent);

}
