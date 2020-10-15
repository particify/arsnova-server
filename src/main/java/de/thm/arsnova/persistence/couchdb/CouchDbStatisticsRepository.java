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

package de.thm.arsnova.persistence.couchdb;

import com.fasterxml.jackson.databind.JsonNode;
import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.ViewResult;
import org.ektorp.support.CouchDbRepositorySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thm.arsnova.model.Statistics;
import de.thm.arsnova.persistence.StatisticsRepository;

public class CouchDbStatisticsRepository extends CouchDbRepositorySupport implements StatisticsRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbStatisticsRepository.class);

	public CouchDbStatisticsRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Object.class, db, "statistics", createIfNotExists);
	}

	@Override
	public Statistics getStatistics() {
		final Statistics stats = new Statistics();
		final Statistics.UserProfileStats userProfileStats = stats.getUserProfile();
		final Statistics.RoomStats roomStats = stats.getRoom();
		final Statistics.ContentStats contentStats = stats.getContent();
		final Statistics.AnswerStats answerStats = stats.getAnswer();
		final Statistics.CommentStats commentStats = stats.getComment();

		try {
			final ViewResult statsResult = db.queryView(createQuery("statistics").group(true));

			if (!statsResult.isEmpty()) {
				for (final ViewResult.Row row: statsResult.getRows()) {
					final JsonNode key = row.getKeyAsNode();
					final int value = row.getValueAsInt();
					if (!key.isArray()) {
						throw new DbAccessException("Invalid key for statistics item.");
					}
					switch (key.get(0).asText()) {
						case "UserProfile":
							if (key.size() == 1) {
								userProfileStats.setTotalCount(value);
							} else if (key.size() > 1) {
								switch (key.get(1).asText()) {
									case "activationPending":
										userProfileStats.setActivationsPending(value);
										break;
									case "authProvider":
										userProfileStats.getCountByAuthProvider().put(key.get(2).asText(), value);
										break;
									default:
										break;
								}
							}
							break;
						case "Room":
							if (key.size() == 1) {
								roomStats.setTotalCount(value);
							} else if (key.size() > 1) {
								switch (key.get(1).asText()) {
									case "closed":
										roomStats.setClosed(value);
										break;
									case "moderated":
										roomStats.setModerated(value);
										break;
									case "moderators":
										roomStats.setModerators(value);
										break;
									default:
										break;
								}
							}
							break;
						case "Content":
							if (key.size() == 1) {
								contentStats.setTotalCount(value);
							} else if (key.size() > 1) {
								switch (key.get(1).asText()) {
									case "format":
										contentStats.getCountByFormat().put(key.get(2).asText(), value);
										break;
									default:
										break;
								}
							}
							break;
						case "Answer":
							if (key.size() == 1) {
								answerStats.setTotalCount(value);
							} else if (key.size() > 1) {
								switch (key.get(1).asText()) {
									case "format":
										answerStats.getCountByFormat().put(key.get(2).asText(), value);
										break;
									default:
										break;
								}
							}
							break;
						case "Comment":
							if (key.size() == 1) {
								commentStats.setTotalCount(value);
							}
							break;
						default:
							break;
					}
				}
			}

			return stats;
		} catch (final DbAccessException e) {
			logger.error("Could not retrieve statistics.", e);
		}

		return stats;
	}
}
