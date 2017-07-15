package de.thm.arsnova.persistance;

import de.thm.arsnova.domain.CourseScore;
import de.thm.arsnova.entities.Session;

public interface SessionStatisticsRepository {
	CourseScore getLearningProgress(Session session);
}
