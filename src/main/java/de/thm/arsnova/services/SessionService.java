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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Service
public class SessionService implements ISessionService {

	@Autowired
	ARSnovaSocketIOServer server;
	
	/**
	 * minutes, after which the feedback is deleted
	 */
	@Value("${feedback.cleanup}")
	private int cleanupFeedbackDelay;
	
	private static final ConcurrentHashMap<String, String> user2session = new ConcurrentHashMap<String, String>();
	
	@Autowired
	IDatabaseDao sessionDao;
	
	@Override
	@Scheduled(fixedDelay=5000)
	public void cleanFeedbackVotes() {
		sessionDao.cleanFeedbackVotes(cleanupFeedbackDelay);		
	}

	@Override
	public Session getSession(String keyword) {
		return sessionDao.getSession(keyword);
	}

	@Override
	public Session saveSession(Session session) {
		return sessionDao.saveSession(session);
	}

	@Override
	public Feedback getFeedback(String keyword) {
		return sessionDao.getFeedback(keyword);
	}

	@Override
	public boolean saveFeedback(String keyword, int value, User user) {
		return sessionDao.saveFeedback(keyword, value, user);
	}

	@Override
	public boolean sessionKeyAvailable(String keyword) {
		return sessionDao.sessionKeyAvailable(keyword);
	}

	@Override
	public boolean isUserInSession(de.thm.arsnova.entities.User user, String keyword) {
		if (keyword == null) return false;
		String session = user2session.get(user.getUsername());
		if(session == null) return false;
		return keyword.equals(session);
	}
	
	@Override
	public List<String> getUsersInSession(String keyword) {
		List<String> result = new ArrayList<String>();
		for(Entry<String, String> e : user2session.entrySet()) {
			if(e.getValue().equals(keyword)) {
				result.add(e.getKey());
			}
		}
		return result;
	}	
	
	@Override
	@Transactional(isolation=Isolation.READ_COMMITTED)
	public void addUserToSessionMap(String username, String keyword) {
		user2session.put(username, keyword);	
	}

	/**
	 * 
	 * @param affectedUsers The user whose feedback got deleted along with all affected session keywords
	 * @param allAffectedSessions For convenience, this represents the union of all session keywords mentioned above.
	 */
	public void broadcastFeedbackChanges(Map<String, Set<String>> affectedUsers, Set<String> allAffectedSessions) {
		for (Map.Entry<String, Set<String>> e : affectedUsers.entrySet()) {
			// Is this user registered with a socket connection?
			String connectedSocket = user2session.get(e.getKey());
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
}
