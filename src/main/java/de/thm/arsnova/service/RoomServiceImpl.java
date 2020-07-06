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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.ektorp.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import de.thm.arsnova.connector.client.ConnectorClient;
import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.event.BeforeDeletionEvent;
import de.thm.arsnova.event.BeforeFullUpdateEvent;
import de.thm.arsnova.event.FlipFlashcardsEvent;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.model.transport.ImportExportContainer;
import de.thm.arsnova.model.transport.ScoreStatistics;
import de.thm.arsnova.persistence.AnswerRepository;
import de.thm.arsnova.persistence.CommentRepository;
import de.thm.arsnova.persistence.ContentRepository;
import de.thm.arsnova.persistence.LogEntryRepository;
import de.thm.arsnova.persistence.RoomRepository;
import de.thm.arsnova.security.User;
import de.thm.arsnova.security.jwt.JwtService;
import de.thm.arsnova.service.score.ScoreCalculator;
import de.thm.arsnova.service.score.ScoreCalculatorFactory;
import de.thm.arsnova.web.exceptions.BadRequestException;
import de.thm.arsnova.web.exceptions.NotFoundException;

/**
 * Performs all room related operations.
 */
@Service
public class RoomServiceImpl extends DefaultEntityServiceImpl<Room> implements RoomService {
	private static final long ROOM_INACTIVITY_CHECK_INTERVAL_MS = 30 * 60 * 1000L;

	private static final Logger logger = LoggerFactory.getLogger(RoomServiceImpl.class);

	private LogEntryRepository dbLogger;

	private RoomRepository roomRepository;

	private ContentRepository contentRepository;

	private AnswerRepository answerRepository;

	private CommentRepository commentRepository;

	private UserService userService;

	private FeedbackService feedbackService;

	private ScoreCalculatorFactory scoreCalculatorFactory;

	private ConnectorClient connectorClient;

	private JwtService jwtService;

	@Value("${system.inactivity-thresholds.delete-inactive-guest-rooms:0}")
	private int guestRoomInactivityThresholdDays;

	@Value("${features.content-pool.logo-max-filesize}")
	private int uploadFileSizeByte;

	public RoomServiceImpl(
			final RoomRepository repository,
			final LogEntryRepository dbLogger,
			final UserService userService,
			final ScoreCalculatorFactory scoreCalculatorFactory,
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
			final Validator validator,
			final JwtService jwtService) {
		super(Room.class, repository, jackson2HttpMessageConverter.getObjectMapper(), validator);
		this.roomRepository = repository;
		this.dbLogger = dbLogger;
		this.userService = userService;
		this.scoreCalculatorFactory = scoreCalculatorFactory;
		this.jwtService = jwtService;
	}

	@Autowired
	public void setCommentRepository(final CommentRepository commentRepository) {
		this.commentRepository = commentRepository;
	}

	@Autowired
	public void setContentRepository(final ContentRepository contentRepository) {
		this.contentRepository = contentRepository;
	}

	@Autowired
	public void setAnswerRepository(final AnswerRepository answerRepository) {
		this.answerRepository = answerRepository;
	}

	@Autowired
	public void setFeedbackService(final FeedbackService feedbackService) {
		this.feedbackService = feedbackService;
	}

