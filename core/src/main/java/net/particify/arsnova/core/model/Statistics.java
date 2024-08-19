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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.HashMap;
import java.util.Map;

import net.particify.arsnova.core.model.serialization.View;

@JsonView(View.Admin.class)
public class Statistics {
  @JsonView(View.Admin.class)
  public static class UserProfileStats {
    private int totalCount;

    // Exclude if value is 0 because it can also mean N/A for deleted when multi
    // tenancy is active
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int deleted;

    private int accountCount;
    private Map<String, Integer> countByAuthProvider = new HashMap<>();
    private int activationsPending;

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }

    public int getDeleted() {
      return deleted;
    }

    public void setDeleted(final int deleted) {
      this.deleted = deleted;
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

    // Exclude if value is 0 because it can also mean N/A for deleted when multi
    // tenancy is active
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int deleted;

    private int closed;

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }

    public int getDeleted() {
      return deleted;
    }

    public void setDeleted(final int deleted) {
      this.deleted = deleted;
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

    // Exclude if value is 0 because it can also mean N/A for deleted when multi
    // tenancy is active
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int deleted;

    private int published;
    private Map<String, Integer> countByPublishingMode = new HashMap<>();
    private Map<String, Integer> countByGroupType = new HashMap<>();
    private int fromTemplate;

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }

    public int getDeleted() {
      return deleted;
    }

    public void setDeleted(final int deleted) {
      this.deleted = deleted;
    }

    public int getPublished() {
      return published;
    }

    public void setPublished(final int published) {
      this.published = published;
    }

    public Map<String, Integer> getCountByPublishingMode() {
      return countByPublishingMode;
    }

    public void setCountByPublishingMode(final Map<String, Integer> countByPublishingMode) {
      this.countByPublishingMode = countByPublishingMode;
    }

    public Map<String, Integer> getCountByGroupType() {
      return countByGroupType;
    }

    public void setCountByGroupType(final Map<String, Integer> countByGroupType) {
      this.countByGroupType = countByGroupType;
    }

    public int getFromTemplate() {
      return fromTemplate;
    }

