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

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * In-memory storage of feedback data.
 */
@Service
public class FeedbackStorageServiceImpl implements FeedbackStorageService {
	private static class FeedbackStorageObject {
		private final int value;
		private final Date timestamp;
		private final User user;

		public FeedbackStorageObject(final int initValue, final User u) {
			value = initValue;
			timestamp = new Date();
			user = u;
		}

		public int getValue() {
			return value;
		}
		public Date getTimestamp() {
			return timestamp;
		}
		public boolean fromUser(final User u) {
			return user.equals(u);
		}
	}

	private final Map<Session, Map<User, FeedbackStorageObject>> data =
			new ConcurrentHashMap<>();

	@Override
	public Feedback getBySession(final Session session) {
		int a = 0;
		int b = 0;
		int c = 0;
		int d = 0;

		if (data.get(session) == null) {
			return new Feedback(0, 0, 0, 0);
		}

		for (final FeedbackStorageObject fso : data.get(session).values()) {
			switch (fso.getValue()) {
			case Feedback.FEEDBACK_FASTER:
				a++;
				break;
			case Feedback.FEEDBACK_OK:
				b++;
				break;
			case Feedback.FEEDBACK_SLOWER:
				c++;
				break;
			case Feedback.FEEDBACK_AWAY:
				d++;
				break;
			default:
				break;
			}
		}
		return new Feedback(a, b, c, d);
	}

	@Override
	public Integer getBySessionAndUser(final Session session, final User u) {
		if (data.get(session) == null) {
			return null;
		}

		for (final FeedbackStorageObject fso : data.get(session).values()) {
			if (fso.fromUser(u)) {
				return fso.getValue();
			}
		}

		return null;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void save(final Session session, final int value, final User user) {
		if (data.get(session) == null) {
			data.put(session, new ConcurrentHashMap<User, FeedbackStorageObject>());
		}

		data.get(session).put(user, new FeedbackStorageObject(value, user));
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Map<Session, List<User>> cleanVotes(final int cleanupFeedbackDelay) {
		final Map<Session, List<User>> removedFeedbackOfUsersInSession = new HashMap<>();
		for (final Session session : data.keySet()) {
			if (!session.getFeatures().isLiveClicker()) {
				List<User> affectedUsers = cleanVotesBySession(session, cleanupFeedbackDelay);
				if (!affectedUsers.isEmpty()) {
					removedFeedbackOfUsersInSession.put(session, affectedUsers);
				}
			}
		}
		return removedFeedbackOfUsersInSession;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public List<User> cleanVotesBySession(final Session session, final int cleanupFeedbackDelayInMins) {
		final long timelimitInMillis = TimeUnit.MILLISECONDS.convert(cleanupFeedbackDelayInMins, TimeUnit.MINUTES);
		final Date maxAllowedTime = new Date(System.currentTimeMillis() - timelimitInMillis);
		final boolean forceClean = cleanupFeedbackDelayInMins == 0;

		final Map<User, FeedbackStorageObject> sessionFeedbacks = data.get(session);
		final List<User> affectedUsers = new ArrayList<>();

		if (sessionFeedbacks != null) {
			for (final Map.Entry<User, FeedbackStorageObject> entry : sessionFeedbacks.entrySet()) {
				final User user = entry.getKey();
				final FeedbackStorageObject feedback = entry.getValue();
				final boolean timeIsUp = feedback.getTimestamp().before(maxAllowedTime);
				final boolean isAwayFeedback = getBySessionAndUser(session, user).equals(Feedback.FEEDBACK_AWAY);
				if (forceClean || timeIsUp && !isAwayFeedback) {
					sessionFeedbacks.remove(user);
					affectedUsers.add(user);
				}
			}
		}
		return affectedUsers;
	}
}
