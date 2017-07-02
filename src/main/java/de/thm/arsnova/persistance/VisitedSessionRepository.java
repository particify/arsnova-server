package de.thm.arsnova.persistance;

public interface VisitedSessionRepository {
	int deleteInactiveGuestVisitedSessionLists(long lastActivityBefore);
}
