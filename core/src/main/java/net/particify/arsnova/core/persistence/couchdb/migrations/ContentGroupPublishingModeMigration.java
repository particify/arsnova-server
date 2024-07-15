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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

/**
 * This migration introduces the publishing mode with a single index which
 * replace the published state and publishing range.
 *
 * @author Daniel Gerhardt
 */
@Service
public class ContentGroupPublishingModeMigration extends AbstractMigration {
  private static final String ID = "20240403144000";
  private static final String ROOM_INDEX = "content-group-published-index";

  public ContentGroupPublishingModeMigration(
      final MangoCouchDbConnector connector) {
    super(ID, connector);
  }

  @PostConstruct
  public void initMigration() {
    addEntityMigrationStepHandler(
        ContentGroupMigrationEntity.class,
        ROOM_INDEX,
        Map.of(
            "type", "ContentGroup",
            "published", Map.of("$exists", true)
        ),
        contentGroup -> {
          if (contentGroup.getFirstPublishedIndex() == 0 && (contentGroup.getLastPublishedIndex() == -1
              || contentGroup.getLastPublishedIndex() == contentGroup.getContentIds().size() - 1)) {
            contentGroup.setPublishingMode(PublishingMode.ALL);
          } else if (contentGroup.getFirstPublishedIndex() == 0) {
            contentGroup.setPublishingMode(PublishingMode.UP_TO);
            contentGroup.setPublishingIndex(contentGroup.getLastPublishedIndex());
          } else {
            contentGroup.setPublished(false);
            contentGroup.setPublishingMode(PublishingMode.ALL);
          }
          return List.of(contentGroup);
        }
    );
  }

  private static class ContentGroupMigrationEntity extends MigrationEntity {
    private List<String> contentIds;
    private PublishingMode publishingMode;
    private int publishingIndex;
    private boolean published;
    private int firstPublishedIndex;
    private int lastPublishedIndex;

    @JsonView(View.Persistence.class)
    public List<String> getContentIds() {
      return contentIds;
    }

    @JsonView(View.Persistence.class)
    public void setContentIds(final List<String> contentIds) {
      this.contentIds = contentIds;
    }

    @JsonView(View.Persistence.class)
    public PublishingMode getPublishingMode() {
      return publishingMode;
    }

    @JsonView(View.Persistence.class)
    public void setPublishingMode(final PublishingMode publishingMode) {
      this.publishingMode = publishingMode;
    }

    @JsonView(View.Persistence.class)
    public int getPublishingIndex() {
      return publishingIndex;
    }

    @JsonView(View.Persistence.class)
    public void setPublishingIndex(final int publishingIndex) {
      this.publishingIndex = publishingIndex;
    }

    @JsonView(View.Persistence.class)
    public boolean isPublished() {
      return published;
    }

    @JsonView(View.Persistence.class)
    public void setPublished(final boolean published) {
      this.published = published;
    }

    /* Legacy property: Do not serialize. */
    @JsonIgnore
    public int getFirstPublishedIndex() {
      return firstPublishedIndex;
    }

    @JsonView(View.Persistence.class)
    public void setFirstPublishedIndex(final int firstPublishedIndex) {
      this.firstPublishedIndex = firstPublishedIndex;
    }

    /* Legacy property: Do not serialize. */
    @JsonIgnore
    public int getLastPublishedIndex() {
      return lastPublishedIndex;
    }

    @JsonView(View.Persistence.class)
    public void setLastPublishedIndex(final int lastPublishedIndex) {
      this.lastPublishedIndex = lastPublishedIndex;
    }
  }

  enum PublishingMode {
    ALL,
    UP_TO
  }
}
