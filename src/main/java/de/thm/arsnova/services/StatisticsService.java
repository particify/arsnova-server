package de.thm.arsnova.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Statistics;

@Service
public class StatisticsService implements IStatisticsService {

	private static final int DURATION_IN_MILLIS = 3 * 60 * 1000;

	@Autowired
	private IDatabaseDao databaseDao;

	@Autowired
	private SessionRegistry sessionRegistry;

	@Override
	public final int countActiveUsers() {
		final long since = System.currentTimeMillis() - DURATION_IN_MILLIS;
		return databaseDao.countActiveUsers(since);
	}

	@Override
	public int countLoggedInUsers() {
		if (sessionRegistry == null) {
			return 0;
		}
		return sessionRegistry.getAllPrincipals().size();
	}

	@Override
	public final Statistics getStatistics() {
		final long since = System.currentTimeMillis() - DURATION_IN_MILLIS;

		final Statistics statistics = new Statistics();
		statistics.setOpenSessions(databaseDao.countOpenSessions());
		statistics.setClosedSessions(databaseDao.countClosedSessions());
		statistics.setAnswers(databaseDao.countAnswers());
		statistics.setQuestions(databaseDao.countQuestions());
		statistics.setActiveUsers(databaseDao.countActiveUsers(since));
		statistics.setLoggedinUsers(countLoggedInUsers());
		return statistics;
	}
}
