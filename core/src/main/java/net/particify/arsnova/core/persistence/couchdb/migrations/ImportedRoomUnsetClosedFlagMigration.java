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
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

/**
 * This migration sets the closed flag to false for Rooms that have been
 * imported.
 *
 * @author Daniel Gerhardt
 */
@Service
public class ImportedRoomUnsetClosedFlagMigration extends AbstractMigration {
  private static final String ID = "20210319160400";
  private static final String ROOM_INDEX = "room-imported-closed-index";

  public ImportedRoomUnsetClosedFlagMigration(
      final MangoCouchDbConnector connector) {
    super(ID, connector);
  }

  @PostConstruct
  public void initMigration() {
    addEntityMigrationStepHandler(
        RoomMigrationEntity.class,
        ROOM_INDEX,
        Map.of(
            "type", "Room",
            "closed", true,
            "importMetadata", Map.of("source", "V2_IMPORT")
        ),
        room -> {
          room.setClosed(false);
          return List.of(room);
        }
    );
  }

  private static class RoomMigrationEntity extends MigrationEntity {
    private boolean closed;

    @JsonView(View.Persistence.class)
    public boolean isClosed() {
      return closed;
    }

    @JsonView(View.Persistence.class)
    public void setClosed(final boolean closed) {
      this.closed = closed;
    }
  }
}
