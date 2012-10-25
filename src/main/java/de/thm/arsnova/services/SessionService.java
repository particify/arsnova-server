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
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.thm.arsnova.annotation.Authenticated;
import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Service
public class SessionService implements ISessionService {

	@Autowired
	ARSnovaSocketIOServer server;
	
	@Autowired
	IDatabaseDao databaseDao;
	
	@Autowired
	IUserService userService;
	
	public void setDatabaseDao(IDatabaseDao databaseDao) {
		this.databaseDao = databaseDao;
	}
	
	@Override
	@Authenticated
	public Session joinSession(String keyword) {
		userService.addCurrentUserToSessionMap(keyword);
		return databaseDao.getSession(keyword);
	}

	@Override
	public List<Session> getMySessions(String username) {
		return databaseDao.getMySessions(username);
	}
	
	@Override
	@Authenticated
	public Session saveSession(Session session) {
		return databaseDao.saveSession(session);
	}

	@Override
	public boolean sessionKeyAvailable(String keyword) {
		return databaseDao.sessionKeyAvailable(keyword);
	}

	/**
	 * 
	 * @param affectedUsers The user whose feedback got deleted along with all affected session keywords
	 * @param allAffectedSessions For convenience, this represents the union of all session keywords mentioned above.
	 */
	public void broadcastFeedbackChanges(Map<String, Set<String>> affectedUsers, Set<String> allAffectedSessions) {
		for (Map.Entry<String, Set<String>> e : affectedUsers.entrySet()) {
			// Is this user registered with a socket connection?
			String connectedSocket = userService.getSessionForUser(e.getKey());
			if (connectedSocket != null) {
				this.server.reportDeletedFeedback(e.getKey(), e.getValue());
			}
		}
		this.server.reportUpdatedFeedbackForSessions(allAffectedSessions);
	}
	
	@Override
	public String generateKeyword() {
		final int low = 10000000;
		final int high = 100000000;
		String keyword = String.valueOf((int)(Math.random() * (high - low) + low));
		
		if (this.sessionKeyAvailable(keyword)) return keyword;
		return generateKeyword();
	}

	@Override
	@Authenticated
	public LoggedIn registerAsOnlineUser(User user, String sessionkey) {
		Session session = this.joinSession(sessionkey);
		if (session == null) return null;
		
		if (session.getCreator().equals(user.getUsername())) {
			databaseDao.updateSessionOwnerActivity(session);
		}
		
		return databaseDao.registerAsOnlineUser(user, session);
	}
}
