package de.thm.arsnova.persistance;

import de.thm.arsnova.services.score.Score;
import de.thm.arsnova.entities.migration.v2.Session;

public interface SessionStatisticsRepository {
	Score getLearningProgress(Session session);
}
