package de.thm.arsnova.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.thm.arsnova.dao.IDatabaseDao;

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
	public final int countSessions() {
		return databaseDao.countSessions();
	}
}
