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

import de.thm.arsnova.connector.client.ConnectorClient;
import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.entities.transport.ImportExportContainer;
import de.thm.arsnova.entities.transport.ScoreStatistics;
import de.thm.arsnova.events.DeleteRoomEvent;
import de.thm.arsnova.events.FeatureChangeEvent;
import de.thm.arsnova.events.FlipFlashcardsEvent;
import de.thm.arsnova.events.LockFeedbackEvent;
import de.thm.arsnova.events.NewRoomEvent;
import de.thm.arsnova.events.StatusRoomEvent;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.persistance.AnswerRepository;
import de.thm.arsnova.persistance.CommentRepository;
import de.thm.arsnova.persistance.ContentRepository;
import de.thm.arsnova.persistance.LogEntryRepository;
import de.thm.arsnova.persistance.RoomRepository;
import de.thm.arsnova.services.score.ScoreCalculator;
import de.thm.arsnova.services.score.ScoreCalculatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Performs all room related operations.
 */
@Service
public class RoomServiceImpl extends DefaultEntityServiceImpl<Room> implements RoomService, ApplicationEventPublisherAware {
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

	@Value("${session.guest-session.cleanup-days:0}")
	private int guestRoomInactivityThresholdDays;

	@Value("${pp.logofilesize_b}")
	private int uploadFileSizeByte;

	private ApplicationEventPublisher publisher;

