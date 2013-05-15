package de.thm.arsnova.services;

import de.thm.arsnova.entities.Statistics;

public interface IStatisticsService {

	int countActiveUsers();

	int countLoggedInUsers();

	Statistics getStatistics();
}
