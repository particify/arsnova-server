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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Service
public class FeedbackService implements IFeedbackService {

	@Autowired
	ARSnovaSocketIOServer server;

	/**
	 * minutes, after which the feedback is deleted
	 */
	@Value("${feedback.cleanup}")
	private int cleanupFeedbackDelay;

	@Autowired
	IDatabaseDao databaseDao;

	@Autowired
	IUserService userService;

	public void setDatabaseDao(IDatabaseDao databaseDao) {
		this.databaseDao = databaseDao;
	}

	@Override
	@Scheduled(fixedDelay = 5000)
	public void cleanFeedbackVotes() {
		databaseDao.cleanFeedbackVotes(cleanupFeedbackDelay);
	}

	@Override
	public Feedback getFeedback(String keyword) {
		return databaseDao.getFeedback(keyword);
	}

	@Override
	public int getFeedbackCount(String keyword) {
		Feedback feedback = databaseDao.getFeedback(keyword);
		List<Integer> values = feedback.getValues();
		return values.get(0) + values.get(1) + values.get(2) + values.get(3);
	}

	@Override
	public double getAverageFeedback(String sessionkey) {
		Feedback feedback = databaseDao.getFeedback(sessionkey);
		List<Integer> values = feedback.getValues();
		double count = values.get(0) + values.get(1) + values.get(2) + values.get(3);
		double sum = values.get(1) + (values.get(2) * 2) + (values.get(3) * 3);

		if (count == 0)
			throw new NoContentException();

		return sum / count;
	}

	@Override
	public long getAverageFeedbackRounded(String sessionkey) {
		return Math.round(this.getAverageFeedback(sessionkey));
	}

	@Override
	public boolean saveFeedback(String keyword, int value, User user) {
		return databaseDao.saveFeedback(keyword, value, user);
	}

	/**
	 * 
	 * @param affectedUsers
	 *            The user whose feedback got deleted along with all affected
	 *            session keywords
	 * @param allAffectedSessions
	 *            For convenience, this represents the union of all session
	 *            keywords mentioned above.
	 */
	@Override
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
	public Integer getMyFeedback(String keyword, User user) {
		return this.databaseDao.getMyFeedback(keyword, user);
	}
}
