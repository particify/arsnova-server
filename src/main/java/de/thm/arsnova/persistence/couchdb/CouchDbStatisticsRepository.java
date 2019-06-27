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

import java.util.HashSet;
import java.util.Set;
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
		try {
			final ViewResult statsResult = db.queryView(createQuery("statistics").group(true));
			final ViewResult creatorResult = db.queryView(createQuery("unique_session_creators").group(true));
			final ViewResult studentUserResult = db.queryView(createQuery("active_student_users").group(true));

			if (!statsResult.isEmpty()) {
				for (final ViewResult.Row row: statsResult.getRows()) {
					final int value = row.getValueAsInt();
					switch (row.getKey()) {
						case "openSessions":
							stats.setOpenSessions(stats.getOpenSessions() + value);
							break;
						case "closedSessions":
							stats.setClosedSessions(stats.getClosedSessions() + value);
							break;
						case "deletedSessions":
							/* Deleted sessions are not exposed separately for now. */
							stats.setClosedSessions(stats.getClosedSessions() + value);
							break;
						case "answers":
							stats.setAnswers(stats.getAnswers() + value);
							break;
						case "lectureQuestions":
							stats.setLectureQuestions(stats.getLectureQuestions() + value);
							break;
						case "preparationQuestions":
							stats.setPreparationQuestions(stats.getPreparationQuestions() + value);
							break;
						case "interposedQuestions":
							stats.setInterposedQuestions(stats.getInterposedQuestions() + value);
							break;
						case "conceptQuestions":
							stats.setConceptQuestions(stats.getConceptQuestions() + value);
							break;
						case "flashcards":
							stats.setFlashcards(stats.getFlashcards() + value);
							break;
						default:
							break;
					}
				}
			}
			if (!creatorResult.isEmpty()) {
				final Set<String> creators = new HashSet<>();
				for (ViewResult.Row row: statsResult.getRows()) {
					creators.add(row.getKey());
				}
				stats.setCreators(creators.size());
			}
			if (!studentUserResult.isEmpty()) {
				final Set<String> students = new HashSet<>();
				for (ViewResult.Row row: statsResult.getRows()) {
					students.add(row.getKey());
				}
				stats.setActiveStudents(students.size());
			}
			return stats;
		} catch (final DbAccessException e) {
			logger.error("Could not retrieve statistics.", e);
		}

		return stats;
	}
}
