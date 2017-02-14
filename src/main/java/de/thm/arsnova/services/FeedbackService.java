/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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

import de.thm.arsnova.FeedbackStorage;
import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.events.DeleteFeedbackForSessionsEvent;
import de.thm.arsnova.events.NewFeedbackEvent;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Performs all feedback related operations.
 */
@Service
public class FeedbackService implements IFeedbackService, ApplicationEventPublisherAware {

	private static final int DEFAULT_SCHEDULER_DELAY = 5000;
	private static final double Z_THRESHOLD = 0.1;

	/**
	 * minutes, after which the feedback is deleted
	 */
	@Value("${feedback.cleanup}")
	private int cleanupFeedbackDelay;

	@Autowired
	private IDatabaseDao databaseDao;

	private FeedbackStorage feedbackStorage;

	private ApplicationEventPublisher publisher;

	public void setDatabaseDao(final IDatabaseDao newDatabaseDao) {
		databaseDao = newDatabaseDao;
	}

	@PostConstruct
	public void init() {
		feedbackStorage = new FeedbackStorage();
	}

	@Override
	@Scheduled(fixedDelay = DEFAULT_SCHEDULER_DELAY)
	public void cleanFeedbackVotes() {
		Map<Session, List<User>> deletedFeedbackOfUsersInSession = feedbackStorage.cleanFeedbackVotes(cleanupFeedbackDelay);
		/*
		 * mapping (Session -> Users) is not suitable for web sockets, because we want to sent all affected
		 * sessions to a single user in one go instead of sending multiple messages for each session. Hence,
		 * we need the mapping (User -> Sessions)
		 */
		final Map<User, Set<Session>> affectedSessionsOfUsers = new HashMap<>();

		for (Map.Entry<Session, List<User>> entry : deletedFeedbackOfUsersInSession.entrySet()) {
			final Session session = entry.getKey();
			final List<User> users = entry.getValue();
			for (User user : users) {
				Set<Session> affectedSessions;
				if (affectedSessionsOfUsers.containsKey(user)) {
					affectedSessions = affectedSessionsOfUsers.get(user);
				} else {
					affectedSessions = new HashSet<>();
				}
				affectedSessions.add(session);
				affectedSessionsOfUsers.put(user, affectedSessions);
			}
		}
		// Send feedback reset event to all affected users
		for (Map.Entry<User, Set<Session>> entry : affectedSessionsOfUsers.entrySet()) {
			final User user = entry.getKey();
			final Set<Session> arsSessions = entry.getValue();
			this.publisher.publishEvent(new DeleteFeedbackForSessionsEvent(this, arsSessions, user));
		}
		// For each session that has deleted feedback, send the new feedback to all clients
		for (Session session : deletedFeedbackOfUsersInSession.keySet()) {
			this.publisher.publishEvent(new NewFeedbackEvent(this, session));
		}
	}

	@Override
	public void cleanFeedbackVotesInSession(final String keyword, final int cleanupFeedbackDelayInMins) {
		final Session session = databaseDao.getSessionFromKeyword(keyword);
		List<User> affectedUsers = feedbackStorage.cleanFeedbackVotesInSession(session, cleanupFeedbackDelayInMins);
		Set<Session> sessionSet = new HashSet<>();
		sessionSet.add(session);

		// Send feedback reset event to all affected users
		for (User user : affectedUsers) {
			this.publisher.publishEvent(new DeleteFeedbackForSessionsEvent(this, sessionSet, user));
		}
		// send the new feedback to all clients in affected session
		this.publisher.publishEvent(new NewFeedbackEvent(this, session));
	}

	@Override
	public Feedback getFeedback(final String keyword) {
		final Session session = databaseDao.getSessionFromKeyword(keyword);
		if (session == null) {
			throw new NotFoundException();
		}
		return feedbackStorage.getFeedback(session);
	}

	@Override
	public int getFeedbackCount(final String keyword) {
		final Feedback feedback = this.getFeedback(keyword);
		final List<Integer> values = feedback.getValues();
		return values.get(Feedback.FEEDBACK_FASTER) + values.get(Feedback.FEEDBACK_OK)
				+ values.get(Feedback.FEEDBACK_SLOWER) + values.get(Feedback.FEEDBACK_AWAY);
	}

	@Override
	public double getAverageFeedback(final String sessionkey) {
		final Session session = databaseDao.getSessionFromKeyword(sessionkey);
		if (session == null) {
			throw new NotFoundException();
		}
		final Feedback feedback = feedbackStorage.getFeedback(session);
		final List<Integer> values = feedback.getValues();
		final double count = values.get(Feedback.FEEDBACK_FASTER) + values.get(Feedback.FEEDBACK_OK)
				+ values.get(Feedback.FEEDBACK_SLOWER) + values.get(Feedback.FEEDBACK_AWAY);
		final double sum = values.get(Feedback.FEEDBACK_OK) + values.get(Feedback.FEEDBACK_SLOWER) * 2
				+ values.get(Feedback.FEEDBACK_AWAY) * 3;

		if (Math.abs(count) < Z_THRESHOLD) {
			throw new NoContentException();
		}
		return sum / count;
	}

	@Override
	public long getAverageFeedbackRounded(final String sessionkey) {
		return Math.round(getAverageFeedback(sessionkey));
	}

	@Override
	public boolean saveFeedback(final String keyword, final int value, final User user) {
		final Session session = databaseDao.getSessionFromKeyword(keyword);
		if (session == null) {
			throw new NotFoundException();
		}
		feedbackStorage.saveFeedback(session, value, user);

		this.publisher.publishEvent(new NewFeedbackEvent(this, session));
		return true;
	}

	@Override
	public Integer getMyFeedback(final String keyword, final User user) {
		final Session session = databaseDao.getSessionFromKeyword(keyword);
		if (session == null) {
			throw new NotFoundException();
		}
		return feedbackStorage.getMyFeedback(session, user);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}
}
