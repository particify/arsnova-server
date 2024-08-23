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
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

/**
 * This migration adds the groupType attributes to existing content groups.
 *
 * @author Daniel Gerhardt
 */
@Service
public class ContentGroupTypeMigration extends AbstractMigration {
  private static final String ID = "20240823101000";
  private static final String CONTENT_GROUP_INDEX = "content-group-type-index";

  public ContentGroupTypeMigration(
      final MangoCouchDbConnector connector) {
    super(ID, connector);
  }

  @PostConstruct
  public void initMigration() {
    addEntityMigrationStepHandler(
        ContentGroupMigrationEntity.class,
        CONTENT_GROUP_INDEX,
        Map.of(
            "type", "ContentGroup",
            "groupType", Map.of("$exists", false)
        ),
        contentGroup -> {
          contentGroup.setGroupType(GroupType.MIXED);
          return List.of(contentGroup);
        }
    );
  }

  private static class ContentGroupMigrationEntity extends MigrationEntity {
    private GroupType groupType;

    @JsonView(View.Persistence.class)
    public GroupType getGroupType() {
      return groupType;
    }

    @JsonView(View.Persistence.class)
    public void setGroupType(final GroupType groupType) {
      this.groupType = groupType;
    }
  }

  enum GroupType {
    MIXED
  }
}
