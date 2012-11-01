/*
 * Copyright (C) 2012 THM webMedia
 *
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.thm.arsnova.annotation.Authenticated;
import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

@Service
public class SessionService implements ISessionService {

	private static final int DURATION_IN_MILLIS = 3 * 60 * 1000;
	
	@Autowired
	private IDatabaseDao databaseDao;

	@Autowired
	private IUserService userService;

	public void setDatabaseDao(final IDatabaseDao newDatabaseDao) {
		this.databaseDao = newDatabaseDao;
	}

	@Override
	@Authenticated
	public final Session joinSession(final String keyword) {
		userService.addCurrentUserToSessionMap(keyword);
		return databaseDao.getSession(keyword);
	}

	@Override
	public final List<Session> getMySessions(final String username) {
		return databaseDao.getMySessions(username);
	}

	@Override
	@Authenticated
	public final Session saveSession(final Session session) {
		return databaseDao.saveSession(session);
	}

	@Override
	public final boolean sessionKeyAvailable(final String keyword) {
		return databaseDao.sessionKeyAvailable(keyword);
	}

	@Override
	public final String generateKeyword() {
		final int low = 10000000;
		final int high = 100000000;
		String keyword = String
				.valueOf((int) (Math.random() * (high - low) + low));

		if (this.sessionKeyAvailable(keyword)) {
			return keyword;
		}
		return generateKeyword();
	}

	@Override
	@Authenticated
	public final LoggedIn registerAsOnlineUser(final User user, final String sessionkey) {
		Session session = this.joinSession(sessionkey);
		if (session == null) {
			return null;
		}
		if (session.getCreator().equals(user.getUsername())) {
			databaseDao.updateSessionOwnerActivity(session);
		}

		return databaseDao.registerAsOnlineUser(user, session);
	}

	@Override
	public int countActiveUsers(String sessionkey) {
		final long since = System.currentTimeMillis() - DURATION_IN_MILLIS;
		Session session = databaseDao.getSessionFromKeyword(sessionkey);
		return databaseDao.countActiveUsers(session, since);
	}
}
