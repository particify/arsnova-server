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
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
		private final ClientAuthentication user;

		public FeedbackStorageObject(final int initValue, final ClientAuthentication u) {
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
		public boolean fromUser(final ClientAuthentication u) {
			return user.equals(u);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(FeedbackStorageServiceImpl.class);

	private final Map<Room, Map<ClientAuthentication, FeedbackStorageObject>> data =
			new ConcurrentHashMap<>();

	@Override
	public Feedback getByRoom(final Room room) {
		int a = 0;
		int b = 0;
		int c = 0;
		int d = 0;

		if (data.get(room) == null) {
			return new Feedback(0, 0, 0, 0);
		}

		for (final FeedbackStorageObject fso : data.get(room).values()) {
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
	public Integer getByRoomAndUser(final Room room, final ClientAuthentication u) {
		if (data.get(room) == null) {
			return null;
		}

		for (final FeedbackStorageObject fso : data.get(room).values()) {
			if (fso.fromUser(u)) {
				return fso.getValue();
			}
		}

		return null;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void save(final Room room, final int value, final ClientAuthentication user) {
		logger.debug("Feedback data for {} Rooms is stored", data.size());
		logger.debug("Saving feedback: Room: {}, Value: {}, User: {}", room, value, user);
		Map<ClientAuthentication, FeedbackStorageObject> roomData = data.get(room);
		if (roomData == null) {
			logger.debug("Creating new feedback container for Room: {}", room);
			roomData = new ConcurrentHashMap<ClientAuthentication, FeedbackStorageObject>();
			data.put(room, roomData);
		}
		logger.debug("Feedback values for Room {}: {}", room.getId(), roomData.size());
		roomData.put(user, new FeedbackStorageObject(value, user));
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Map<Room, List<ClientAuthentication>> cleanVotes(final int cleanupFeedbackDelay) {
		final Map<Room, List<ClientAuthentication>> removedFeedbackOfUsersInSession = new HashMap<>();
		for (final Room room : data.keySet()) {
			if (!room.getSettings().isQuickSurveyEnabled()) {
				List<ClientAuthentication> affectedUsers = cleanVotesByRoom(room, cleanupFeedbackDelay);
				if (!affectedUsers.isEmpty()) {
					removedFeedbackOfUsersInSession.put(room, affectedUsers);
				}
			}
		}
		return removedFeedbackOfUsersInSession;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public List<ClientAuthentication> cleanVotesByRoom(final Room room, final int cleanupFeedbackDelayInMins) {
		final long timelimitInMillis = TimeUnit.MILLISECONDS.convert(cleanupFeedbackDelayInMins, TimeUnit.MINUTES);
		final Date maxAllowedTime = new Date(System.currentTimeMillis() - timelimitInMillis);
		final boolean forceClean = cleanupFeedbackDelayInMins == 0;

		final Map<ClientAuthentication, FeedbackStorageObject> roomFeedbacks = data.get(room);
		final List<ClientAuthentication> affectedUsers = new ArrayList<>();

		if (roomFeedbacks != null) {
			for (final Map.Entry<ClientAuthentication, FeedbackStorageObject> entry : roomFeedbacks.entrySet()) {
				final ClientAuthentication user = entry.getKey();
				final FeedbackStorageObject feedback = entry.getValue();
				final boolean timeIsUp = feedback.getTimestamp().before(maxAllowedTime);
				final boolean isAwayFeedback = getByRoomAndUser(room, user).equals(Feedback.FEEDBACK_AWAY);
				if (forceClean || timeIsUp && !isAwayFeedback) {
					roomFeedbacks.remove(user);
					affectedUsers.add(user);
				}
			}
		}
		return affectedUsers;
	}
}
