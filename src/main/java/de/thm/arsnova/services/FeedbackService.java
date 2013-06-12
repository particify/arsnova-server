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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import de.thm.arsnova.FeedbackStorage;
import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Service
public class FeedbackService implements IFeedbackService {

	private static final int DEFAULT_SCHEDULER_DELAY = 5000;

	@Autowired
	private ARSnovaSocketIOServer server;

	/**
	 * minutes, after which the feedback is deleted
	 */
	@Value("${feedback.cleanup}")
	private int cleanupFeedbackDelay;

	@Autowired
	private IDatabaseDao databaseDao;

	@Autowired
	private IUserService userService;
	
	private FeedbackStorage feedbackStorage;

	public final void setDatabaseDao(final IDatabaseDao newDatabaseDao) {
		this.databaseDao = newDatabaseDao;
	}

	@PostConstruct
	public void init() {
		this.feedbackStorage = new FeedbackStorage(databaseDao);
	}
	
	@Override
	@Scheduled(fixedDelay = DEFAULT_SCHEDULER_DELAY)
	public final void cleanFeedbackVotes() {
		feedbackStorage.cleanFeedbackVotes(cleanupFeedbackDelay);
	}

	@Override
	public final Feedback getFeedback(final String keyword) {
		return feedbackStorage.getFeedback(keyword);
	}

	@Override
	public final int getFeedbackCount(final String keyword) {
		Feedback feedback = feedbackStorage.getFeedback(keyword);
		List<Integer> values = feedback.getValues();
		return values.get(Feedback.FEEDBACK_FASTER) + values.get(Feedback.FEEDBACK_OK)
				+ values.get(Feedback.FEEDBACK_SLOWER) + values.get(Feedback.FEEDBACK_AWAY);
	}

	@Override
	public final double getAverageFeedback(final String sessionkey) {
		Feedback feedback = feedbackStorage.getFeedback(sessionkey);
		List<Integer> values = feedback.getValues();
		double count = values.get(Feedback.FEEDBACK_FASTER) + values.get(Feedback.FEEDBACK_OK)
				+ values.get(Feedback.FEEDBACK_SLOWER) + values.get(Feedback.FEEDBACK_AWAY);
		double sum = values.get(Feedback.FEEDBACK_OK) + (values.get(Feedback.FEEDBACK_SLOWER) * 2)
				+ (values.get(Feedback.FEEDBACK_AWAY) * 3);

		if (count == 0) {
			throw new NoContentException();
		}
		return sum / count;
	}

	@Override
	public final long getAverageFeedbackRounded(final String sessionkey) {
		return Math.round(this.getAverageFeedback(sessionkey));
	}

	@Override
	public final boolean saveFeedback(final String keyword, final int value, final User user) {
		boolean result = feedbackStorage.saveFeedback(keyword, value, user);
		if (result) {
			System.out.println(new Date().toLocaleString());
			this.server.reportUpdatedFeedbackForSession(keyword);
		}
		return result;
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
	public final void broadcastFeedbackChanges(
			final Map<String, Set<String>> affectedUsers,
			final Set<String> allAffectedSessions
	) {
		for (Map.Entry<String, Set<String>> e : affectedUsers.entrySet()) {
			// Is this user registered with a socket connection?
			String connectedSocket = userService.getSessionForUser(e.getKey());
			if (connectedSocket != null) {
				this.server.reportDeletedFeedback(e.getKey(), e.getValue());
			}
		}
		for (String session : allAffectedSessions) {
			this.server.reportUpdatedFeedbackForSession(session);
		}
	}

	@Override
	public final Integer getMyFeedback(final String keyword, final User user) {
		return this.feedbackStorage.getMyFeedback(keyword, user);
	}
}
