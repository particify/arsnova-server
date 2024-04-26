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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.core.style.ToStringCreator;

import net.particify.arsnova.core.model.serialization.View;

public class ContentGroup extends Entity implements RoomIdAware {
  @NotEmpty
  private String roomId;

  @NotBlank
  private String name;

  @NotNull
  private GroupType groupType = GroupType.MIXED;

  private List<String> contentIds;

  @NotNull
  private PublishingMode publishingMode = PublishingMode.NONE;

  /**
   * The index of the published content (publishingMode SINGLE) or the last
   * published content (UP_TO). Not relevant for the publishingModes NONE and
   * ALL.
   */
  @PositiveOrZero
  private int publishingIndex;

  private boolean statisticsPublished = true;
  private boolean correctOptionsPublished = true;

  private String templateId;

  public ContentGroup() {

  }

  public ContentGroup(final String roomId, final String name) {
    this.roomId = roomId;
    this.name = name;
  }

  /**
   * Copying constructor which adopts most of the original content group's
   * properties which are not used to store relations to other data.
   */
  public ContentGroup(final ContentGroup contentGroup) {
    super(contentGroup);
    this.name = contentGroup.name;
    this.groupType = contentGroup.groupType;
    this.contentIds = contentGroup.contentIds;
    this.publishingMode = contentGroup.publishingMode;
    this.publishingIndex = contentGroup.publishingIndex;
    this.statisticsPublished = contentGroup.statisticsPublished;
    this.correctOptionsPublished = contentGroup.correctOptionsPublished;
    this.templateId = contentGroup.templateId;
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
  public String getName() {
    return this.name;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setName(final String name) {
    this.name = name;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public GroupType getGroupType() {
    return groupType;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setGroupType(final GroupType groupType) {
    this.groupType = groupType;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public List<String> getContentIds() {
    if (contentIds == null) {
      contentIds = new ArrayList<>();
    }

    return contentIds;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setContentIds(final List<String> contentIds) {
    this.contentIds = contentIds;
    if (publishingIndex != 0 && publishingIndex >= contentIds.size()) {
      this.publishingIndex = contentIds.size() - 1;
    }
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public PublishingMode getPublishingMode() {
    return publishingMode;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setPublishingMode(final PublishingMode publishingMode) {
    this.publishingMode = publishingMode;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public int getPublishingIndex() {
    return publishingIndex;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setPublishingIndex(final int publishingIndex) {
    this.publishingIndex = publishingIndex != 0 && publishingIndex >= contentIds.size()
        ? contentIds.size() - 1
        : publishingIndex;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public boolean isStatisticsPublished() {
    return statisticsPublished;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setStatisticsPublished(final boolean statisticsPublished) {
    this.statisticsPublished = statisticsPublished;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public boolean isCorrectOptionsPublished() {
    return correctOptionsPublished;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setCorrectOptionsPublished(final boolean correctOptionsPublished) {
    this.correctOptionsPublished = correctOptionsPublished;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getTemplateId() {
    return templateId;
  }

  @JsonView(View.Persistence.class)
  public void setTemplateId(final String templateId) {
    this.templateId = templateId;
  }

  public boolean isPublished() {
    return publishingMode != PublishingMode.NONE;
  }

  public boolean isContentPublished(final String contentId) {
    final int i = contentIds.indexOf(contentId);
    return switch (publishingMode) {
      case NONE -> false;
      case ALL -> true;
      case SINGLE -> i == publishingIndex;
      case UP_TO -> i <= publishingIndex;
    };
  }

  public boolean containsContent(final String contentId) {
    return contentIds.contains(contentId);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ContentGroup that = (ContentGroup) o;

    return Objects.equals(name, that.name)
      && Objects.equals(groupType, that.groupType)
      && Objects.equals(contentIds, that.contentIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, groupType, contentIds);
  }

  @Override
  public String toString() {
    return new ToStringCreator(this)
        .append("name", name)
        .append("groupType", groupType)
        .append("contentIds", contentIds)
        .append("publishingMode", publishingMode)
        .append("publishingIndex", publishingIndex)
        .append("statisticsPublished", statisticsPublished)
        .append("correctOptionsPublished", correctOptionsPublished)
        .toString();
  }

  public enum GroupType {
    MIXED,
    QUIZ
  }

  public enum PublishingMode {
    /**
     * Publishes no contents / do not publish content group at all.
     */
    NONE,
    /**
     * Publishes all contents.
     */
    ALL,
    /**
     * Publishes the content referenced by publishingIndex.
     */
    SINGLE,
    /**
     * Publishes contents up to publishingIndex.
     */
    UP_TO
  }
}
