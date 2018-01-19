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
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.entities.transport.ImportExportSession;
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
import de.thm.arsnova.persistance.VisitedSessionRepository;
import de.thm.arsnova.services.score.ScoreCalculator;
import de.thm.arsnova.services.score.ScoreCalculatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
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
 * Performs all session related operations.
 */
@Service
public class RoomServiceImpl extends DefaultEntityServiceImpl<Room> implements RoomService, ApplicationEventPublisherAware {
	private static final long SESSION_INACTIVITY_CHECK_INTERVAL_MS = 30 * 60 * 1000L;

	private static final Logger logger = LoggerFactory.getLogger(RoomServiceImpl.class);

	private LogEntryRepository dbLogger;

	private RoomRepository roomRepository;

	private ContentRepository contentRepository;

	private AnswerRepository answerRepository;

	private CommentRepository commentRepository;

	private VisitedSessionRepository visitedSessionRepository;

	private UserService userService;

	private FeedbackService feedbackService;

	private ScoreCalculatorFactory scoreCalculatorFactory;

	private ConnectorClient connectorClient;

	@Value("${session.guest-session.cleanup-days:0}")
	private int guestSessionInactivityThresholdDays;

	@Value("${pp.logofilesize_b}")
	private int uploadFileSizeByte;

	private ApplicationEventPublisher publisher;

	public RoomServiceImpl(
			RoomRepository repository,
			ContentRepository contentRepository,
			AnswerRepository answerRepository,
			CommentRepository commentRepository,
			VisitedSessionRepository visitedSessionRepository,
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
		this.visitedSessionRepository = visitedSessionRepository;
		this.dbLogger = dbLogger;
		this.userService = userService;
		this.feedbackService = feedbackService;
		this.scoreCalculatorFactory = scoreCalculatorFactory;
	}

