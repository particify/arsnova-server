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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.style.ToStringCreator;

import net.particify.arsnova.core.model.serialization.View;

public class RoomStatistics {
  private int currentParticipants;
  private int contentCount = 0;
  private int unansweredContentCount = 0;
  private int answerCount = 0;
  private int unreadAnswerCount = 0;
  private int commentCount = 0;
  private int unreadCommentCount = 0;
  private List<ContentGroupStatistics> groupStats;

  @JsonView(View.Public.class)
  public int getCurrentParticipants() {
    return currentParticipants;
  }

  public void setCurrentParticipants(final int currentParticipants) {
    this.currentParticipants = currentParticipants;
  }

  public int getUnansweredContentCount() {
    return unansweredContentCount;
  }

  public void setUnansweredContentCount(final int unansweredContentCount) {
    this.unansweredContentCount = unansweredContentCount;
  }

  @JsonView(View.Public.class)
  public int getContentCount() {
    return contentCount;
  }

  public void setContentCount(final int contentCount) {
    this.contentCount = contentCount;
  }

  @JsonView(View.Public.class)
  public int getAnswerCount() {
    return answerCount;
  }

  public void setAnswerCount(final int answerCount) {
    this.answerCount = answerCount;
  }

  public int getUnreadAnswerCount() {
    return unreadAnswerCount;
  }

  public void setUnreadAnswerCount(final int unreadAnswerCount) {
    this.unreadAnswerCount = unreadAnswerCount;
  }

  @JsonView(View.Public.class)
  public int getCommentCount() {
    return commentCount;
  }

  public void setCommentCount(final int commentCount) {
    this.commentCount = commentCount;
  }

  public int getUnreadCommentCount() {
    return unreadCommentCount;
  }

  public void setUnreadCommentCount(final int unreadCommentCount) {
    this.unreadCommentCount = unreadCommentCount;
  }

  @JsonView(View.Public.class)
  public List<ContentGroupStatistics> getGroupStats() {
    if (groupStats == null) {
      groupStats = new ArrayList<>();
    }

    return groupStats;
  }

  public void setGroupStats(final List<ContentGroupStatistics> groupStats) {
    this.groupStats = groupStats;
  }

  public RoomStatistics updateFromContentGroups(final Collection<ContentGroup> contentGroups) {
    setGroupStats(contentGroups.stream()
        .map(cg ->  new RoomStatistics.ContentGroupStatistics(cg)).collect(Collectors.toList()));
    setContentCount(contentGroups.stream()
        .mapToInt(cg -> cg.getContentIds().size()).reduce((a, b) -> a + b).orElse(0));

    return this;
  }

  @Override
  public String toString() {
    return new ToStringCreator(this)
      .append("currentParticipants", currentParticipants)
      .append("contentCount", contentCount)
      .append("unansweredContentCount", unansweredContentCount)
      .append("answerCount", answerCount)
      .append("unreadAnswerCount", unreadAnswerCount)
      .append("commentCount", commentCount)
      .append("unreadCommentCount", unreadCommentCount)
      .append("groupStats", groupStats)
      .toString();
  }

  public static class ContentGroupStatistics {
    private String id;
    private String groupName;
    private int contentCount = 0;

    public ContentGroupStatistics() {

    }

    public ContentGroupStatistics(final ContentGroup contentGroup) {
      this.setId(contentGroup.getId());
      this.setGroupName(contentGroup.getName());
      this.setContentCount(contentGroup.getContentIds().size());
    }

    @JsonView(View.Public.class)
    public String getId() {
      return id;
    }

    public void setId(final String id) {
      this.id = id;
    }

    @JsonView(View.Public.class)
    public String getGroupName() {
      return groupName;
    }

    public void setGroupName(final String groupName) {
      this.groupName = groupName;
    }

    @JsonView(View.Public.class)
    public int getContentCount() {
      return contentCount;
    }

    public void setContentCount(final int contentCount) {
      this.contentCount = contentCount;
    }

    @Override
    public String toString() {
      return new ToStringCreator(this)
        .append("id", id)
        .append("groupName", groupName)
        .append("contentCount", contentCount)
        .toString();
    }
  }
}
