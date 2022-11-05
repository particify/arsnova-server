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

package net.particify.arsnova.core.persistence.couchdb.migrations;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.event.RoomHistoryMigrationEvent;
import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

/**
 * This migration only creates events for {@link net.particify.arsnova.core.model.UserProfile.RoomHistoryEntry}s
 * of {@link net.particify.arsnova.core.model.UserProfile}s which are obsolete and will be
 * removed by a later migration. It does not perform any database changes.
 *
 * @author Daniel Gerhardt
 */
@Service
public class UserProfileRoomHistoryMigration extends AbstractMigration {
  private static final String ID = "20210511192000";
  private static final String USER_PROFILE_INDEX = "userprofile-index";
  private static final Map<String, Map<String, Integer>> arrayNotEmptySelector =
      Map.of("$not", Map.of("$size", 0));

  private final ApplicationEventPublisher applicationEventPublisher;

  public UserProfileRoomHistoryMigration(
      final MangoCouchDbConnector connector, final ApplicationEventPublisher applicationEventPublisher) {
    super(ID, connector);
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @PostConstruct
  public void initMigration() {
    addEntityMigrationStepHandler(
        UserProfileMigrationEntity.class,
        USER_PROFILE_INDEX,
        Map.of(
            "type", "UserProfile",
            "roomHistory", arrayNotEmptySelector
        ),
        userProfile -> {
          this.applicationEventPublisher.publishEvent(new RoomHistoryMigrationEvent(
              this,
              userProfile.getId(),
              userProfile.getRoomHistory().stream().map(rh -> rh.roomId).collect(Collectors.toList())));
          return Collections.emptyList();
        }
    );
  }

  @JsonView(View.Persistence.class)
  private static class UserProfileMigrationEntity extends MigrationEntity {
    private List<RoomHistoryEntryMigrationEntity> roomHistory;

    public List<RoomHistoryEntryMigrationEntity> getRoomHistory() {
      return roomHistory;
    }

    public void setRoomHistory(final List<RoomHistoryEntryMigrationEntity> roomHistory) {
      this.roomHistory = roomHistory;
    }

    @JsonView(View.Persistence.class)
    public static class RoomHistoryEntryMigrationEntity extends InnerMigrationEntity {
      private String roomId;

      public String getRoomId() {
        return roomId;
      }

      public void setRoomId(final String roomId) {
        this.roomId = roomId;
      }
    }
  }
}
