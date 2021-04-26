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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ektorp.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import de.thm.arsnova.event.BeforeDeletionEvent;
import de.thm.arsnova.event.BeforeFullUpdateEvent;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.RoomMembership;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.persistence.AnswerRepository;
import de.thm.arsnova.persistence.ContentRepository;
import de.thm.arsnova.persistence.LogEntryRepository;
import de.thm.arsnova.persistence.RoomRepository;
import de.thm.arsnova.security.PasswordUtils;
import de.thm.arsnova.security.RoomRole;
import de.thm.arsnova.security.User;
import de.thm.arsnova.security.jwt.JwtService;
import de.thm.arsnova.web.exceptions.BadRequestException;
import de.thm.arsnova.web.exceptions.NotFoundException;
import net.particify.arsnova.connector.client.ConnectorClient;

/**
 * Performs all room related operations.
 */
@Service
@Primary
public class RoomServiceImpl extends DefaultEntityServiceImpl<Room> implements RoomService {
	private static final long ROOM_INACTIVITY_CHECK_INTERVAL_MS = 30 * 60 * 1000L;

	private static final Logger logger = LoggerFactory.getLogger(RoomServiceImpl.class);

	private LogEntryRepository dbLogger;

	private RoomRepository roomRepository;

	private ContentRepository contentRepository;

	private AnswerRepository answerRepository;

	private UserService userService;

	private ConnectorClient connectorClient;

	private JwtService jwtService;

	private PasswordUtils passwordUtils;

	@Value("${system.inactivity-thresholds.delete-inactive-guest-rooms:0}")
	private int guestRoomInactivityThresholdDays;

	@Value("${features.content-pool.logo-max-filesize}")
	private int uploadFileSizeByte;

	public RoomServiceImpl(
			final RoomRepository repository,
			final LogEntryRepository dbLogger,
			final UserService userService,
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
			final Validator validator,
			final JwtService jwtService,
			final PasswordUtils passwordUtils) {
		super(Room.class, repository, jackson2HttpMessageConverter.getObjectMapper(), validator);
		this.roomRepository = repository;
		this.dbLogger = dbLogger;
		this.userService = userService;
		this.jwtService = jwtService;
		this.passwordUtils = passwordUtils;
	}

	@Autowired
	public void setContentRepository(final ContentRepository contentRepository) {
		this.contentRepository = contentRepository;
	}

	@Autowired
	public void setAnswerRepository(final AnswerRepository answerRepository) {
		this.answerRepository = answerRepository;
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

	@Override
	public List<String> getUserRoomIds(final String userId) {
		return roomRepository.findIdsByOwnerId(userId);
	}

	@Override
	public List<String> getRoomIdsByModeratorId(final String userId) {
		return roomRepository.findIdsByModeratorId(userId);
	}

	@Override
	public List<Room> getUserRoomHistory(final String userId) {
		final UserProfile profile = userService.get(userId);
		final List<String> roomIds = profile.getRoomHistory().stream()
				.map(entry -> entry.getRoomId()).collect(Collectors.toList());
		final List<Room> rooms = new ArrayList<>();
		roomRepository.findAllById(roomIds).forEach(rooms::add);

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

	public boolean isShortIdAvailable(final String shortId) {
		try {
			return getIdByShortId(shortId) == null;
		} catch (final NotFoundException e) {
			return true;
		}
	}

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
	/* TODO: move caching to DefaultEntityServiceImpl */
	//@CachePut(value = "rooms", key = "#room")
	protected void prepareUpdate(final Room room) {
		final Room existingRoom = get(room.getId());
		if (room.getOwnerId() == null) {
			room.setOwnerId(existingRoom.getOwnerId());
		}
		room.setPassword(existingRoom.getPassword());
		handleLogo(room);

		/* TODO: only publish event when feedback has changed */
		/* FIXME: event */
		// this.publisher.publishEvent(new FeatureChangeEvent(this, room));
	}

	@Override
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
	public Room transferOwnershipThroughToken(final Room room, final String targetUserToken) {
		final User user = jwtService.verifyToken(targetUserToken);
		room.setOwnerId(user.getId());
		return update(room);
	}

	@Override
	public String getPassword(final Room room) {
		return room.getPassword();
	}

	@Override
	public void setPassword(final Room room, final String password) {
		room.setPassword(password != null && !password.isBlank() ? password : null);
		update(room);
	}

	@Override
	public Optional<RoomMembership> requestMembership(final String roomId, final String password) {
		final Room room = get(roomId);
		return room.isClosed()
				|| room.isPasswordProtected() && !passwordUtils.matches(password, room.getPassword())
				? Optional.empty()
				: Optional.of(new RoomMembership(room, RoomRole.PARTICIPANT));
	}

	private void handleLogo(final Room room) {
		if (room.getAuthor() != null && room.getAuthor().getOrganizationLogo() != null) {
			if (!room.getAuthor().getOrganizationLogo().startsWith("http")) {
				throw new IllegalArgumentException("Invalid logo URL.");
			}
		}
	}
}
