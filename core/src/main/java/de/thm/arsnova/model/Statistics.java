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

import com.fasterxml.jackson.annotation.JsonView;
import java.util.HashMap;
import java.util.Map;

import de.thm.arsnova.model.serialization.View;

@JsonView(View.Admin.class)
public class Statistics {
  @JsonView(View.Admin.class)
  public static class UserProfileStats {
    private int totalCount;
    private int accountCount;
    private Map<String, Integer> countByAuthProvider = new HashMap<>();
    private int activationsPending;

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }

    public int getAccountCount() {
      return accountCount;
    }

    public void setAccountCount(final int accountCount) {
      this.accountCount = accountCount;
    }

    public Map<String, Integer> getCountByAuthProvider() {
      return countByAuthProvider;
    }

    public void setCountByAuthProvider(final Map<String, Integer> countByAuthProvider) {
      this.countByAuthProvider = countByAuthProvider;
    }

    public int getActivationsPending() {
      return activationsPending;
    }

    public void setActivationsPending(final int activationsPending) {
      this.activationsPending = activationsPending;
    }
  }

  @JsonView(View.Admin.class)
  public static class RoomStats {
    private int totalCount;
    private int closed;

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }

    public int getClosed() {
      return closed;
    }

    public void setClosed(final int closed) {
      this.closed = closed;
    }
  }

  @JsonView(View.Admin.class)
  public static class ContentGroupStats {
    private int totalCount;
    private int published;
    private int usingPublishingRange;

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }

    public int getPublished() {
      return published;
    }

    public void setPublished(final int published) {
      this.published = published;
    }

    public int getUsingPublishingRange() {
      return usingPublishingRange;
    }

    public void setUsingPublishingRange(final int usingPublishingRange) {
      this.usingPublishingRange = usingPublishingRange;
    }
  }

  @JsonView(View.Admin.class)
  public static class ContentStats {
    private int totalCount;
    private Map<String, Integer> countByFormat = new HashMap<>();

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }

    public Map<String, Integer> getCountByFormat() {
      return countByFormat;
    }

    public void setCountByFormat(final Map<String, Integer> countByFormat) {
      this.countByFormat = countByFormat;
    }
  }

  @JsonView(View.Admin.class)
  public static class AnswerStats {
    private int totalCount;
    private Map<String, Integer> countByFormat = new HashMap<>();

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }

    public Map<String, Integer> getCountByFormat() {
      return countByFormat;
    }

    public void setCountByFormat(final Map<String, Integer> countByFormat) {
      this.countByFormat = countByFormat;
    }
  }

  @JsonView(View.Admin.class)
  public static class AnnouncementStats {
    private int totalCount;

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }
  }

  private UserProfileStats userProfile;
  private RoomStats room;
  private ContentGroupStats contentGroup;
  private ContentStats content;
  private AnswerStats answer;
  private AnnouncementStats announcement;

  public Statistics() {
    this.userProfile = new UserProfileStats();
    this.room = new RoomStats();
    this.contentGroup = new ContentGroupStats();
    this.content = new ContentStats();
    this.answer = new AnswerStats();
    this.announcement = new AnnouncementStats();
  }

  public Statistics(
      final UserProfileStats userProfile,
      final RoomStats room,
      final ContentGroupStats contentGroup,
      final ContentStats content,
      final AnswerStats answer,
      final AnnouncementStats announcement) {
    this.userProfile = userProfile;
    this.room = room;
    this.contentGroup = contentGroup;
    this.content = content;
    this.answer = answer;
    this.announcement = announcement;
  }

  public UserProfileStats getUserProfile() {
    return userProfile;
  }

  public void setUserProfile(final UserProfileStats userProfile) {
    this.userProfile = userProfile;
  }

  public RoomStats getRoom() {
    return room;
  }

  public void setRoom(final RoomStats room) {
    this.room = room;
  }

  public ContentGroupStats getContentGroup() {
    return contentGroup;
  }

  public void setContentGroup(final ContentGroupStats contentGroup) {
    this.contentGroup = contentGroup;
  }

  public ContentStats getContent() {
    return content;
  }

  public void setContent(final ContentStats content) {
    this.content = content;
  }

  public AnswerStats getAnswer() {
    return answer;
  }

  public void setAnswer(final AnswerStats answer) {
    this.answer = answer;
  }

  public AnnouncementStats getAnnouncement() {
    return announcement;
  }

  public void setAnnouncement(final AnnouncementStats announcement) {
    this.announcement = announcement;
  }
}