    public void setFromTemplate(final int fromTemplate) {
      this.fromTemplate = fromTemplate;
    }
  }

  @JsonView(View.Admin.class)
  public static class ContentStats {
    private int totalCount;

    // Exclude if value is 0 because it can also mean N/A for deleted when multi
    // tenancy is active
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int deleted;

    private Map<String, Integer> countByFormat = new HashMap<>();
    private int fromTemplate;

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }

    public int getDeleted() {
      return deleted;
    }

    public void setDeleted(final int deleted) {
      this.deleted = deleted;
    }

    public Map<String, Integer> getCountByFormat() {
      return countByFormat;
    }

    public void setCountByFormat(final Map<String, Integer> countByFormat) {
      this.countByFormat = countByFormat;
    }

    public int getFromTemplate() {
      return fromTemplate;
    }

    public void setFromTemplate(final int fromTemplate) {
      this.fromTemplate = fromTemplate;
    }
  }

  @JsonView(View.Admin.class)
  public static class ContentGroupTemplateStats {
    private int totalCount;

    // Exclude if value is 0 because it can also mean N/A for deleted when multi
    // tenancy is active
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int deleted;

    private Map<String, Integer> countByLanguage = new HashMap<>();
    private Map<String, Integer> countByLicense = new HashMap<>();

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }

    public int getDeleted() {
      return deleted;
    }

    public void setDeleted(final int deleted) {
      this.deleted = deleted;
    }

    public Map<String, Integer> getCountByLanguage() {
      return countByLanguage;
    }

    public void setCountByLanguage(final Map<String, Integer> countByLanguage) {
      this.countByLanguage = countByLanguage;
    }

    public Map<String, Integer> getCountByLicense() {
      return countByLicense;
    }

    public void setCountByLicense(final Map<String, Integer> countByLicense) {
      this.countByLicense = countByLicense;
    }
  }

  @JsonView(View.Admin.class)
  public static class ContentTemplateStats {
    private int totalCount;

    // Exclude if value is 0 because it can also mean N/A for deleted when multi
    // tenancy is active
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int deleted;

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }

    public int getDeleted() {
      return deleted;
    }

    public void setDeleted(final int deleted) {
      this.deleted = deleted;
    }
  }

  @JsonView(View.Admin.class)
  public static class AnswerStats {
    private int totalCount;

    // Exclude if value is 0 because it can also mean N/A for deleted when multi
    // tenancy is active
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int deleted;

    private Map<String, Integer> countByFormat = new HashMap<>();

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }

    public int getDeleted() {
      return deleted;
    }

    public void setDeleted(final int deleted) {
      this.deleted = deleted;
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

    // Exclude if value is 0 because it can also mean N/A for deleted when multi
    // tenancy is active
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int deleted;


    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }

    public int getDeleted() {
      return deleted;
    }

    public void setDeleted(final int deleted) {
      this.deleted = deleted;
    }
  }

  @JsonView(View.Admin.class)
  public static class ViolationReportStats {
    private int totalCount;

    // Exclude if value is 0 because it can also mean N/A for deleted when multi
    // tenancy is active
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int deleted;

    private Map<String, Integer> countByReason = new HashMap<>();
    private Map<String, Integer> countByDecision = new HashMap<>();

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(final int totalCount) {
      this.totalCount = totalCount;
    }

    public int getDeleted() {
      return deleted;
    }

    public void setDeleted(final int deleted) {
      this.deleted = deleted;
    }

    public Map<String, Integer> getCountByReason() {
      return countByReason;
    }

    public void setCountByReason(final Map<String, Integer> countByReason) {
      this.countByReason = countByReason;
    }

    public Map<String, Integer> getCountByDecision() {
      return countByDecision;
    }

    public void setCountByDecision(final Map<String, Integer> countByDecision) {
      this.countByDecision = countByDecision;
    }
  }

  private UserProfileStats userProfile;
  private RoomStats room;
  private ContentGroupStats contentGroup;
  private ContentStats content;
  private ContentGroupTemplateStats contentGroupTemplate;
  private ContentTemplateStats contentTemplate;
  private AnswerStats answer;
  private AnnouncementStats announcement;
  private ViolationReportStats violationReport;

  public Statistics() {
    this.userProfile = new UserProfileStats();
    this.room = new RoomStats();
    this.contentGroup = new ContentGroupStats();
    this.content = new ContentStats();
    this.contentGroupTemplate = new ContentGroupTemplateStats();
    this.contentTemplate = new ContentTemplateStats();
    this.answer = new AnswerStats();
    this.announcement = new AnnouncementStats();
    this.violationReport = new ViolationReportStats();
  }

  public Statistics(
      final UserProfileStats userProfile,
      final RoomStats room,
      final ContentGroupStats contentGroup,
      final ContentStats content,
      final ContentGroupTemplateStats contentGroupTemplate,
      final ContentTemplateStats contentTemplate,
      final AnswerStats answer,
      final AnnouncementStats announcement,
      final ViolationReportStats violationReport) {
    this.userProfile = userProfile;
    this.room = room;
    this.contentGroup = contentGroup;
    this.content = content;
    this.contentGroupTemplate = contentGroupTemplate;
    this.contentTemplate = contentTemplate;
    this.answer = answer;
    this.announcement = announcement;
    this.violationReport = violationReport;
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

  public ContentGroupTemplateStats getContentGroupTemplate() {
    return contentGroupTemplate;
  }

  public void setContentGroupTemplate(final ContentGroupTemplateStats contentGroupTemplate) {
    this.contentGroupTemplate = contentGroupTemplate;
  }

  public ContentTemplateStats getContentTemplate() {
    return contentTemplate;
  }

  public void setContentTemplate(final ContentTemplateStats contentTemplate) {
    this.contentTemplate = contentTemplate;
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

  public ViolationReportStats getViolationReport() {
    return violationReport;
  }

  public void setViolationReport(final ViolationReportStats violationReport) {
    this.violationReport = violationReport;
  }
}
