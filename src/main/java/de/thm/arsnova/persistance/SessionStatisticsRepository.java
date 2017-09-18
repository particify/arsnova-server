package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.migration.v2.Room;
import de.thm.arsnova.services.score.Score;

public interface SessionStatisticsRepository {
	Score getLearningProgress(Room room);
}