	public static class RoomNameComparator implements Comparator<Room>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final Room room1, final Room room2) {
			return room1.getName().compareToIgnoreCase(room2.getName());
		}
	}

	public static class RoomShortNameComparator implements Comparator<Room>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final Room room1, final Room room2) {
			return room1.getAbbreviation().compareToIgnoreCase(room2.getAbbreviation());
		}
	}

	@Autowired(required = false)
	public void setConnectorClient(final ConnectorClient connectorClient) {
		this.connectorClient = connectorClient;
	}

	@EventListener
	public void handleRoomUpdate(final BeforeFullUpdateEvent<Room> event) {
		// Check if event is result of a full update from API or from adding/removing a moderator
		if (!event.getEntity().isModeratorsInitialized() && event.getEntity().getModerators().isEmpty()) {
			// When it's a result from a full update from the API, the moderators need to be loaded from the old entity
			event.getEntity().setModerators(event.getOldEntity().getModerators());
		}
	}

	@EventListener
	@Secured({"ROLE_USER", "RUN_AS_SYSTEM"})
	public void handleUserDeletion(final BeforeDeletionEvent<UserProfile> event) {
		final Iterable<Room> rooms = roomRepository.findByOwnerId(event.getEntity().getId(), -1, -1);
		delete(rooms);
	}

	@Scheduled(fixedDelay = ROOM_INACTIVITY_CHECK_INTERVAL_MS)
	public void deleteInactiveRooms() {
		if (guestRoomInactivityThresholdDays > 0) {
			logger.info("Delete inactive rooms.");
			final long unixTime = System.currentTimeMillis();
			final long lastActivityBefore = unixTime - guestRoomInactivityThresholdDays * 24 * 60 * 60 * 1000L;
			final int[] totalCount = new int[] {0, 0, 0};
			final List<Room> inactiveRooms = roomRepository.findInactiveGuestRoomsMetadata(lastActivityBefore);
			delete(inactiveRooms);

			if (!inactiveRooms.isEmpty()) {
				logger.info("Deleted {} inactive guest rooms.", inactiveRooms.size());
			}
		}
	}

	@Override
	public Room join(final String id, final UUID socketId) {
		final Room room = null != id ? get(id) : null;
		if (null == room) {
			userService.removeUserFromRoomBySocketId(socketId);
			return null;
		}

		/* FIXME: migrate LMS course support
		if (connectorClient != null && room.isCourseSession()) {
			final String courseid = room.getCourseId();
			if (!connectorClient.getMembership(user.getUsername(), courseid).isMember()) {
				throw new ForbiddenException("User is no course member.");
			}
		}
		*/

		userService.addUserToRoomBySocketId(socketId, id);
		userService.addRoomToHistory(userService.getCurrentUserProfile(), room);

		return room;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Room getByShortId(final String shortId) {
		return get(getIdByShortId(shortId));
	}

	@Override
	@Cacheable("room.id-by-shortid")
	public String getIdByShortId(final String shortId) {
		if (shortId == null) {
			throw new NullPointerException("shortId cannot be null");
		}
		final Room room = roomRepository.findByShortId(shortId);
		if (room == null) {
			throw new NotFoundException("No Room exists for short ID");
		}

		return room.getId();
	}

	@PreAuthorize("hasPermission(#id, 'room', 'owner')")
	public Room getForAdmin(final String id) {
		return get(id);
	}

	@Override
	public Room get(final String id) {
		final Room room = super.get(id);

		/* FIXME: migrate LMS course support
		if (connectorClient != null && room.isCourseSession()) {
			final String courseid = room.getCourseId();
			if (!connectorClient.getMembership(user.getUsername(), courseid).isMember()) {
				throw new ForbiddenException("User is no course member.");
			}
		}
		*/

		return room;
	}

	/* TODO: Updated SpEL expression has not been tested yet */
	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#userId, 'userprofile', 'owner')")
	public List<Room> getUserRooms(final String userId) {
		return roomRepository.findByOwnerId(userId, 0, 0);
	}

	/* TODO: Updated SpEL expression has not been tested yet */
	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#userId, 'userprofile', 'owner')")
	public List<String> getUserRoomIds(final String userId) {
		return roomRepository.findIdsByOwnerId(userId);
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#userId, 'userprofile', 'owner')")
	public List<String> getRoomIdsByModeratorId(final String userId) {
		return roomRepository.findIdsByModeratorId(userId);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getMyRooms(final int offset, final int limit) {
		return roomRepository.findByOwnerId(userService.getCurrentUser().getId(), offset, limit);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getPublicPoolRoomsInfo() {
		return roomRepository.findInfosForPublicPool();
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getMyPublicPoolRoomsInfo() {
		return roomRepository.findInfosForPublicPoolByOwnerId(userService.getCurrentUser().getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getMyRoomsInfo(final int offset, final int limit) {
		final User user = userService.getCurrentUser();
		return roomRepository.getRoomsWithStatsForOwnerId(user.getId(), offset, limit);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getMyRoomHistory(final int offset, final int limit) {
		/* TODO: implement pagination */
		return getUserRoomHistory(userService.getCurrentUser().getId());
	}

	@Override
	@PreAuthorize("hasPermission(#userId, 'userprofile', 'read')")
	public List<Room> getUserRoomHistory(final String userId) {
		final UserProfile profile = userService.get(userId);
		final List<String> roomIds = profile.getRoomHistory().stream()
				.map(entry -> entry.getRoomId()).collect(Collectors.toList());
		final List<Room> rooms = new ArrayList<>();
		roomRepository.findAllById(roomIds).forEach(rooms::add);

		return rooms;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getMyRoomHistoryInfo(final int offset, final int limit) {
		final List<Room> rooms = getMyRoomHistory(0, 0);
		roomRepository.getRoomHistoryWithStatsForUser(rooms, userService.getCurrentUser().getId());

		return rooms;
	}

	@Override
	/* TODO: move caching to DefaultEntityServiceImpl */
	//@Caching(evict = @CacheEvict(cacheNames = "rooms", key = "#result.id"))
	public void prepareCreate(final Room room) {
		/* FIXME: migrate LMS course support
		if (connectorClient != null && room.getCourseId() != null) {
			if (!connectorClient.getMembership(
					userService.getCurrentUser().getUsername(), room.getCourseId()).isMember()) {
				throw new ForbiddenException();
			}
		}
		*/

		handleLogo(room);

		final Room.Settings sf = new Room.Settings();
		room.setSettings(sf);

		room.setShortId(generateShortId());
		if (room.getOwnerId() == null) {
			room.setOwnerId(userService.getCurrentUser().getId());
		}
		room.setClosed(false);

		/* FIXME: event */
		// this.publisher.publishEvent(new NewRoomEvent(this, result));
	}

	@Override
	public boolean isShortIdAvailable(final String shortId) {
		try {
			return getIdByShortId(shortId) == null;
		} catch (final NotFoundException e) {
			return true;
		}
	}

	@Override
	public String generateShortId() {
		final int low = 10000000;
		final int high = 100000000;
		final String keyword = String
				.valueOf((int) (Math.random() * (high - low) + low));

		if (isShortIdAvailable(keyword)) {
			return keyword;
		}
		return generateShortId();
	}

	@Override
	public int countRoomsByCourses(final List<Course> courses) {
		final List<Room> rooms = roomRepository.findRoomsByCourses(courses);
		if (rooms == null) {
			return 0;
		}
		return rooms.size();
	}

	@Override
	public int activeUsers(final String id) {
		return userService.getUsersByRoomId(id).size();
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'owner')")
	public Room setActive(final String id, final Boolean lock) throws IOException {
		final Room room = get(id);
		patch(room, Collections.singletonMap("closed", lock));

		return room;
	}

	@Override
	/* TODO: move caching to DefaultEntityServiceImpl */
	//@CachePut(value = "rooms", key = "#room")
	protected void prepareUpdate(final Room room) {
		final Room existingRoom = get(room.getId());
		if (room.getOwnerId() == null) {
			room.setOwnerId(existingRoom.getOwnerId());
		}
		handleLogo(room);

		/* TODO: only publish event when feedback has changed */
		/* FIXME: event */
		// this.publisher.publishEvent(new FeatureChangeEvent(this, room));
	}

	@Override
	@PreAuthorize("hasPermission('', 'motd', 'admin')")
	@Caching(evict = { @CacheEvict("rooms"), @CacheEvict(cacheNames = "rooms", key = "#id") })
	public Room updateCreator(final String id, final String newCreator) {
		throw new UnsupportedOperationException("No longer implemented.");
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public Room transferOwnership(final Room room, final String newOwnerId) {
		final UserProfile newOwner;
		try {
			newOwner = userService.get(newOwnerId);
		} catch (final DocumentNotFoundException e) {
			throw new BadRequestException("Invalid user ID.", e);
		}
		room.setOwnerId(newOwner.getId());

		return update(room);
	}

	@Override
	@PreAuthorize("hasPermission(#room, 'owner')")
	public Room transferOwnershipThroughToken(final Room room, final String targetUserToken) {
		final User user = jwtService.verifyToken(targetUserToken);
		room.setOwnerId(user.getId());
		return update(room);
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'read')")
	public ScoreStatistics getLearningProgress(final String id, final String type, final String questionVariant) {
		final Room room = get(id);
		final ScoreCalculator scoreCalculator = scoreCalculatorFactory.create(type, questionVariant);
		return scoreCalculator.getCourseProgress(room);
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'read')")
	public ScoreStatistics getMyLearningProgress(final String id, final String type, final String questionVariant) {
		final Room room = get(id);
		final User user = userService.getCurrentUser();
		final ScoreCalculator scoreCalculator = scoreCalculatorFactory.create(type, questionVariant);
		return scoreCalculator.getMyProgress(room, user.getId());
	}

	@Override
	@PreAuthorize("hasPermission('', 'room', 'create')")
	public Room importRooms(final ImportExportContainer importRoom) {
		final User user = userService.getCurrentUser();
		final Room info = roomRepository.importRoom(user.getId(), importRoom);
		if (info == null) {
			throw new NullPointerException("Could not import room.");
		}
		return info;
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'owner')")
	public ImportExportContainer exportRoom(
			final String id, final Boolean withAnswerStatistics, final Boolean withFeedbackQuestions) {
		return roomRepository.exportRoom(id, withAnswerStatistics, withFeedbackQuestions);
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'owner')")
	public Room copyRoomToPublicPool(final String id, final ImportExportContainer.PublicPool pp) {
		final ImportExportContainer temp = roomRepository.exportRoom(id, false, false);
		temp.getSession().setPublicPool(pp);
		temp.getSession().setSessionType("public_pool");
		final User user = userService.getCurrentUser();
		return roomRepository.importRoom(user.getId(), temp);
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'read')")
	public Room.Settings getFeatures(final String id) {
		return get(id).getSettings();
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'owner')")
	public Room.Settings updateFeatures(final String id, final Room.Settings settings) {
		final Room room = get(id);
		room.setSettings(settings);

		update(room);

		return room.getSettings();
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'owner')")
	public boolean lockFeedbackInput(final String id, final Boolean lock) throws IOException {
		final Room room = get(id);
		if (!lock) {
			feedbackService.cleanFeedbackVotesByRoomId(id, 0);
		}
		patch(room, Collections.singletonMap("feedbackLocked", lock), Room::getSettings);

		return room.getSettings().isFeedbackLocked();
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'owner')")
	public boolean flipFlashcards(final String id, final Boolean flip) {
		final Room room = get(id);
		this.eventPublisher.publishEvent(new FlipFlashcardsEvent(this, room.getId()));

		return flip;
	}

	private void handleLogo(final Room room) {
		if (room.getAuthor() != null && room.getAuthor().getOrganizationLogo() != null) {
			if (!room.getAuthor().getOrganizationLogo().startsWith("http")) {
				throw new IllegalArgumentException("Invalid logo URL.");
			}
		}
	}
}
