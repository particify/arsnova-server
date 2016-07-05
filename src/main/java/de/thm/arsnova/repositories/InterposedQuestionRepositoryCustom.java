package de.thm.arsnova.repositories;

import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

/**
 * Custom interface to allow view-based repository methods with complex keys.
 */
public interface InterposedQuestionRepositoryCustom {

	public InterposedReadingCount countReadingBySessionAndCreator(Session session, User creator);

	public InterposedReadingCount countReadingBySession(Session session);

}