	public static class SessionNameComparator implements Comparator<Room>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final Room room1, final Room room2) {
			return room1.getName().compareToIgnoreCase(room2.getName());
		}
	}

	public static class SessionShortNameComparator implements Comparator<Room>, Serializable {
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

	@Scheduled(fixedDelay = SESSION_INACTIVITY_CHECK_INTERVAL_MS)
	public void deleteInactiveSessions() {
		if (guestSessionInactivityThresholdDays > 0) {
			logger.info("Delete inactive sessions.");
			long unixTime = System.currentTimeMillis();
			long lastActivityBefore = unixTime - guestSessionInactivityThresholdDays * 24 * 60 * 60 * 1000L;
			int totalCount[] = new int[] {0, 0, 0};
			List<Room> inactiveRooms = roomRepository.findInactiveGuestSessionsMetadata(lastActivityBefore);
			for (Room room : inactiveRooms) {
				int[] count = deleteCascading(room);
				totalCount[0] += count[0];
				totalCount[1] += count[1];
				totalCount[2] += count[2];
			}

			if (!inactiveRooms.isEmpty()) {
				logger.info("Deleted {} inactive guest sessions.", inactiveRooms.size());
				dbLogger.log("cleanup", "type", "session",
						"sessionCount", inactiveRooms.size(),
						"questionCount", totalCount[0],
						"answerCount", totalCount[1],
						"commentCount", totalCount[2]);
			}
		}
	}

	@Scheduled(fixedDelay = SESSION_INACTIVITY_CHECK_INTERVAL_MS)
	public void deleteInactiveVisitedSessionLists() {
		if (guestSessionInactivityThresholdDays > 0) {
			logger.info("Delete lists of visited session for inactive users.");
			long unixTime = System.currentTimeMillis();
			long lastActivityBefore = unixTime - guestSessionInactivityThresholdDays * 24 * 60 * 60 * 1000L;
			visitedSessionRepository.deleteInactiveGuestVisitedSessionLists(lastActivityBefore);
		}
	}

	@Override
	public Room join(final String keyword, final UUID socketId) {
		/* Socket.IO solution */

		Room room = null != keyword ? roomRepository.findByKeyword(keyword) : null;

		if (null == room) {
			userService.removeUserFromSessionBySocketId(socketId);
			return null;
		}
		final UserAuthentication user = userService.getUser2SocketId(socketId);

		userService.addUserToSessionBySocketId(socketId, keyword);

		roomRepository.registerAsOnlineUser(user, room);

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

	@Override
	@PreAuthorize("isAuthenticated()")
	public Room getByKey(final String keyword) {
		final UserAuthentication user = userService.getCurrentUser();
		return this.getInternal(keyword, user);
	}

	@PreAuthorize("hasPermission(#sessionkey, 'session', 'owner')")
	public Room getForAdmin(final String keyword) {
		return roomRepository.findByKeyword(keyword);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public Room getInternal(final String keyword, final UserAuthentication user) {
		final Room room = roomRepository.findByKeyword(keyword);
		if (room == null) {
			throw new NotFoundException();
		}
		if (room.isClosed()) {
			if (user.hasRole(UserRoomService.Role.STUDENT)) {
				throw new ForbiddenException("User is not session creator.");
			} else if (user.hasRole(UserRoomService.Role.SPEAKER) && !room.getOwnerId().equals(user.getId())) {
				throw new ForbiddenException("User is not session creator.");
			}
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

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#sessionkey, 'session', 'owner')")
	public List<Room> getUserSessions(String username) {
		return roomRepository.findByUsername(username, 0, 0);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getMySessions(final int offset, final int limit) {
		return roomRepository.findByUser(userService.getCurrentUser(), offset, limit);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getPublicPoolSessionsInfo() {
		return roomRepository.findInfosForPublicPool();
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getMyPublicPoolSessionsInfo() {
		return roomRepository.findInfosForPublicPoolByUser(userService.getCurrentUser());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getMySessionsInfo(final int offset, final int limit) {
		final UserAuthentication user = userService.getCurrentUser();
		return roomRepository.getRoomsWithStatsForUser(user, offset, limit);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getMyVisitedSessions(final int offset, final int limit) {
		/* TODO: implement pagination */
		return getUserVisitedSessions(userService.getCurrentUser().getUsername());
	}

	@Override
	@PreAuthorize("hasPermission('', 'motd', 'admin')")
	public List<Room> getUserVisitedSessions(String username) {
		UserProfile profile = userService.getByUsername(username);
		List<String> roomIds = profile.getRoomHistory().stream().map(entry -> entry.getRoomId()).collect(Collectors.toList());
		roomRepository.findAll(roomIds);
		List<Room> rooms = new ArrayList<>();
		roomRepository.findAll(roomIds).forEach(rooms::add);

		return rooms;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Room> getMyVisitedSessionsInfo(final int offset, final int limit) {
		List<Room> rooms = getMyVisitedSessions(0, 0);
		roomRepository.getVisitedRoomsWithStatsForUser(rooms, userService.getCurrentUser());

		return rooms;
	}

	@Override
	@PreAuthorize("hasPermission('', 'session', 'create')")
	@Caching(evict = @CacheEvict(cacheNames = "rooms", key = "#result.keyword"))
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

		room.setShortId(generateKey());
		room.setCreationTimestamp(new Date());
		room.setOwnerId(userService.getCurrentUser().getId());
		room.setClosed(false);

		final Room result = save(room);
		this.publisher.publishEvent(new NewRoomEvent(this, result));
		return result;
	}

	@Override
	public boolean isKeyAvailable(final String keyword) {
		return getByKey(keyword) == null;
	}

	@Override
	public String generateKey() {
		final int low = 10000000;
		final int high = 100000000;
		final String keyword = String
				.valueOf((int) (Math.random() * (high - low) + low));

		if (isKeyAvailable(keyword)) {
			return keyword;
		}
		return generateKey();
	}

	@Override
	public int countSessionsByCourses(final List<Course> courses) {
		final List<Room> sessions = roomRepository.findSessionsByCourses(courses);
		if (sessions == null) {
			return 0;
		}
		return sessions.size();
	}

	@Override
	public int activeUsers(final String sessionkey) {
		return userService.getUsersBySessionKey(sessionkey).size();
	}

	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'owner')")
	public Room setActive(final String sessionkey, final Boolean lock) {
		final Room room = roomRepository.findByKeyword(sessionkey);
		room.setClosed(!lock);
		this.publisher.publishEvent(new StatusRoomEvent(this, room));
		roomRepository.save(room);

		return room;
	}

	@Override
	@PreAuthorize("hasPermission(#room, 'owner')")
	@CachePut(value = "rooms", key = "#room")
	public Room update(final String sessionkey, final Room room) {
		final Room existingSession = roomRepository.findByKeyword(sessionkey);
		handleLogo(room);
		update(existingSession, room);

		return room;
	}

	@Override
	@PreAuthorize("hasPermission('', 'motd', 'admin')")
	@Caching(evict = { @CacheEvict("rooms"), @CacheEvict(cacheNames = "rooms", key = "#sessionkey.keyword") })
	public Room updateCreator(String sessionkey, String newCreator) {
		throw new UnsupportedOperationException("No longer implemented.");
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public Room updateInternal(final Room room, final UserAuthentication user) {
		if (room.getOwnerId().equals(user.getId())) {
			roomRepository.save(room);
			return room;
		}
		return null;
	}

	@Override
	@PreAuthorize("hasPermission(#room, 'owner')")
	@CacheEvict("rooms")
	public int[] deleteCascading(final Room room) {
		int[] count = new int[] {0, 0, 0};
		List<String> contentIds = contentRepository.findIdsBySessionId(room.getId());
		count[2] = commentRepository.deleteBySessionId(room.getId());
		count[1] = answerRepository.deleteByContentIds(contentIds);
		count[0] = contentRepository.deleteBySessionId(room.getId());
		roomRepository.delete(room);
		logger.debug("Deleted session document {} and related data.", room.getId());
		dbLogger.log("delete", "type", "session", "id", room.getId());

		this.publisher.publishEvent(new DeleteRoomEvent(this, room));

		return count;
	}

	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'read')")
	public ScoreStatistics getLearningProgress(final String sessionkey, final String type, final String questionVariant) {
		final Room room = roomRepository.findByKeyword(sessionkey);
		ScoreCalculator scoreCalculator = scoreCalculatorFactory.create(type, questionVariant);
		return scoreCalculator.getCourseProgress(room);
	}

	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'read')")
	public ScoreStatistics getMyLearningProgress(final String sessionkey, final String type, final String questionVariant) {
		final Room room = roomRepository.findByKeyword(sessionkey);
		final UserAuthentication user = userService.getCurrentUser();
		ScoreCalculator scoreCalculator = scoreCalculatorFactory.create(type, questionVariant);
		return scoreCalculator.getMyProgress(room, user);
	}

	@Override
	@PreAuthorize("hasPermission('', 'session', 'create')")
	public Room importSession(ImportExportSession importSession) {
		final UserAuthentication user = userService.getCurrentUser();
		final Room info = roomRepository.importSession(user, importSession);
		if (info == null) {
			throw new NullPointerException("Could not import session.");
		}
		return info;
	}

	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'owner')")
	public ImportExportSession exportSession(String sessionkey, Boolean withAnswerStatistics, Boolean withFeedbackQuestions) {
		return roomRepository.exportSession(sessionkey, withAnswerStatistics, withFeedbackQuestions);
	}

	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'owner')")
	public Room copySessionToPublicPool(String sessionkey, de.thm.arsnova.entities.transport.ImportExportSession.PublicPool pp) {
		ImportExportSession temp = roomRepository.exportSession(sessionkey, false, false);
		temp.getSession().setPublicPool(pp);
		temp.getSession().setSessionType("public_pool");
		final UserAuthentication user = userService.getCurrentUser();
		return roomRepository.importSession(user, temp);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'read')")
	public Room.Settings getFeatures(String sessionkey) {
		return roomRepository.findByKeyword(sessionkey).getSettings();
	}

	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'owner')")
	public Room.Settings updateFeatures(String sessionkey, Room.Settings settings) {
		final Room room = roomRepository.findByKeyword(sessionkey);
		final UserAuthentication user = userService.getCurrentUser();
		room.setSettings(settings);
		this.publisher.publishEvent(new FeatureChangeEvent(this, room));
		roomRepository.save(room);

		return room.getSettings();
	}

	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'owner')")
	public boolean lockFeedbackInput(String sessionkey, Boolean lock) {
		final Room room = roomRepository.findByKeyword(sessionkey);
		final UserAuthentication user = userService.getCurrentUser();
		if (!lock) {
			feedbackService.cleanFeedbackVotesBySessionKey(sessionkey, 0);
		}

		room.getSettings().setFeedbackLocked(lock);
		this.publisher.publishEvent(new LockFeedbackEvent(this, room));
		roomRepository.save(room);

		return room.getSettings().isFeedbackLocked();
	}

	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'owner')")
	public boolean flipFlashcards(String sessionkey, Boolean flip) {
		final Room room = roomRepository.findByKeyword(sessionkey);
		this.publisher.publishEvent(new FlipFlashcardsEvent(this, room));

		return flip;
	}

	private void handleLogo(Room room) {
		if (room.getAuthor().getOrganizationLogo() != null) {
			if (!room.getAuthor().getOrganizationLogo().startsWith("http")) {
				throw new IllegalArgumentException("Invalid logo URL.");
			}
		}
	}
}
