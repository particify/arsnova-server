/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.migration.v2.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.events.DeleteFeedbackForSessionsEvent;
import de.thm.arsnova.events.NewFeedbackEvent;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.persistance.SessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Performs all feedback related operations.
 */
@Service
public class FeedbackServiceImpl implements FeedbackService, ApplicationEventPublisherAware {

	private static final int DEFAULT_SCHEDULER_DELAY = 5000;
	private static final double Z_THRESHOLD = 0.1;

	/**
	 * minutes, after which the feedback is deleted
	 */
	@Value("${feedback.cleanup}")
	private int cleanupFeedbackDelay;

	private SessionRepository sessionRepository;

	private FeedbackStorageService feedbackStorage;

	private ApplicationEventPublisher publisher;

	public FeedbackServiceImpl(FeedbackStorageService feedbackStorage, SessionRepository sessionRepository) {
		this.feedbackStorage = feedbackStorage;
		this.sessionRepository = sessionRepository;
	}

	@Override
	@Scheduled(fixedDelay = DEFAULT_SCHEDULER_DELAY)
	public void cleanFeedbackVotes() {
		Map<Session, List<User>> deletedFeedbackOfUsersInSession = feedbackStorage.cleanVotes(cleanupFeedbackDelay);
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
	public void cleanFeedbackVotesBySessionKey(final String keyword, final int cleanupFeedbackDelayInMins) {
		final Session session = sessionRepository.findByKeyword(keyword);
		List<User> affectedUsers = feedbackStorage.cleanVotesBySession(session, cleanupFeedbackDelayInMins);
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
	public Feedback getBySessionKey(final String keyword) {
		final Session session = sessionRepository.findByKeyword(keyword);
		if (session == null) {
			throw new NotFoundException();
		}
		return feedbackStorage.getBySession(session);
	}

	@Override
	public int countFeedbackBySessionKey(final String keyword) {
		final Feedback feedback = this.getBySessionKey(keyword);
		final List<Integer> values = feedback.getValues();
		return values.get(Feedback.FEEDBACK_FASTER) + values.get(Feedback.FEEDBACK_OK)
				+ values.get(Feedback.FEEDBACK_SLOWER) + values.get(Feedback.FEEDBACK_AWAY);
	}

	@Override
	public double calculateAverageFeedback(final String sessionkey) {
		final Session session = sessionRepository.findByKeyword(sessionkey);
		if (session == null) {
			throw new NotFoundException();
		}
		final Feedback feedback = feedbackStorage.getBySession(session);
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
	public long calculateRoundedAverageFeedback(final String sessionkey) {
		return Math.round(calculateAverageFeedback(sessionkey));
	}

	@Override
	public boolean save(final String keyword, final int value, final User user) {
		final Session session = sessionRepository.findByKeyword(keyword);
		if (session == null) {
			throw new NotFoundException();
		}
		feedbackStorage.save(session, value, user);

		this.publisher.publishEvent(new NewFeedbackEvent(this, session));
		return true;
	}

	@Override
	public Integer getBySessionKeyAndUser(final String keyword, final User user) {
		final Session session = sessionRepository.findByKeyword(keyword);
		if (session == null) {
			throw new NotFoundException();
		}
		return feedbackStorage.getBySessionAndUser(session, user);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}
}
