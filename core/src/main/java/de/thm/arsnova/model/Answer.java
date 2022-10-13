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

package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import java.util.Objects;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.FormatAnswerTypeIdResolver;
import de.thm.arsnova.model.serialization.View;

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
        && Objects.equals(creatorId, answer.creatorId);
  }

  @Override
  protected ToStringCreator buildToString() {
    return super.buildToString()
        .append("contentId", contentId)
        .append("roomId", roomId)
        .append("creatorId", creatorId)
        .append("format", format)
        .append("round", round);
  }
}
