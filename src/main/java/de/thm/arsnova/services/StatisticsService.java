package de.thm.arsnova.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Statistics;

@Service
public class StatisticsService implements IStatisticsService {

	@Autowired
	private IDatabaseDao databaseDao;

	@Autowired
	private IUserService userService;

	@Autowired
	private SessionRegistry sessionRegistry;

	@Override
	public final int countActiveUsers() {
		return userService.loggedInUsers();
	}

	@Override
	public int countLoggedInUsers() {
		return sessionRegistry.getAllPrincipals().size();
	}

	@Override
	public final Statistics getStatistics() {
		Statistics statistics = new Statistics();
		statistics.setOpenSessions(databaseDao.countOpenSessions());
		statistics.setClosedSessions(databaseDao.countClosedSessions());
		statistics.setAnswers(databaseDao.countAnswers());
		statistics.setQuestions(databaseDao.countQuestions());
		statistics.setActiveUsers(userService.loggedInUsers());

		return statistics;
	}
}
