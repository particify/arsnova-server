package de.thm.arsnova.persistance;

import de.thm.arsnova.services.score.Score;
import de.thm.arsnova.entities.Session;

public interface SessionStatisticsRepository {
	Score getLearningProgress(Session session);
}
