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

package net.particify.arsnova.core.service;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import net.particify.arsnova.connector.client.ConnectorClient;
import net.particify.arsnova.connector.model.Membership;
import net.particify.arsnova.core.event.BeforeDeletionEvent;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.RoomMembership;
import net.particify.arsnova.core.model.UserProfile;
import net.particify.arsnova.core.persistence.RoomRepository;
import net.particify.arsnova.core.security.RoomRole;
import net.particify.arsnova.core.web.exceptions.NotFoundException;

/**
 * Performs all room related operations.
 */
@Service
@Primary
public class RoomServiceImpl extends DefaultEntityServiceImpl<Room> implements RoomService {
  private static final long DELETE_SCHEDULED_ROOMS_INTERVAL_MS = 30 * 60 * 1000L;

  private static final Logger logger = LoggerFactory.getLogger(RoomServiceImpl.class);

  private RoomRepository roomRepository;

  private UserService userService;

  private AccessTokenService accessTokenService;

  private ConnectorClient connectorClient;

  @Value("${system.inactivity-thresholds.delete-inactive-guest-rooms:0}")
  private int guestRoomInactivityThresholdDays;

  public RoomServiceImpl(
      final RoomRepository repository,
      final UserService userService,
      final AccessTokenService accessTokenService,
      @Qualifier("defaultJsonMessageConverter")
      final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
      final Validator validator) {
    super(Room.class, repository, jackson2HttpMessageConverter.getObjectMapper(), validator);
    this.roomRepository = repository;
    this.userService = userService;
    this.accessTokenService = accessTokenService;
  }

  public static class RoomNameComparator implements Comparator<Room>, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(final Room room1, final Room room2) {
      return room1.getName().compareToIgnoreCase(room2.getName());
    }
  }

  @Autowired(required = false)
  public void setConnectorClient(final ConnectorClient connectorClient) {
    this.connectorClient = connectorClient;
  }

  @EventListener
  public void handleUserDeletion(final BeforeDeletionEvent<UserProfile> event) {
    final Iterable<Room> rooms = roomRepository.findByOwnerId(event.getEntity().getId(), -1, -1);
    delete(rooms);
  }

  @Scheduled(fixedRate = DELETE_SCHEDULED_ROOMS_INTERVAL_MS)
  private void deleteScheduledRooms() {
    logger.trace("Checking for rooms scheduled for deletion.");
    final List<Room> rooms = roomRepository.findStubsByScheduledDeletionAfter(new Date());
    if (!rooms.isEmpty()) {
      delete(rooms);
      logger.info("Deleted {} scheduled rooms.", rooms.size());
    }
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
  public List<String> getUserRoomIds(final String userId) {
    return roomRepository.findIdsByOwnerId(userId);
  }

  @Override
  public void prepareCreate(final Room room) {
    final Room.Settings sf = new Room.Settings();
    room.setSettings(sf);

    room.setShortId(generateShortId());
    if (room.getOwnerId() == null) {
      room.setOwnerId(userService.getCurrentUser().getId());
    }
    room.setClosed(false);
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
    if (room.isClosed()) {
      return Optional.empty();
    }
    if (room.getLmsCourseId() != null) {
      return determineMembershipFromLms(room);
    }
    return room.isPasswordProtected() && !room.getPassword().equals(password)
        ? Optional.empty()
        : Optional.of(new RoomMembership(room, RoomRole.PARTICIPANT));
  }

  @Override
  public Optional<RoomMembership> requestMembershipByToken(final String roomId, final String token) {
    final Room room = get(roomId);
    final Optional<RoomRole> accessToken = accessTokenService.redeemToken(roomId, token);
    return accessToken.map(role -> new RoomMembership(room, role));
  }

  private Optional<RoomMembership> determineMembershipFromLms(final Room room) {
    logger.trace("Determining user role via LMS membership.");
    if (connectorClient == null) {
      logger.warn("Room {} is connected to LMS course {} but LMS connector client is disabled.",
          room.getId(),
          room.getLmsCourseId());
      return Optional.empty();
    }
    final String loginId = userService.getCurrentUser().getUsername();
    final Membership lmsMembership = connectorClient.getMembership(loginId, room.getLmsCourseId());
    return lmsMembership.isMember()
        ? Optional.of(new RoomMembership(room, RoomRole.PARTICIPANT))
        : Optional.empty();
  }
}
