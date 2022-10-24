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

package de.thm.arsnova.persistence.couchdb.migrations;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.ektorp.ViewQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import de.thm.arsnova.config.properties.CouchDbMigrationProperties;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.persistence.couchdb.support.MangoCouchDbConnector;

/**
 * This migration adjusts ContentGroups which have been created through
 * migration before. It restores the order of contents and replaces the group's
 * name with a mapped group name. The order is only updated for ContentGroups
 * which have not yet been modified manually by the user.
 *
 * @author Daniel Gerhardt
 */
@Service
@ConditionalOnProperty(
    name = "enabled",
    prefix = CouchDbMigrationProperties.PREFIX)
public class MigratedContentGroupMigration extends AbstractMigration {
  private static final String ID = "20201208172400";
  private static final String CONTENT_GROUP_INDEX = "contentgroup-index";
  private static final String CONTENT_DESIGN_DOC = "_design/Content";
  private static final String CONTENT_BY_ID_VIEW = "by_id";
  private static final Map<String, Boolean> notExistsSelector = Map.of("$exists", false);

  private final Map<String, String> contentGroupNames;

  public MigratedContentGroupMigration(
      final MangoCouchDbConnector connector,
      final CouchDbMigrationProperties couchDbMigrationProperties) {
    super(ID, connector);
    this.contentGroupNames = couchDbMigrationProperties.getContentGroupNames();
  }

  @PostConstruct
  public void initMigration() {
    addEntityMigrationStepHandler(
        ContentGroupMigrationEntity.class,
        CONTENT_GROUP_INDEX,
        Map.of(
            "type", "ContentGroup",
            /* The creationTimestamp was not set for migrated ContentGroups in the
             * past, so its non-existence is a feature of migrated ContentGroups. */
            "creationTimestamp", notExistsSelector
        ),
        contentGroup -> {
          contentGroup.setName(contentGroupNames.getOrDefault(contentGroup.getName(), contentGroup.getName()));
          contentGroup.setAutoSort(false);
          /* Apply alphanumerical sorting if the ContentGroup has not yet been manually modified. */
          if (contentGroup.getUpdateTimestamp() == null) {
            final List<ContentMigrationEntity> contents = connector.queryView(new ViewQuery()
                    .designDocId(CONTENT_DESIGN_DOC)
                    .viewName(CONTENT_BY_ID_VIEW)
                    .keys(contentGroup.getContentIds())
                    .reduce(false)
                    .includeDocs(true),
                ContentMigrationEntity.class);
            contentGroup.setContentIds(
                contents.stream()
                    .sorted(Comparator.comparing(ContentMigrationEntity::getBody))
                    .map(ContentMigrationEntity::getId)
                    .collect(Collectors.toList()));
          }
          final RoomMigrationEntity room = connector.get(RoomMigrationEntity.class, contentGroup.getRoomId());
          contentGroup.setCreationTimestamp(room.getCreationTimestamp());

          return List.of(contentGroup);
        }
    );
  }

  private static class ContentGroupMigrationEntity extends MigrationEntity {
    private String roomId;
    private String name;
    private List<String> contentIds;
    private boolean autoSort;

    @JsonView(View.Persistence.class)
    public String getRoomId() {
      return roomId;
    }

    @JsonView(View.Persistence.class)
    public void setRoomId(final String roomId) {
      this.roomId = roomId;
    }

    @JsonView(View.Persistence.class)
    public String getName() {
      return name;
    }

    @JsonView(View.Persistence.class)
    public void setName(final String name) {
      this.name = name;
    }

    @JsonView(View.Persistence.class)
    public List<String> getContentIds() {
      return contentIds;
    }

    @JsonView(View.Persistence.class)
    public void setContentIds(final List<String> contentIds) {
      this.contentIds = contentIds;
    }

    @JsonView(View.Persistence.class)
    public boolean isAutoSort() {
      return autoSort;
    }

    @JsonView(View.Persistence.class)
    public void setAutoSort(final boolean autoSort) {
      this.autoSort = autoSort;
    }
  }

  private static class ContentMigrationEntity extends MigrationEntity {
    private String body;

    @JsonView(View.Persistence.class)
    public String getBody() {
      return body;
    }

    @JsonView(View.Persistence.class)
    public void setBody(final String body) {
      this.body = body;
    }
  }

  private static class RoomMigrationEntity extends MigrationEntity {
  }
}
