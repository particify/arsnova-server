package de.thm.arsnova.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Statistics;

@Service
public class StatisticsService implements IStatisticsService {

	private static final int SINCEDURATION = 3 * 60 * 1000;

	@Autowired
	private IDatabaseDao databaseDao;

	@Override
	public final int countActiveUsers() {
		long since = System.currentTimeMillis() - SINCEDURATION;
		return databaseDao.countActiveUsers(since);
	}
	
	@Override
	public Statistics getStatistics() {
		Statistics statistics = new Statistics();
		statistics.setOpenSessions(databaseDao.countOpenSessions());
		statistics.setClosedSessions(databaseDao.countClosedSessions());
		statistics.setAnsers(databaseDao.countAnswers());
		statistics.setQuestions(databaseDao.countQuestions());
		return statistics;
	}
}
