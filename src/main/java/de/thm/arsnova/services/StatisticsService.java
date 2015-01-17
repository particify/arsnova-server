/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
	public final Statistics getStatistics() {
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
