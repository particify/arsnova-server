/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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

package de.thm.arsnova.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.thm.arsnova.model.Feedback;
import de.thm.arsnova.model.Room;

/**
 * In-memory storage of feedback data.
 */
@Service
public class FeedbackStorageServiceImpl implements FeedbackStorageService {
	private static class FeedbackStorageObject {
		private final int value;
		private final Date timestamp;
		private final String userId;

		public FeedbackStorageObject(final int initValue, final String userId) {
			value = initValue;
			timestamp = new Date();
			this.userId = userId;
		}

		public int getValue() {
			return value;
		}

		public Date getTimestamp() {
			return timestamp;
		}

		public boolean fromUser(final String userId) {
			return this.userId.equals(userId);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(FeedbackStorageServiceImpl.class);

	private final Map<Room, Map<String, FeedbackStorageObject>> data =
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
	public Integer getByRoomAndUserId(final Room room, final String userId) {
		if (data.get(room) == null) {
			return null;
		}

		for (final FeedbackStorageObject fso : data.get(room).values()) {
			if (fso.fromUser(userId)) {
				return fso.getValue();
			}
		}

		return null;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void save(final Room room, final int value, final String userId) {
		logger.debug("Feedback data for {} Rooms is stored", data.size());
		logger.debug("Saving feedback: Room: {}, Value: {}, User: {}", room, value, userId);
		Map<String, FeedbackStorageObject> roomData = data.get(room);
		if (roomData == null) {
			logger.debug("Creating new feedback container for Room: {}", room);
			roomData = new ConcurrentHashMap<String, FeedbackStorageObject>();
			data.put(room, roomData);
		}
		logger.debug("Feedback values for Room {}: {}", room.getId(), roomData.size());
		roomData.put(userId, new FeedbackStorageObject(value, userId));
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Map<Room, List<String>> cleanVotes(final int cleanupFeedbackDelay) {
		final Map<Room, List<String>> removedFeedbackOfUsersInSession = new HashMap<>();
		for (final Room room : data.keySet()) {
			if (!room.getSettings().isQuickSurveyEnabled()) {
				List<String> affectedUserIds = cleanVotesByRoom(room, cleanupFeedbackDelay);
				if (!affectedUserIds.isEmpty()) {
					removedFeedbackOfUsersInSession.put(room, affectedUserIds);
				}
			}
		}
		return removedFeedbackOfUsersInSession;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public List<String> cleanVotesByRoom(final Room room, final int cleanupFeedbackDelayInMins) {
		final long timelimitInMillis = TimeUnit.MILLISECONDS.convert(cleanupFeedbackDelayInMins, TimeUnit.MINUTES);
		final Date maxAllowedTime = new Date(System.currentTimeMillis() - timelimitInMillis);
		final boolean forceClean = cleanupFeedbackDelayInMins == 0;

		final Map<String, FeedbackStorageObject> roomFeedbacks = data.get(room);
		final List<String> affectedUsers = new ArrayList<>();

		if (roomFeedbacks != null) {
			for (final Map.Entry<String, FeedbackStorageObject> entry : roomFeedbacks.entrySet()) {
				final String userId = entry.getKey();
				final FeedbackStorageObject feedback = entry.getValue();
				final boolean timeIsUp = feedback.getTimestamp().before(maxAllowedTime);
				final boolean isAwayFeedback = getByRoomAndUserId(room, userId).equals(Feedback.FEEDBACK_AWAY);
				if (forceClean || timeIsUp && !isAwayFeedback) {
					roomFeedbacks.remove(userId);
					affectedUsers.add(userId);
				}
			}
		}
		return affectedUsers;
	}
}
