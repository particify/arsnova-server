package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;

import net.particify.arsnova.core.model.serialization.View;

public class ViolationReport extends Entity {
  @NotEmpty
  @Pattern(regexp = "ContentGroupTemplate|Room")
  private String targetType;

  @NotEmpty
  private String targetId;

  @NotNull
  private Reason reason;

  @NotBlank
  @Size(max = 500)
  private String description;

  @NotEmpty
  private String creatorId;

  private Decision decision;

  @JsonView({View.Persistence.class, View.Public.class})
  public String getTargetType() {
    return targetType;
  }

  @JsonView(View.Persistence.class)
  public void setTargetType(final String targetType) {
    this.targetType = targetType;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getTargetId() {
    return targetId;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setTargetId(final String targetId) {
    this.targetId = targetId;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public Reason getReason() {
    return reason;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setReason(final Reason reason) {
    this.reason = reason;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getDescription() {
    return description;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setDescription(final String description) {
    this.description = description;
  }

  @JsonView(View.Persistence.class)
  public String getCreatorId() {
    return creatorId;
  }

  @JsonView(View.Persistence.class)
  public void setCreatorId(final String creatorId) {
    this.creatorId = creatorId;
  }

  @JsonView({View.Persistence.class, View.Admin.class})
  public Decision getDecision() {
    return decision;
  }

  /* Only admins should be able to set the decision. This currently needs to be handled manually because the admin view
   * cannot be applied to the response body. */
  @JsonView({View.Persistence.class, View.Public.class})
  public void setDecision(final Decision decision) {
    this.decision = decision;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ViolationReport that = (ViolationReport) o;
    return Objects.equals(targetType, that.targetType)
      && Objects.equals(targetId, that.targetId)
      && reason == that.reason && Objects.equals(description, that.description)
      && Objects.equals(creatorId, that.creatorId)
      && decision == that.decision;
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetType, targetId, reason, description, creatorId, decision);
  }

  enum Decision {
    INVALID,
    REMOVAL
  }

  enum Reason {
    ADVERTISING,
    COPYRIGHT,
    DISINFORMATION,
    HATE_SPEECH,
    OTHER
  }
}
