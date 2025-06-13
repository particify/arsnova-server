package net.particify.arsnova.core.persistence.couchdb.migrations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.style.ToStringCreator;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

/**
 * In preparation of moving the room out of this service, this migration moves
 * the settings out of the room into their own entity.
 */
@Service
public class RoomSettingsMigration extends AbstractMigration {
  private static final String ID = "20250519223300";
  private static final String ROOM_INDEX = "room-index";

  public RoomSettingsMigration(
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
          "$or", List.of(
            Map.of(
              "settings", Map.of(
                "feedbackLocked", Map.of("$exists", true)
              )
            ),
            Map.of("focusModeEnabled", Map.of("$exists", true)),
            Map.of("extensions", Map.of("$exists", true))
          )
        ),
        room -> {
          final RoomSettingsMigrationEntity roomSettings = new RoomSettingsMigrationEntity();
          roomSettings.setCreationTimestamp(new Date());
          roomSettings.roomId = room.getId();
          final Map<String, Object> commentsSettings =
              room.extensions.getOrDefault("comments", Collections.emptyMap());
          final Object thresholdEnabledObject = commentsSettings.get("enableThreshold");
          if (thresholdEnabledObject instanceof Boolean thresholdEnabled) {
            roomSettings.commentThresholdEnabled = thresholdEnabled;
          }
          final Object thresholdObject = commentsSettings.get("commentThreshold");
          if (thresholdObject instanceof Integer threshold) {
            roomSettings.commentThreshold = threshold;
          }
          final Object tags = commentsSettings.get("tags");
          if (tags instanceof List tagList) {
            roomSettings.commentTags = Set.copyOf(tagList);
          }
          roomSettings.surveyEnabled = !room.settings.feedbackLocked;
          final Map<String, Object> feedbackSettings =
              room.extensions.getOrDefault("feedback", Collections.emptyMap());
          roomSettings.surveyType = feedbackSettings.getOrDefault("type", "FEEDBACK").toString();
          roomSettings.focusModeEnabled = room.focusModeEnabled;
          final List<MigrationEntity> list = new ArrayList<>();
          list.add(room);
          if (!roomSettings.commentTags.isEmpty()
              || roomSettings.commentThresholdEnabled
              || roomSettings.surveyEnabled
              || !roomSettings.surveyType.equals("FEEDBACK")
              || roomSettings.focusModeEnabled) {
            list.add(roomSettings);
          }
          return list;
        });
  }

  @JsonView(View.Persistence.class)
  private static class RoomSettingsMigrationEntity extends MigrationEntity {
    private String roomId;
    private boolean commentThresholdEnabled;
    private int commentThreshold;
    private Set<String> commentTags = new HashSet<>();
    private boolean surveyEnabled;
    private String surveyType;
    private boolean focusModeEnabled;

    public String getType() {
      return "RoomSettings";
    }

    public boolean isCommentThresholdEnabled() {
      return commentThresholdEnabled;
    }

    public void setCommentThresholdEnabled(final boolean commentThresholdEnabled) {
      this.commentThresholdEnabled = commentThresholdEnabled;
    }

    public int getCommentThreshold() {
      return commentThreshold;
    }

    public void setCommentThreshold(final int commentThreshold) {
      this.commentThreshold = commentThreshold;
    }

    public Set<String> getCommentTags() {
      return commentTags;
    }

    public void setCommentTags(final Set<String> commentTags) {
      this.commentTags = commentTags;
    }

    public String getRoomId() {
      return roomId;
    }

    public void setRoomId(final String roomId) {
      this.roomId = roomId;
    }

    public boolean isSurveyEnabled() {
      return surveyEnabled;
    }

    public void setSurveyEnabled(final boolean surveyEnabled) {
      this.surveyEnabled = surveyEnabled;
    }

    public String getSurveyType() {
      return surveyType;
    }

    public void setSurveyType(final String surveyType) {
      this.surveyType = surveyType;
    }

    public boolean isFocusModeEnabled() {
      return focusModeEnabled;
    }

    public void setFocusModeEnabled(final boolean focusModeEnabled) {
      this.focusModeEnabled = focusModeEnabled;
    }

    @Override
    public String toString() {
      return new ToStringCreator(this)
        .append("roomId", roomId)
        .append("surveyEnabled", surveyEnabled)
        .append("surveyType", surveyType)
        .append("focusModeEnabled", focusModeEnabled)
        .append("commentTags", commentTags)
        .append("[properties]", getProperties())
        .toString();
    }
  }

  private static class RoomMigrationEntity extends MigrationEntity {
    private SettingsMigrationEntity settings = new SettingsMigrationEntity();
    private boolean focusModeEnabled;
    private Map<String, Map<String, Object>> extensions = new HashMap<>();

    /* Legacy properties: Do not serialize. */
    @JsonIgnore
    public Map<String, Map<String, Object>> getExtensions() {
      return extensions;
    }

    @JsonView(View.Persistence.class)
    public void setExtensions(final Map<String, Map<String, Object>> extensions) {
      this.extensions = extensions;
    }

    /* Legacy properties: Do not serialize. */
    @JsonIgnore
    public SettingsMigrationEntity getSettings() {
      return settings;
    }

    @JsonView(View.Persistence.class)
    public void setSettings(final SettingsMigrationEntity settings) {
      this.settings = settings;
    }

    /* Legacy properties: Do not serialize. */
    @JsonIgnore
    public boolean isFocusModeEnabled() {
      return focusModeEnabled;
    }

    @JsonView(View.Persistence.class)
    public void setFocusModeEnabled(final boolean focusModeEnabled) {
      this.focusModeEnabled = focusModeEnabled;
    }

    @Override
    public String toString() {
      return new ToStringCreator(this)
        .append("settings", settings)
        .append("focusModeEnabled", focusModeEnabled)
        .append("extensions", extensions)
        .append("[properties]", getProperties())
        .toString();
    }

    @JsonView(View.Persistence.class)
    private static class SettingsMigrationEntity extends InnerMigrationEntity {
      private boolean feedbackLocked = true;

      public boolean isFeedbackLocked() {
        return feedbackLocked;
      }

      public void setFeedbackLocked(final boolean feedbackLocked) {
        this.feedbackLocked = feedbackLocked;
      }

      @Override
      public String toString() {
        return new ToStringCreator(this)
          .append("feedbackLocked", feedbackLocked)
          .append("[properties]", getProperties())
          .toString();
      }
    }
  }
}
