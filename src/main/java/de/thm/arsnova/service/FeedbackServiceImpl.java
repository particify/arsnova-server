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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import de.thm.arsnova.event.DeleteFeedbackForRoomsEvent;
import de.thm.arsnova.event.NewFeedbackEvent;
import de.thm.arsnova.model.Feedback;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.web.exceptions.NotFoundException;

/**
 * Performs all feedback related operations.
 */
@Service
public class FeedbackServiceImpl implements FeedbackService, ApplicationEventPublisherAware {

	private static final int DEFAULT_SCHEDULER_DELAY = 5000;

	/**
	 * Minutes, after which the feedback is deleted.
	 */
	@Value("${features.live-feedback.reset-interval}")
	private int cleanupFeedbackDelay;

	private RoomService roomService;

	private FeedbackStorageService feedbackStorage;

	private ApplicationEventPublisher publisher;

	public FeedbackServiceImpl(final FeedbackStorageService feedbackStorage, final RoomService roomService) {
		this.feedbackStorage = feedbackStorage;
		this.roomService = roomService;
	}

	@Override
	@Scheduled(fixedDelay = DEFAULT_SCHEDULER_DELAY)
	public void cleanFeedbackVotes() {
		final Map<Room, List<String>> deletedFeedbackOfUsersInSession = feedbackStorage.cleanVotes(cleanupFeedbackDelay);
		/*
		 * mapping (Room -> Users) is not suitable for web sockets, because we want to sent all affected
		 * sessions to a single user in one go instead of sending multiple messages for each session. Hence,
		 * we need the mapping (User -> Sessions)
		 */
		final Map<String, Set<Room>> affectedSessionsOfUsers = new HashMap<>();

		for (final Map.Entry<Room, List<String>> entry : deletedFeedbackOfUsersInSession.entrySet()) {
			final Room room = entry.getKey();
			final List<String> userIds = entry.getValue();
			for (final String userId : userIds) {
				final Set<Room> affectedSessions;
				if (affectedSessionsOfUsers.containsKey(userId)) {
					affectedSessions = affectedSessionsOfUsers.get(userId);
				} else {
					affectedSessions = new HashSet<>();
				}
				affectedSessions.add(room);
				affectedSessionsOfUsers.put(userId, affectedSessions);
			}
		}
		// Send feedback reset event to all affected users
		for (final Map.Entry<String, Set<Room>> entry : affectedSessionsOfUsers.entrySet()) {
			final String userId = entry.getKey();
			final Set<Room> rooms = entry.getValue();
			this.publisher.publishEvent(new DeleteFeedbackForRoomsEvent(this, rooms, userId));
		}
		// For each session that has deleted feedback, send the new feedback to all clients
		for (final Room room : deletedFeedbackOfUsersInSession.keySet()) {
			this.publisher.publishEvent(new NewFeedbackEvent(this, room.getId()));
		}
	}

	@Override
	public void cleanFeedbackVotesByRoomId(final String roomId, final int cleanupFeedbackDelayInMins) {
		final Room room = roomService.get(roomId);
		final List<String> affectedUserIds = feedbackStorage.cleanVotesByRoom(room, cleanupFeedbackDelayInMins);
		final Set<Room> sessionSet = new HashSet<>();
		sessionSet.add(room);

		// Send feedback reset event to all affected users
		for (final String userId : affectedUserIds) {
			this.publisher.publishEvent(new DeleteFeedbackForRoomsEvent(this, sessionSet, userId));
		}
		// send the new feedback to all clients in affected session
		this.publisher.publishEvent(new NewFeedbackEvent(this, room.getId()));
	}

	@Override
	public Feedback getByRoomId(final String roomId) {
		final Room room = roomService.get(roomId);
		if (room == null) {
			throw new NotFoundException();
		}
		return feedbackStorage.getByRoom(room);
	}

	@Override
	public int countFeedbackByRoomId(final String roomId) {
		final Feedback feedback = this.getByRoomId(roomId);
		return feedback.getCount();
	}

	@Override
	public boolean save(final String roomId, final int value, final String userId) {
		final Room room = roomService.get(roomId);
		if (room == null) {
			throw new NotFoundException();
		}
		feedbackStorage.save(room, value, userId);

		this.publisher.publishEvent(new NewFeedbackEvent(this, room.getId()));
		return true;
	}

	@Override
	public Integer getByRoomIdAndUserId(final String roomId, final String userId) {
		final Room room = roomService.get(roomId);
		if (room == null) {
			throw new NotFoundException();
		}
		return feedbackStorage.getByRoomAndUserId(room, userId);
	}

	@Override
	public void setApplicationEventPublisher(final ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}
}
