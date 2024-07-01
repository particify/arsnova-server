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

package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Objects;
import org.springframework.core.style.ToStringCreator;

import net.particify.arsnova.core.model.serialization.FormatAnswerTypeIdResolver;
import net.particify.arsnova.core.model.serialization.View;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.CUSTOM,
    property = "format",
    visible = true,
    defaultImpl = Answer.class
)
@JsonTypeIdResolver(FormatAnswerTypeIdResolver.class)
public class Answer extends Entity implements RoomIdAware {
  public Answer() {

  }

  public Answer(final Content content, final String creatorId) {
    this.contentId = content.getId();
    this.roomId = content.getRoomId();
    this.format = content.getFormat();
    this.round = content.getState().getRound();
    this.creatorId = creatorId;
  }

  @NotEmpty
  private String contentId;

  @NotEmpty
  private String roomId;

  @NotEmpty
  private String creatorId;

  @NotNull
  private Content.Format format;

  @PositiveOrZero
  private int round = 1;

  private AnswerResult.AnswerResultState result;

  @PositiveOrZero
  private int points;

  @PositiveOrZero
  private int durationMs;

  @JsonView({View.Persistence.class, View.Public.class})
  public String getContentId() {
    return contentId;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setContentId(final String contentId) {
    this.contentId = contentId;
  }

  @JsonView(View.Persistence.class)
  public String getRoomId() {
    return roomId;
  }

  @JsonView(View.Persistence.class)
  public void setRoomId(final String roomId) {
    this.roomId = roomId;
  }

  @JsonView(View.Persistence.class)
  public String getCreatorId() {
    return creatorId;
  }

  public void setCreatorId(final String creatorId) {
    this.creatorId = creatorId;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public Content.Format getFormat() {
    return format;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setFormat(final Content.Format format) {
    this.format = format;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public int getRound() {
    return round;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setRound(final int round) {
    this.round = round;
  }

  @JsonView(View.Persistence.class)
  public AnswerResult.AnswerResultState getResult() {
    return result;
  }

  @JsonView(View.Persistence.class)
  public void setResult(final AnswerResult.AnswerResultState result) {
    this.result = result;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public int getPoints() {
    return points;
  }

  @JsonView(View.Persistence.class)
  public void setPoints(final int points) {
    this.points = points;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public int getDurationMs() {
    return durationMs;
  }

  @JsonView(View.Persistence.class)
  public void setDurationMs(final int durationMs) {
    this.durationMs = durationMs;
  }

  public boolean isAbstention() {
    return false;
  }

  @JsonView(View.Persistence.class)
  @Override
  public Class<? extends Entity> getType() {
    return Answer.class;
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * The following fields of <tt>Answer</tt> are excluded from equality checks:
   * none.
   * </p>
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!super.equals(o)) {
      return false;
    }
    final Answer answer = (Answer) o;

    return round == answer.round
        && Objects.equals(contentId, answer.contentId)
        && Objects.equals(roomId, answer.roomId)
        && Objects.equals(creatorId, answer.creatorId)
        && Objects.equals(points, answer.points)
        && Objects.equals(durationMs, answer.durationMs);
  }

  @Override
  public int hashCode() {
    return hashCode(super.hashCode(), round, contentId, roomId, creatorId, points);
  }

  @Override
  protected ToStringCreator buildToString() {
    return super.buildToString()
        .append("contentId", contentId)
        .append("roomId", roomId)
        .append("creatorId", creatorId)
        .append("format", format)
        .append("round", round)
        .append("points", points)
        .append("durationMs", durationMs);
  }
}
