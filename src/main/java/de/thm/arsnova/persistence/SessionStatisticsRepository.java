package de.thm.arsnova.persistence;

import de.thm.arsnova.model.Room;
import de.thm.arsnova.service.score.Score;

public interface SessionStatisticsRepository {
	Score getLearningProgress(Room room);
}
