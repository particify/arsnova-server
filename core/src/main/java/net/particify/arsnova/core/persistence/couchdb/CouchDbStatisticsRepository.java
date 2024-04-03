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

package net.particify.arsnova.core.persistence.couchdb;

import com.fasterxml.jackson.databind.JsonNode;
import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.ViewResult;
import org.ektorp.support.CouchDbRepositorySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.particify.arsnova.core.model.Statistics;
import net.particify.arsnova.core.model.UserProfile;
import net.particify.arsnova.core.persistence.StatisticsRepository;

public class CouchDbStatisticsRepository extends CouchDbRepositorySupport implements StatisticsRepository {
  private static final Logger logger = LoggerFactory.getLogger(CouchDbStatisticsRepository.class);

  protected final int keyOffset;

  public CouchDbStatisticsRepository(final CouchDbConnector db, final boolean createIfNotExists) {
    super(Object.class, db, "statistics", createIfNotExists);
    this.keyOffset = 0;
  }

  public CouchDbStatisticsRepository(
      final CouchDbConnector db,
      final boolean createIfNotExists,
      final int keyOffset) {
    super(Object.class, db, "statistics", createIfNotExists);
    this.keyOffset = keyOffset;
  }

  @Override
  public Statistics getStatistics() {
    try {
      final ViewResult viewResult = fetchStatistics();
      return parseStatistics(viewResult);
    } catch (final DbAccessException e) {
      logger.error("Could not retrieve statistics.", e);
      return new Statistics();
    }
  }

  protected ViewResult fetchStatistics() {
    return db.queryView(createQuery("statistics").group(true));
  }

  protected Statistics parseStatistics(final ViewResult viewResult) {
    final Statistics stats = new Statistics();
    final Statistics.UserProfileStats userProfileStats = stats.getUserProfile();
    final Statistics.RoomStats roomStats = stats.getRoom();
    final Statistics.ContentGroupStats contentGroupStats = stats.getContentGroup();
    final Statistics.ContentStats contentStats = stats.getContent();
    final Statistics.ContentGroupTemplateStats contentGroupTemplateStats = stats.getContentGroupTemplate();
    final Statistics.ContentTemplateStats contentTemplateStats = stats.getContentTemplate();
    final Statistics.AnswerStats answerStats = stats.getAnswer();
    final Statistics.AnnouncementStats announcementStats = stats.getAnnouncement();
    final Statistics.ViolationReportStats violationReportStats = stats.getViolationReport();

    if (viewResult.isEmpty()) {
      return stats;
    }

    for (final ViewResult.Row row : viewResult.getRows()) {
      final JsonNode key = row.getKeyAsNode();
      final int value = row.getValueAsInt();
      if (!key.isArray()) {
        throw new DbAccessException("Invalid key for statistics item.");
      }
      final int offsetKeySize = key.size() - keyOffset;

      switch (key.get(keyOffset + 0).asText()) {
        case "UserProfile":
          if (offsetKeySize == 1) {
            userProfileStats.setTotalCount(value);
          } else if (offsetKeySize > 1) {
            switch (key.get(keyOffset + 1).asText()) {
              case "deleted":
                userProfileStats.setDeleted(value);
                break;
              case "activationPending":
                userProfileStats.setActivationsPending(value);
                break;
              case "authProvider":
                userProfileStats.getCountByAuthProvider().put(key.get(keyOffset + 2).asText(), value);
                break;
              default:
                break;
            }
          }
          break;
        case "Room":
          if (offsetKeySize == 1) {
            roomStats.setTotalCount(value);
          } else if (offsetKeySize > 1) {
            switch (key.get(keyOffset + 1).asText()) {
              case "deleted":
                roomStats.setDeleted(value);
                break;
              case "closed":
                roomStats.setClosed(value);
                break;
              default:
                break;
            }
          }
          break;
        case "ContentGroup":
          if (offsetKeySize == 1) {
            contentGroupStats.setTotalCount(value);
          } else if (offsetKeySize > 1) {
            switch (key.get(keyOffset + 1).asText()) {
              case "deleted":
                contentGroupStats.setDeleted(value);
                break;
              case "publishingMode":
                contentGroupStats.getCountByPublishingMode().put(key.get(keyOffset + 2).asText(), value);
                break;
              case "fromTemplate":
                contentGroupStats.setFromTemplate(value);
                break;
              default:
                break;
            }
          }
          break;
        case "Content":
          if (offsetKeySize == 1) {
            contentStats.setTotalCount(value);
          } else if (offsetKeySize > 1) {
            switch (key.get(keyOffset + 1).asText()) {
              case "deleted":
                contentStats.setDeleted(value);
                break;
              case "format":
                contentStats.getCountByFormat().put(key.get(keyOffset + 2).asText(), value);
                break;
              case "fromTemplate":
                contentStats.setFromTemplate(value);
                break;
              default:
                break;
            }
          }
          break;
        case "ContentGroupTemplate":
          if (offsetKeySize == 1) {
            contentGroupTemplateStats.setTotalCount(value);
          } else if (offsetKeySize > 1) {
            switch (key.get(keyOffset + 1).asText()) {
              case "deleted":
                contentGroupTemplateStats.setDeleted(value);
                break;
              case "language":
                contentGroupTemplateStats.getCountByLanguage().put(key.get(keyOffset + 2).asText(), value);
                break;
              case "license":
                contentGroupTemplateStats.getCountByLicense().put(key.get(keyOffset + 2).asText(), value);
                break;
              default:
                break;
            }
          }
          break;
        case "ContentTemplate":
          if (offsetKeySize == 1) {
            contentTemplateStats.setTotalCount(value);
          } else if (offsetKeySize > 1) {
            switch (key.get(keyOffset + 1).asText()) {
              case "deleted":
                contentTemplateStats.setDeleted(value);
                break;
              default:
                break;
            }
          }
          break;
        case "Answer":
          if (offsetKeySize == 1) {
            answerStats.setTotalCount(value);
          } else if (offsetKeySize > 1) {
            switch (key.get(keyOffset + 1).asText()) {
              case "deleted":
                answerStats.setDeleted(value);
                break;
              case "format":
                answerStats.getCountByFormat().put(key.get(keyOffset + 2).asText(), value);
                break;
              default:
                break;
            }
          }
          break;
        case "Announcement":
          if (offsetKeySize == 1) {
            announcementStats.setTotalCount(value);
          } else if (offsetKeySize > 1) {
            switch (key.get(keyOffset + 1).asText()) {
              case "deleted":
                announcementStats.setDeleted(value);
                break;
              default:
                break;
            }
          }
          break;
        case "ViolationReport":
          if (offsetKeySize == 1) {
            violationReportStats.setTotalCount(value);
          } else if (offsetKeySize > 1) {
            switch (key.get(keyOffset + 1).asText()) {
              case "deleted":
                violationReportStats.setDeleted(value);
                break;
              case "reason":
                violationReportStats.getCountByReason().put(key.get(keyOffset + 2).asText(), value);
                break;
              case "decision":
                violationReportStats.getCountByDecision().put(key.get(keyOffset + 2).asText(), value);
                break;
              default:
                break;
            }
          }
          break;
        default:
          break;
      }
    }

    userProfileStats.setAccountCount(
        userProfileStats.getTotalCount()
        - userProfileStats.getCountByAuthProvider()
        .getOrDefault(UserProfile.AuthProvider.ARSNOVA_GUEST.toString(), 0)
        - userProfileStats.getActivationsPending());

    return stats;
  }
}
