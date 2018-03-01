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
import de.thm.arsnova.events.DeleteFeedbackForRoomsEvent;
import de.thm.arsnova.events.NewFeedbackEvent;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.persistance.RoomRepository;
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

	private RoomRepository roomRepository;

	private FeedbackStorageService feedbackStorage;

	private ApplicationEventPublisher publisher;

	public FeedbackServiceImpl(FeedbackStorageService feedbackStorage, RoomRepository roomRepository) {
		this.feedbackStorage = feedbackStorage;
		this.roomRepository = roomRepository;
	}

	@Override
	@Scheduled(fixedDelay = DEFAULT_SCHEDULER_DELAY)
	public void cleanFeedbackVotes() {
		Map<Room, List<ClientAuthentication>> deletedFeedbackOfUsersInSession = feedbackStorage.cleanVotes(cleanupFeedbackDelay);
		/*
		 * mapping (Room -> Users) is not suitable for web sockets, because we want to sent all affected
		 * sessions to a single user in one go instead of sending multiple messages for each session. Hence,
		 * we need the mapping (User -> Sessions)
		 */
		final Map<ClientAuthentication, Set<Room>> affectedSessionsOfUsers = new HashMap<>();

		for (Map.Entry<Room, List<ClientAuthentication>> entry : deletedFeedbackOfUsersInSession.entrySet()) {
			final Room room = entry.getKey();
			final List<ClientAuthentication> users = entry.getValue();
			for (ClientAuthentication user : users) {
				Set<Room> affectedSessions;
				if (affectedSessionsOfUsers.containsKey(user)) {
					affectedSessions = affectedSessionsOfUsers.get(user);
				} else {
					affectedSessions = new HashSet<>();
				}
				affectedSessions.add(room);
				affectedSessionsOfUsers.put(user, affectedSessions);
			}
		}
		// Send feedback reset event to all affected users
		for (Map.Entry<ClientAuthentication, Set<Room>> entry : affectedSessionsOfUsers.entrySet()) {
			final ClientAuthentication user = entry.getKey();
			final Set<Room> arsSessions = entry.getValue();
			this.publisher.publishEvent(new DeleteFeedbackForRoomsEvent(this, arsSessions, user));
		}
		// For each session that has deleted feedback, send the new feedback to all clients
		for (Room session : deletedFeedbackOfUsersInSession.keySet()) {
			this.publisher.publishEvent(new NewFeedbackEvent(this, session));
		}
	}

	@Override
	public void cleanFeedbackVotesByRoomId(final String roomId, final int cleanupFeedbackDelayInMins) {
		final Room room = roomRepository.findOne(roomId);
		List<ClientAuthentication> affectedUsers = feedbackStorage.cleanVotesByRoom(room, cleanupFeedbackDelayInMins);
		Set<Room> sessionSet = new HashSet<>();
		sessionSet.add(room);

		// Send feedback reset event to all affected users
		for (ClientAuthentication user : affectedUsers) {
			this.publisher.publishEvent(new DeleteFeedbackForRoomsEvent(this, sessionSet, user));
		}
		// send the new feedback to all clients in affected session
		this.publisher.publishEvent(new NewFeedbackEvent(this, room));
	}

	@Override
	public Feedback getByRoomId(final String roomId) {
		final Room room = roomRepository.findOne(roomId);
		if (room == null) {
			throw new NotFoundException();
		}
		return feedbackStorage.getByRoom(room);
	}

	@Override
	public int countFeedbackByRoomId(final String roomId) {
		final Feedback feedback = this.getByRoomId(roomId);
		final List<Integer> values = feedback.getValues();
		return values.get(Feedback.FEEDBACK_FASTER) + values.get(Feedback.FEEDBACK_OK)
				+ values.get(Feedback.FEEDBACK_SLOWER) + values.get(Feedback.FEEDBACK_AWAY);
	}

	@Override
	public double calculateAverageFeedback(final String roomId) {
		final Room room = roomRepository.findOne(roomId);
		if (room == null) {
			throw new NotFoundException();
		}
		final Feedback feedback = feedbackStorage.getByRoom(room);
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
	public long calculateRoundedAverageFeedback(final String roomId) {
		return Math.round(calculateAverageFeedback(roomId));
	}

	@Override
	public boolean save(final String roomId, final int value, final ClientAuthentication user) {
		final Room room = roomRepository.findOne(roomId);
		if (room == null) {
			throw new NotFoundException();
		}
		feedbackStorage.save(room, value, user);

		this.publisher.publishEvent(new NewFeedbackEvent(this, room));
		return true;
	}

	@Override
	public Integer getByRoomIdAndUser(final String roomId, final ClientAuthentication user) {
		final Room room = roomRepository.findOne(roomId);
		if (room == null) {
			throw new NotFoundException();
		}
		return feedbackStorage.getByRoomAndUser(room, user);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}
}
