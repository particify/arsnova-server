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

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Date;
import java.util.Objects;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import org.springframework.core.style.ToStringCreator;

import net.particify.arsnova.core.model.serialization.View;

public class Announcement extends Entity implements RoomIdAware {
  @NotEmpty
  private String roomId;

  @NotEmpty
  private String creatorId;

  @NotBlank
  private String title;

  @NotBlank
  private String body;

  private String renderedBody;

  {
    final TextRenderingOptions options = new TextRenderingOptions();
    options.setMarkdownFeatureset(TextRenderingOptions.MarkdownFeatureset.EXTENDED);
    this.addRenderingMapping(
        this::getBody,
        this::setRenderedBody,
        options);
  }

  @Override
  @JsonView({View.Persistence.class, View.Public.class})
  public Date getCreationTimestamp() {
    return creationTimestamp;
  }

  @Override
  @JsonView({View.Persistence.class, View.Public.class})
  public Date getUpdateTimestamp() {
    return updateTimestamp;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getRoomId() {
    return roomId;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setRoomId(final String roomId) {
    this.roomId = roomId;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getCreatorId() {
    return creatorId;
  }

  @JsonView(View.Persistence.class)
  public void setCreatorId(final String creatorId) {
    this.creatorId = creatorId;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getTitle() {
    return title;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setTitle(final String title) {
    this.title = title;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getBody() {
    return body;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setBody(final String body) {
    this.body = body;
  }

  @JsonView(View.Public.class)
  public String getRenderedBody() {
    return renderedBody;
  }

  public void setRenderedBody(final String renderedBody) {
    this.renderedBody = renderedBody;
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * All fields of <tt>Announcement</tt> are included in equality checks.
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
    final Announcement announcement = (Announcement) o;

    return Objects.equals(roomId, announcement.roomId)
        && Objects.equals(title, announcement.title)
        && Objects.equals(body, announcement.body);
  }

  @Override
  protected ToStringCreator buildToString() {
    return super.buildToString()
        .append("roomId", roomId)
        .append("title", title)
        .append("body", body);
  }
}
