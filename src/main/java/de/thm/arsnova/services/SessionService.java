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

import de.thm.arsnova.annotation.Authenticated;
import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.LoggedIn;
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
	IDatabaseDao databaseDao;
	
	public void setDatabaseDao(IDatabaseDao databaseDao) {
		this.databaseDao = databaseDao;
	}
	
	@Override
	@Scheduled(fixedDelay=5000)
	public void cleanFeedbackVotes() {
		databaseDao.cleanFeedbackVotes(cleanupFeedbackDelay);		
	}

	@Override
	@Authenticated
	public Session getSession(String keyword) {
		return databaseDao.getSession(keyword);
	}

	@Override
	public List<Session> getMySessions(String username) {
		return databaseDao.getMySessions(username);
	}

	@Override
	public List<Question> getSkillQuestions(String sessionkey, String sort) {
		return databaseDao.getSkillQuestions(sessionkey, sort);
	}
	
	@Override
	public int getSkillQuestionCount(String sessionkey) {
		return databaseDao.getSkillQuestionCount(sessionkey);
	}
	
	@Override
	@Authenticated
	public Session saveSession(Session session) {
		return databaseDao.saveSession(session);
	}

	@Override
	@Authenticated
	public Feedback getFeedback(String keyword) {
		return databaseDao.getFeedback(keyword);
	}

	@Override
	@Authenticated
	public boolean saveFeedback(String keyword, int value, User user) {
		return databaseDao.saveFeedback(keyword, value, user);
	}

	@Override
	public boolean sessionKeyAvailable(String keyword) {
		return databaseDao.sessionKeyAvailable(keyword);
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

	@Override
	@Authenticated
	public boolean saveQuestion(Question question) {
		Session session = this.databaseDao.getSessionFromKeyword(question.getSession());
		return this.databaseDao.saveQuestion(session, question);
	}
	
	@Override
	@Authenticated
	public Question getQuestion(String id) {
		return databaseDao.getQuestion(id);
	}

	@Override
	@Authenticated
	public LoggedIn registerAsOnlineUser(User user, String sessionkey) {
		Session session = this.getSession(sessionkey);
		if (session == null) return null;
		
		return databaseDao.registerAsOnlineUser(user, session);
	}
}
