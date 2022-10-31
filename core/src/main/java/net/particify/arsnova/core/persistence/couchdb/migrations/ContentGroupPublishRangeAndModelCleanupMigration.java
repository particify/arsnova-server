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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.ektorp.ViewQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

/**
 * This migration moves some state information previously stored for individual
 * Contents to ContentGroups. The new publishing state and range for
 * ContentGroups is determined by first checking if all or none of the
 * individual Contents are published (state.visible). In this case, the
 * published is set accordingly and -1 is set for firstPublishedIndex and
 * lastPublishedIndex. Otherwise, published is set to true and the range is
 * determined by taking the first published Content and the last published
 * Content in a row. Later Contents, which are not published consecutively, will
 * be excluded from the range.
 *
 * <p>
 * Furthermore, this migration adds additional properties which have been
 * introduced to the models and removes obsolete ones.
 * </p>
 *
 * @author Daniel Gerhardt
 */
@Service
public class ContentGroupPublishRangeAndModelCleanupMigration extends AbstractMigration {
  private static final String ID = "20210216102500";
  private static final String CONTENT_GROUP_INDEX = "contentgroup-index";
  private static final String CONTENT_INDEX = "content-index";
  private static final String CONTENT_DESIGN_DOC = "_design/Content";
  private static final String CONTENT_BY_ID_VIEW = "by_id";
  private static final Map<String, Boolean> existsSelector = Map.of("$exists", true);
  private static final Map<String, Boolean> notExistsSelector = Map.of("$exists", false);
  private static final Logger logger = LoggerFactory.getLogger(ContentGroupPublishRangeAndModelCleanupMigration.class);

