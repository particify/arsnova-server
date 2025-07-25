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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Date;
import java.util.Objects;
import org.springframework.core.style.ToStringCreator;

import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.validation.LanguageIso639;

public class Room extends Entity implements RoomIdAware {
  @JsonView(View.Persistence.class)
  public static class ImportMetadata {
    private String source;
    private Date timestamp;
    private String jobId;

    public String getSource() {
      return source;
    }

    public void setSource(final String source) {
      this.source = source;
    }

    public Date getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(final Date timestamp) {
      this.timestamp = timestamp;
    }

    public String getJobId() {
      return jobId;
    }

    public void setJobId(final String jobId) {
      this.jobId = jobId;
    }
  }

  @NotEmpty
  private String shortId;

  @NotEmpty
  private String ownerId;

  @NotBlank
  private String name;

  private String description;
  private String renderedDescription;
  private boolean closed;
  private boolean template;
  private String password;
  private String lmsCourseId;
  private Date scheduledDeletion;

  @LanguageIso639
  private String language;

  private ImportMetadata importMetadata;
  private RoomStatistics statistics;

  {
    final TextRenderingOptions options = new TextRenderingOptions();
    options.setMarkdownFeatureset(TextRenderingOptions.MarkdownFeatureset.EXTENDED);
    this.addRenderingMapping(
        this::getDescription,
        this::setRenderedDescription,
        options);
  }

  public Room() {

  }

  /**
   * Copying constructor which adopts most of the original room's properties
   * which are not used to store relations to other data.
   */
  public Room(final Room room) {
    super(room);
    this.name = room.name;
    this.description = room.description;
    this.closed = room.closed;
    this.language = room.language;
  }

  @Override
  public String getRoomId() {
    return id;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getShortId() {
    return shortId;
  }

  @JsonView(View.Persistence.class)
  public void setShortId(final String shortId) {
    this.shortId = shortId;
  }

  @JsonView({View.Persistence.class, View.Admin.class})
  public String getOwnerId() {
    return ownerId;
  }

  @JsonView(View.Persistence.class)
  public void setOwnerId(final String ownerId) {
    this.ownerId = ownerId;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getName() {
    return name;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setName(final String name) {
    this.name = name;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getDescription() {
    return description;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setDescription(final String description) {
    this.description = description;
  }

  @JsonView(View.Public.class)
  public String getRenderedDescription() {
    return renderedDescription;
  }

  public void setRenderedDescription(final String renderedDescription) {
    this.renderedDescription = renderedDescription;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public boolean isClosed() {
    return closed;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setClosed(final boolean closed) {
    this.closed = closed;
  }

  @JsonView(View.Persistence.class)
  public boolean isTemplate() {
    return template;
  }

  @JsonView(View.Persistence.class)
  public void setTemplate(final boolean template) {
    this.template = template;
  }

  @JsonView(View.Persistence.class)
  public String getPassword() {
    return password;
  }

  @JsonView(View.Persistence.class)
  public void setPassword(final String password) {
    this.password = password == null || password.isBlank() ? null : password;
  }

  @JsonView(View.Public.class)
  public boolean isPasswordProtected() {
    return this.password != null;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getLmsCourseId() {
    return lmsCourseId;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setLmsCourseId(final String lmsCourseId) {
    this.lmsCourseId = lmsCourseId;
  }

  @JsonView(View.Persistence.class)
  public Date getScheduledDeletion() {
    return scheduledDeletion;
  }

  @JsonView(View.Persistence.class)
  public void setScheduledDeletion(final Date scheduledDeletion) {
    this.scheduledDeletion = scheduledDeletion;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getLanguage() {
    return language;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setLanguage(final String language) {
    this.language = language;
  }

  @JsonView(View.Persistence.class)
  public ImportMetadata getImportMetadata() {
    return importMetadata;
  }

  @JsonView(View.Persistence.class)
  public void setImportMetadata(final ImportMetadata importMetadata) {
    this.importMetadata = importMetadata;
  }

  @JsonView(View.Public.class)
  public RoomStatistics getStatistics() {
    return statistics;
  }

  public void setStatistics(final RoomStatistics statistics) {
    this.statistics = statistics;
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * The following fields of <tt>Room</tt> are excluded from equality checks:
   * {@link #statistics}.
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
    final Room room = (Room) o;

    return closed == room.closed
        && Objects.equals(shortId, room.shortId)
        && Objects.equals(ownerId, room.ownerId)
        && Objects.equals(name, room.name)
        && Objects.equals(description, room.description)
        && Objects.equals(language, room.language);
  }

  @Override
  public int hashCode() {
    return hashCode(super.hashCode(), closed, shortId, ownerId, name, description, language);
  }

  @Override
  protected ToStringCreator buildToString() {
    return super.buildToString()
        .append("shortId", shortId)
        .append("ownerId", ownerId)
        .append("name", name)
        .append("description", description)
        .append("closed", closed)
        .append("passwordProtected", isPasswordProtected())
        .append("language", language)
        .append("statistics", statistics);
  }
}
