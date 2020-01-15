package de.thm.arsnova.domain;

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.LearningProgressValues;

/**
 * Null Object Pattern for unknown learning progress calculation
 */
public class NullLearningProgress implements LearningProgress {
	@Override
	public LearningProgressValues getCourseProgress(Session session) {
		return new LearningProgressValues();
	}

	@Override
	public LearningProgressValues getMyProgress(Session session, User user) {
		return new LearningProgressValues();
	}
}
