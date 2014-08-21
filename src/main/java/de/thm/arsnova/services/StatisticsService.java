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
	private IUserService userService;

	@Autowired
	private SessionRegistry sessionRegistry;

	@Override
	public final Statistics getStatistics() {
		final long since = System.currentTimeMillis() - DURATION_IN_MILLIS;

		final Statistics statistics = new Statistics();
		statistics.setOpenSessions(databaseDao.countOpenSessions());
		statistics.setClosedSessions(databaseDao.countClosedSessions());
		statistics.setAnswers(databaseDao.countAnswers());
		statistics.setQuestions(databaseDao.countQuestions());
		/* TODO: Are both of the following do the same, now? If so, remove one of them. */
		statistics.setActiveUsers(userService.loggedInUsers());
		statistics.setLoggedinUsers(countLoggedInUsers());

		return statistics;
	}

	private int countLoggedInUsers() {
		if (sessionRegistry == null) {
			return 0;
		}
		return sessionRegistry.getAllPrincipals().size();
	}
}
