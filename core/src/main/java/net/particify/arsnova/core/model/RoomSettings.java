package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.HashSet;
import java.util.Set;

import net.particify.arsnova.core.model.serialization.View;

public class RoomSettings extends Entity implements RoomIdAware {
  private String roomId;
  private boolean commentThresholdEnabled;
  private int commentThreshold = -50;
  private Set<String> commentTags = new HashSet<>();
  private boolean surveyEnabled;
  private SurveyType surveyType = SurveyType.FEEDBACK;
  private boolean focusModeEnabled;

  public RoomSettings() {
  }

  public RoomSettings(final String roomId) {
    this.roomId = roomId;
  }

  @Override
  @JsonView(View.Persistence.class)
  public void setId(final String id) {
    super.setId(id);
  }

  @JsonView({View.Persistence.class, View.Public.class})
  @Override
  public String getRoomId() {
    return roomId;
  }

  @JsonView(View.Persistence.class)
  public void setRoomId(final String roomId) {
    this.roomId = roomId;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public boolean isCommentThresholdEnabled() {
    return commentThresholdEnabled;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setCommentThresholdEnabled(final boolean commentThresholdEnabled) {
    this.commentThresholdEnabled = commentThresholdEnabled;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public int getCommentThreshold() {
    return commentThreshold;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setCommentThreshold(final int commentThreshold) {
    this.commentThreshold = commentThreshold;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public Set<String> getCommentTags() {
    return commentTags;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setCommentTags(final Set<String> commentTags) {
    this.commentTags = commentTags;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public boolean isSurveyEnabled() {
    return surveyEnabled;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setSurveyEnabled(final boolean surveyEnabled) {
    this.surveyEnabled = surveyEnabled;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public SurveyType getSurveyType() {
    return surveyType;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setSurveyType(final SurveyType surveyType) {
    this.surveyType = surveyType;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public boolean isFocusModeEnabled() {
    return focusModeEnabled;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setFocusModeEnabled(final boolean focusModeEnabled) {
    this.focusModeEnabled = focusModeEnabled;
  }

  public enum SurveyType {
    FEEDBACK,
    SURVEY
  }
}