	public RoomServiceImpl(
			RoomRepository repository,
			ContentRepository contentRepository,
			AnswerRepository answerRepository,
			CommentRepository commentRepository,
			LogEntryRepository dbLogger,
			UserService userService,
			FeedbackService feedbackService,
			ScoreCalculatorFactory scoreCalculatorFactory,
			@Qualifier("defaultJsonMessageConverter") MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
		super(Room.class, repository, jackson2HttpMessageConverter.getObjectMapper());
		this.roomRepository = repository;
		this.contentRepository = contentRepository;
		this.answerRepository = answerRepository;
		this.commentRepository = commentRepository;
		this.dbLogger = dbLogger;
		this.userService = userService;
		this.feedbackService = feedbackService;
		this.scoreCalculatorFactory = scoreCalculatorFactory;
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
	public void setConnectorClient(ConnectorClient connectorClient) {
		this.connectorClient = connectorClient;
	}

	@Scheduled(fixedDelay = ROOM_INACTIVITY_CHECK_INTERVAL_MS)
	public void deleteInactiveRooms() {
		if (guestRoomInactivityThresholdDays > 0) {
			logger.info("Delete inactive rooms.");
			long unixTime = System.currentTimeMillis();
			long lastActivityBefore = unixTime - guestRoomInactivityThresholdDays * 24 * 60 * 60 * 1000L;
			int totalCount[] = new int[] {0, 0, 0};
			List<Room> inactiveRooms = roomRepository.findInactiveGuestRoomsMetadata(lastActivityBefore);
			for (Room room : inactiveRooms) {
				int[] count = deleteCascading(room);
				totalCount[0] += count[0];
				totalCount[1] += count[1];
				totalCount[2] += count[2];
			}

			if (!inactiveRooms.isEmpty()) {
				logger.info("Deleted {} inactive guest rooms.", inactiveRooms.size());
				dbLogger.log("cleanup", "type", "session",
						"sessionCount", inactiveRooms.size(),
						"questionCount", totalCount[0],
						"answerCount", totalCount[1],
						"commentCount", totalCount[2]);
			}
		}
	}

	@Override
	public Room join(final String id, final UUID socketId) {
		Room room = null != id ? roomRepository.findOne(id) : null;
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
		final ClientAuthentication user = userService.getCurrentUser();
		return this.getInternal(getIdByShortId(shortId), user);
	}

	@Override
	@Cacheable("room.id-by-shortid")
	public String getIdByShortId(final String shortId) {
		if (shortId == null) {
			throw new NullPointerException("shortId cannot be null");
		}
		Room room = roomRepository.findByShortId(shortId);
		if (room == null) {
			throw new NotFoundException("No Room exists for short ID");
		}

		return room.getId();
	}

	@PreAuthorize("hasPermission(#id, 'room', 'owner')")
	public Room getForAdmin(final String id) {
		return roomRepository.findOne(id);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public Room getInternal(final String id, final ClientAuthentication user) {
		final Room room = roomRepository.findOne(id);
		if (room == null) {
			throw new NotFoundException();
		}
		if (room.isClosed() && !room.getOwnerId().equals(user.getId())) {
			throw new ForbiddenException("User is not room creator.");
		}

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
	public List<Room> getUserRooms(String userId) {
		return roomRepository.findByOwnerId(userId, 0, 0);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getMyRooms(final int offset, final int limit) {
		return roomRepository.findByOwner(userService.getCurrentUser(), offset, limit);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getPublicPoolRoomsInfo() {
		return roomRepository.findInfosForPublicPool();
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getMyPublicPoolRoomsInfo() {
		return roomRepository.findInfosForPublicPoolByOwner(userService.getCurrentUser());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getMyRoomsInfo(final int offset, final int limit) {
		final ClientAuthentication user = userService.getCurrentUser();
		return roomRepository.getRoomsWithStatsForOwner(user, offset, limit);
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
		final List<String> roomIds = profile.getRoomHistory().stream().map(entry -> entry.getRoomId()).collect(Collectors.toList());
		List<Room> rooms = new ArrayList<>();
		roomRepository.findAll(roomIds).forEach(rooms::add);

		return rooms;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getMyRoomHistoryInfo(final int offset, final int limit) {
		List<Room> rooms = getMyRoomHistory(0, 0);
		roomRepository.getRoomHistoryWithStatsForUser(rooms, userService.getCurrentUser());

		return rooms;
	}

	@Override
	@PreAuthorize("hasPermission(#room, 'create')")
	@Caching(evict = @CacheEvict(cacheNames = "rooms", key = "#result.id"))
	public Room save(final Room room) {
		/* FIXME: migrate LMS course support
		if (connectorClient != null && room.getCourseId() != null) {
			if (!connectorClient.getMembership(
					userService.getCurrentUser().getUsername(), room.getCourseId()).isMember()
					) {
				throw new ForbiddenException();
			}
		}
		*/

		handleLogo(room);

		Room.Settings sf = new Room.Settings();
		room.setSettings(sf);

		room.setShortId(generateShortId());
		room.setCreationTimestamp(new Date());
		room.setOwnerId(userService.getCurrentUser().getId());
		room.setClosed(false);

		final Room result = super.create(room);
		this.publisher.publishEvent(new NewRoomEvent(this, result));
		return result;
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
	public Room setActive(final String id, final Boolean lock) {
		final Room room = roomRepository.findOne(id);
		room.setClosed(!lock);
		this.publisher.publishEvent(new StatusRoomEvent(this, room));
		roomRepository.save(room);

		return room;
	}

	@Override
	@PreAuthorize("hasPermission(#room.id, 'room', 'owner')")
	@CachePut(value = "rooms", key = "#room")
	public Room update(final String id, final Room room) {
		final Room existingRoom = roomRepository.findOne(id);
		room.setOwnerId(existingRoom.getOwnerId());
		handleLogo(room);
		update(existingRoom, room);
		/* TODO: only publish event when feedback has changed */
		this.publisher.publishEvent(new FeatureChangeEvent(this, room));

		return room;
	}

	@Override
	@PreAuthorize("hasPermission('', 'motd', 'admin')")
	@Caching(evict = { @CacheEvict("rooms"), @CacheEvict(cacheNames = "rooms", key = "#id") })
	public Room updateCreator(String id, String newCreator) {
		throw new UnsupportedOperationException("No longer implemented.");
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public Room updateInternal(final Room room, final ClientAuthentication user) {
		if (room.getOwnerId().equals(user.getId())) {
			roomRepository.save(room);
			return room;
		}
		return null;
	}

	@Override
	@PreAuthorize("hasPermission(#room, 'owner')")
	@Caching(evict = {
			@CacheEvict("rooms"),
			@CacheEvict(value = "room.id-by-shortid", key = "#room.shortId")
	})
	public int[] deleteCascading(final Room room) {
		int[] count = new int[] {0, 0, 0};
		List<String> contentIds = contentRepository.findIdsByRoomId(room.getId());
		count[2] = commentRepository.deleteByRoomId(room.getId());
		count[1] = answerRepository.deleteByContentIds(contentIds);
		count[0] = contentRepository.deleteByRoomId(room.getId());
		roomRepository.delete(room);
		logger.debug("Deleted room document {} and related data.", room.getId());
		dbLogger.log("delete", "type", "session", "id", room.getId());

		this.publisher.publishEvent(new DeleteRoomEvent(this, room));

		return count;
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'read')")
	public ScoreStatistics getLearningProgress(final String id, final String type, final String questionVariant) {
		final Room room = roomRepository.findOne(id);
		ScoreCalculator scoreCalculator = scoreCalculatorFactory.create(type, questionVariant);
		return scoreCalculator.getCourseProgress(room);
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'read')")
	public ScoreStatistics getMyLearningProgress(final String id, final String type, final String questionVariant) {
		final Room room = roomRepository.findOne(id);
		final ClientAuthentication user = userService.getCurrentUser();
		ScoreCalculator scoreCalculator = scoreCalculatorFactory.create(type, questionVariant);
		return scoreCalculator.getMyProgress(room, user);
	}

	@Override
	@PreAuthorize("hasPermission('', 'room', 'create')")
	public Room importRooms(ImportExportContainer importRoom) {
		final ClientAuthentication user = userService.getCurrentUser();
		final Room info = roomRepository.importRoom(user, importRoom);
		if (info == null) {
			throw new NullPointerException("Could not import room.");
		}
		return info;
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'owner')")
	public ImportExportContainer exportRoom(String id, Boolean withAnswerStatistics, Boolean withFeedbackQuestions) {
		return roomRepository.exportRoom(id, withAnswerStatistics, withFeedbackQuestions);
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'owner')")
	public Room copyRoomToPublicPool(String id, ImportExportContainer.PublicPool pp) {
		ImportExportContainer temp = roomRepository.exportRoom(id, false, false);
		temp.getSession().setPublicPool(pp);
		temp.getSession().setSessionType("public_pool");
		final ClientAuthentication user = userService.getCurrentUser();
		return roomRepository.importRoom(user, temp);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'read')")
	public Room.Settings getFeatures(String id) {
		return roomRepository.findOne(id).getSettings();
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'owner')")
	public Room.Settings updateFeatures(String id, Room.Settings settings) {
		final Room room = roomRepository.findOne(id);
		final ClientAuthentication user = userService.getCurrentUser();
		room.setSettings(settings);
		this.publisher.publishEvent(new FeatureChangeEvent(this, room));
		roomRepository.save(room);

		return room.getSettings();
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'owner')")
	public boolean lockFeedbackInput(String id, Boolean lock) {
		final Room room = roomRepository.findOne(id);
		final ClientAuthentication user = userService.getCurrentUser();
		if (!lock) {
			feedbackService.cleanFeedbackVotesByRoomId(id, 0);
		}

		room.getSettings().setFeedbackLocked(lock);
		this.publisher.publishEvent(new LockFeedbackEvent(this, room));
		roomRepository.save(room);

		return room.getSettings().isFeedbackLocked();
	}

	@Override
	@PreAuthorize("hasPermission(#id, 'room', 'owner')")
	public boolean flipFlashcards(String id, Boolean flip) {
		final Room room = roomRepository.findOne(id);
		this.publisher.publishEvent(new FlipFlashcardsEvent(this, room));

		return flip;
	}

	private void handleLogo(Room room) {
		if (room.getAuthor() != null && room.getAuthor().getOrganizationLogo() != null) {
			if (!room.getAuthor().getOrganizationLogo().startsWith("http")) {
				throw new IllegalArgumentException("Invalid logo URL.");
			}
		}
	}
}