  public ContentGroupPublishRangeAndModelCleanupMigration(
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
            "published", notExistsSelector
        ),
        contentGroup -> {
          final List<MigrationEntity> entitiesForUpdate = new ArrayList<>();
          final List<ContentMigrationEntity> contentMigrationEntities = connector.queryView(new ViewQuery()
                  .designDocId(CONTENT_DESIGN_DOC)
                  .viewName(CONTENT_BY_ID_VIEW)
                  .keys(contentGroup.getContentIds())
                  .reduce(false)
                  .includeDocs(true),
              ContentMigrationEntity.class);
          logger.debug("contentMigrationEntities: {}", contentMigrationEntities);
          final List<String> contentIds = contentMigrationEntities.stream()
              .filter(c -> c.getState().isVisible())
              .map(c -> c.getId())
              .collect(Collectors.toList());

          /* Migrate publishing state to use ranges. */
          logger.debug("Creating content group state data for {}, visible contents {}",
              contentGroup, contentIds);
          if (contentIds.size() == 0) {
            logger.debug("No contents published for {}.", contentGroup.getId());
            contentGroup.setPublished(false);
            contentGroup.setFirstPublishedIndex(-1);
            contentGroup.setLastPublishedIndex(-1);
          } else if (contentGroup.getContentIds().size() == contentIds.size()) {
            logger.debug("All contents published for {}.", contentGroup.getId());
            contentGroup.setPublished(true);
            contentGroup.setFirstPublishedIndex(0);
            contentGroup.setLastPublishedIndex(-1);
          } else {
            logger.debug("Some contents published for {}.", contentGroup.getId());
            contentGroup.setPublished(true);
            final List<Boolean> publishedFlags = contentGroup.getContentIds().stream()
                .map(id -> contentIds.contains(id)).collect(Collectors.toList());
            updateIndexRange(contentGroup, publishedFlags);
          }
          entitiesForUpdate.add(contentGroup);

          /* Update Contents to adjust their models. */
          entitiesForUpdate.addAll(contentMigrationEntities);

          return entitiesForUpdate;
        }
    );
    addEntityMigrationStepHandler(
        ContentMigrationEntity.class,
        CONTENT_INDEX,
        Map.of(
            "type", "Content",
            "state.visible", existsSelector
        ),
        /* Update Contents to adjust their models. */
        content -> List.of(content)
    );
  }

  private void updateIndexRange(final ContentGroupMigrationEntity contentGroup, final List<Boolean> publishedFlags) {
    final int first = publishedFlags.indexOf(true);
    final int lastOffset = first < publishedFlags.size() - 1
        ? publishedFlags.subList(first + 1, publishedFlags.size()).indexOf(false)
        : -1;
    final int last = lastOffset > -1 ? first + lastOffset : -1;
    logger.debug("Published flags: {}, resulting range: {}-{}", publishedFlags, first, last);
    contentGroup.setFirstPublishedIndex(first);
    contentGroup.setLastPublishedIndex(last);
  }

  /**
   * This class is used to access legacy properties for Contents which are no
   * longer part of the domain model.
   */
  private static class ContentMigrationEntity extends MigrationEntity {
    private State state;

    @JsonView(View.Persistence.class)
    public State getState() {
      return state;
    }

    @JsonView(View.Persistence.class)
    public void setState(final State state) {
      this.state = state;
    }

    @Override
    public String toString() {
      return new ToStringCreator(this)
          .append("state", state)
          .append("[properties]", getProperties())
          .toString();
    }

    private static class State extends InnerMigrationEntity {
      private boolean visible;
      private boolean responsesVisible;
      private boolean responsesEnabled;
      private boolean additionalTextVisible;
      private boolean answersPublished = true;

      /* Legacy property: Do not serialize. */
      @JsonIgnore
      public boolean isVisible() {
        return visible;
      }

      @JsonView(View.Persistence.class)
      public void setVisible(final boolean visible) {
        this.visible = visible;
      }

      /* Legacy property: Do not serialize. */
      @JsonIgnore
      public boolean isResponsesVisible() {
        return responsesVisible;
      }

      @JsonView(View.Persistence.class)
      public void setResponsesVisible(final boolean responsesVisible) {
        this.responsesVisible = responsesVisible;
      }

      /* Legacy property: Do not serialize. */
      @JsonIgnore
      public boolean isResponsesEnabled() {
        return responsesEnabled;
      }

      @JsonView(View.Persistence.class)
      public void setResponsesEnabled(final boolean responsesEnabled) {
        this.responsesEnabled = responsesEnabled;
      }

      /* Legacy property: Do not serialize. */
      @JsonIgnore
      public boolean isAdditionalTextVisible() {
        return additionalTextVisible;
      }

      @JsonView(View.Persistence.class)
      public void setAdditionalTextVisible(final boolean additionalTextVisible) {
        this.additionalTextVisible = additionalTextVisible;
      }

      @JsonView(View.Persistence.class)
      public boolean isAnswersPublished() {
        return answersPublished;
      }

      @JsonView(View.Persistence.class)
      public void setAnswersPublished(final boolean answersPublished) {
        this.answersPublished = answersPublished;
      }

      @Override
      public String toString() {
        return new ToStringCreator(this)
            .append("visible", visible)
            .append("responsesVisible", responsesVisible)
            .append("responsesEnabled", responsesEnabled)
            .append("additionalTextVisible", additionalTextVisible)
            .append("answersPublished", answersPublished)
            .append("[properties]", getProperties())
            .toString();
      }
    }
  }

  private static class ContentGroupMigrationEntity extends MigrationEntity {
    private List<String> contentIds;
    private boolean published = true;
    private int firstPublishedIndex = -1;
    private int lastPublishedIndex = -1;
    private boolean statisticsPublished = true;
    private boolean correctOptionsPublished = true;
    private boolean autoSort;

    @JsonView(View.Persistence.class)
    public List<String> getContentIds() {
      return contentIds;
    }

    @JsonView(View.Persistence.class)
    public void setContentIds(final List<String> contentIds) {
      this.contentIds = contentIds;
    }

    @JsonView(View.Persistence.class)
    public boolean isPublished() {
      return published;
    }

    @JsonView(View.Persistence.class)
    public void setPublished(final boolean published) {
      this.published = published;
    }

    @JsonView(View.Persistence.class)
    public int getFirstPublishedIndex() {
      return firstPublishedIndex;
    }

    @JsonView(View.Persistence.class)
    public void setFirstPublishedIndex(final int firstPublishedIndex) {
      this.firstPublishedIndex = firstPublishedIndex;
    }

    @JsonView(View.Persistence.class)
    public int getLastPublishedIndex() {
      return lastPublishedIndex;
    }

    @JsonView(View.Persistence.class)
    public void setLastPublishedIndex(final int lastPublishedIndex) {
      this.lastPublishedIndex = lastPublishedIndex;
    }

    @JsonView(View.Persistence.class)
    public boolean isStatisticsPublished() {
      return statisticsPublished;
    }

    @JsonView(View.Persistence.class)
    public void setStatisticsPublished(final boolean statisticsPublished) {
      this.statisticsPublished = statisticsPublished;
    }

    @JsonView(View.Persistence.class)
    public boolean isCorrectOptionsPublished() {
      return correctOptionsPublished;
    }

    @JsonView(View.Persistence.class)
    public void setCorrectOptionsPublished(final boolean correctOptionsPublished) {
      this.correctOptionsPublished = correctOptionsPublished;
    }

    /* Legacy property: Do not serialize. */
    @JsonIgnore
    public boolean isAutoSort() {
      return autoSort;
    }

    @JsonView(View.Persistence.class)
    public void setAutoSort(final boolean autoSort) {
      this.autoSort = autoSort;
    }

    @Override
    public String toString() {
      return new ToStringCreator(this)
          .append("contentIds", contentIds)
          .append("published", published)
          .append("firstPublishedIndex", firstPublishedIndex)
          .append("lastPublishedIndex", lastPublishedIndex)
          .append("statisticsPublished", statisticsPublished)
          .append("correctOptionsPublished", correctOptionsPublished)
          .append("autoSort", autoSort)
          .append("[properties]", getProperties())
          .toString();
    }
  }
}
