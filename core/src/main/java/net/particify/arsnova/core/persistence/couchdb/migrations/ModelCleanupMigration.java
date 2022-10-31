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
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.springframework.core.style.ToStringCreator;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

/**
 * This migration removes legacy properties which are no longer used.
 *
 * @author Daniel Gerhardt
 */
@Service
public class ModelCleanupMigration extends AbstractMigration {
  private static final String ID = "20210902132900";
  private static final String USER_PROFILE_INDEX = "userprofile-index";
  private static final String ROOM_INDEX = "room-index";
  private static final String CONTENT_INDEX = "content-index";
  private static final Map<String, Boolean> existsSelector = Map.of("$exists", true);

  public ModelCleanupMigration(
      final MangoCouchDbConnector connector) {
    super(ID, connector);
  }

  @PostConstruct
  public void initMigration() {
    addEntityMigrationStepHandler(
        UserProfileMigrationEntity.class,
        USER_PROFILE_INDEX,
        Map.of(
            "type", "UserProfile",
            "roomHistory", existsSelector
        ),
        /* Update UserProfiles to adjust their models. */
        userProfile -> List.of(userProfile)
    );
    addEntityMigrationStepHandler(
        RoomMigrationEntity.class,
        ROOM_INDEX,
        Map.of(
            "type", "Room",
            "abbreviation", existsSelector
        ),
        /* Update Rooms to adjust their models. */
        room -> List.of(room)
    );
    addEntityMigrationStepHandler(
        ContentMigrationEntity.class,
        CONTENT_INDEX,
        Map.of(
            "type", "Content",
            "extensions", existsSelector
        ),
        /* Update Contents to adjust their models. */
        content -> List.of(content)
    );
  }

  /**
   * This class is used to access legacy properties for UserProfiles which are
   * no longer part of the domain model.
   */
  private static class UserProfileMigrationEntity extends MigrationEntity {
    private Set roomHistory;

    /* Legacy properties: Do not serialize. */
    @JsonIgnore
    public Set getRoomHistory() {
      return roomHistory;
    }

    @JsonView(View.Persistence.class)
    public void setRoomHistory(final Set roomHistory) {
      this.roomHistory = roomHistory;
    }

    @Override
    public String toString() {
      return new ToStringCreator(this)
          .append("roomHistory", roomHistory)
          .append("[properties]", getProperties())
          .toString();
    }
  }

  /**
   * This class is used to access legacy properties for Rooms which are no
   * longer part of the domain model.
   */
  private static class RoomMigrationEntity extends MigrationEntity {
    private String abbreviation;
    private List moderators;
    private SettingsMigrationEntity settings;
    private Map author;
    private Map poolProperties;

    /* Legacy property: Do not serialize. */
    @JsonIgnore
    public String getAbbreviation() {
      return abbreviation;
    }

    @JsonView(View.Persistence.class)
    public void setAbbreviation(final String abbreviation) {
      this.abbreviation = abbreviation;
    }

    /* Legacy property: Do not serialize. */
    @JsonIgnore
    public List getModerators() {
      return moderators;
    }

    @JsonView(View.Persistence.class)
    public void setModerators(final List moderators) {
      this.moderators = moderators;
    }

    @JsonView(View.Persistence.class)
    public SettingsMigrationEntity getSettings() {
      return settings;
    }

    @JsonView(View.Persistence.class)
    public void setSettings(final SettingsMigrationEntity settings) {
      this.settings = settings;
    }

    /* Legacy property: Do not serialize. */
    @JsonIgnore
    public Map getAuthor() {
      return author;
    }

    @JsonView(View.Persistence.class)
    public void setAuthor(final Map author) {
      this.author = author;
    }

    /* Legacy property: Do not serialize. */
    @JsonIgnore
    public Map getPoolProperties() {
      return poolProperties;
    }

    @JsonView(View.Persistence.class)
    public void setPoolProperties(final Map poolProperties) {
      this.poolProperties = poolProperties;
    }

    @Override
    public String toString() {
      return new ToStringCreator(this)
          .append("abbreviation", abbreviation)
          .append("moderators", moderators)
          .append("settings", settings)
          .append("author", author)
          .append("poolProperties", poolProperties)
          .append("[properties]", getProperties())
          .toString();
    }

