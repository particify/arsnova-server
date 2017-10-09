package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.Room;
import de.thm.arsnova.services.score.Score;

public interface SessionStatisticsRepository {
	Score getLearningProgress(Room room);
}