    private static class SettingsMigrationEntity extends InnerMigrationEntity {
      private boolean commentsEnabled;
      private boolean flashcardsEnabled;
      private boolean multipleRoundsEnabled;
      private boolean questionsEnabled;
      private boolean quickSurveyEnabled;
      private boolean quickFeedbackEnabled;
      private boolean scoreEnabled;
      private boolean slidesEnabled;
      private boolean timerEnabled;

      /* Legacy property: Do not serialize. */
      @JsonIgnore
      public boolean isCommentsEnabled() {
        return commentsEnabled;
      }

      @JsonView(View.Persistence.class)
      public void setCommentsEnabled(final boolean commentsEnabled) {
        this.commentsEnabled = commentsEnabled;
      }

      /* Legacy property: Do not serialize. */
      @JsonIgnore
      public boolean isFlashcardsEnabled() {
        return flashcardsEnabled;
      }

      @JsonView(View.Persistence.class)
      public void setFlashcardsEnabled(final boolean flashcardsEnabled) {
        this.flashcardsEnabled = flashcardsEnabled;
      }

      /* Legacy property: Do not serialize. */
      @JsonIgnore
      public boolean isMultipleRoundsEnabled() {
        return multipleRoundsEnabled;
      }

      @JsonView(View.Persistence.class)
      public void setMultipleRoundsEnabled(final boolean multipleRoundsEnabled) {
        this.multipleRoundsEnabled = multipleRoundsEnabled;
      }

      /* Legacy property: Do not serialize. */
      @JsonIgnore
      public boolean isQuestionsEnabled() {
        return questionsEnabled;
      }

      @JsonView(View.Persistence.class)
      public void setQuestionsEnabled(final boolean questionsEnabled) {
        this.questionsEnabled = questionsEnabled;
      }

      /* Legacy property: Do not serialize. */
      @JsonIgnore
      public boolean isQuickSurveyEnabled() {
        return quickSurveyEnabled;
      }

      @JsonView(View.Persistence.class)
      public void setQuickSurveyEnabled(final boolean quickSurveyEnabled) {
        this.quickSurveyEnabled = quickSurveyEnabled;
      }

      /* Legacy property: Do not serialize. */
      @JsonIgnore
      public boolean isQuickFeedbackEnabled() {
        return quickFeedbackEnabled;
      }

      @JsonView(View.Persistence.class)
      public void setQuickFeedbackEnabled(final boolean quickFeedbackEnabled) {
        this.quickFeedbackEnabled = quickFeedbackEnabled;
      }

      /* Legacy property: Do not serialize. */
      @JsonIgnore
      public boolean isScoreEnabled() {
        return scoreEnabled;
      }

      @JsonView(View.Persistence.class)
      public void setScoreEnabled(final boolean scoreEnabled) {
        this.scoreEnabled = scoreEnabled;
      }

      /* Legacy property: Do not serialize. */
      @JsonIgnore
      public boolean isSlidesEnabled() {
        return slidesEnabled;
      }

      @JsonView(View.Persistence.class)
      public void setSlidesEnabled(final boolean slidesEnabled) {
        this.slidesEnabled = slidesEnabled;
      }

      /* Legacy property: Do not serialize. */
      @JsonIgnore
      public boolean isTimerEnabled() {
        return timerEnabled;
      }

      @JsonView(View.Persistence.class)
      public void setTimerEnabled(final boolean timerEnabled) {
        this.timerEnabled = timerEnabled;
      }

      @Override
      public String toString() {
        return new ToStringCreator(this)
            .append("commentsEnabled", commentsEnabled)
            .append("flashcardsEnabled", flashcardsEnabled)
            .append("multipleRoundsEnabled", multipleRoundsEnabled)
            .append("questionsEnabled", questionsEnabled)
            .append("scoreEnabled", scoreEnabled)
            .append("slidesEnabled", slidesEnabled)
            .append("timerEnabled", timerEnabled)
            .toString();
      }
    }
  }

  /**
   * This class is used to access legacy properties for Contents which are no
   * longer part of the domain model.
   */
  private static class ContentMigrationEntity extends MigrationEntity {
    private Map extensions;

    /* Legacy properties: Do not serialize. */
    @JsonIgnore
    public Map getExtensions() {
      return extensions;
    }

    @JsonView(View.Persistence.class)
    public void setExtensions(final Map extensions) {
      this.extensions = extensions;
    }

    @Override
    public String toString() {
      return new ToStringCreator(this)
          .append("extensions", extensions)
          .append("[properties]", getProperties())
          .toString();
    }
  }
}
