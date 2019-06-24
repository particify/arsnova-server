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
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewResult;
import org.ektorp.support.CouchDbRepositorySupport;

import de.thm.arsnova.model.Room;
import de.thm.arsnova.persistence.SessionStatisticsRepository;
import de.thm.arsnova.service.score.Score;

public class CouchDbSessionStatisticsRepository extends CouchDbRepositorySupport implements SessionStatisticsRepository {
	public CouchDbSessionStatisticsRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Object.class, db, "learning_progress", createIfNotExists);
	}

	@Override
	public Score getLearningProgress(final Room room) {
		final ViewResult maximumValueResult = db.queryView(createQuery("maximum_value_of_question")
				.startKey(ComplexKey.of(room.getId()))
				.endKey(ComplexKey.of(room.getId(), ComplexKey.emptyObject())));
		final ViewResult answerSumResult = db.queryView(createQuery("question_value_achieved_for_user")
				.startKey(ComplexKey.of(room.getId()))
				.endKey(ComplexKey.of(room.getId(), ComplexKey.emptyObject())));
		final Score courseScore = new Score();

		// no results found
		if (maximumValueResult.isEmpty() && answerSumResult.isEmpty()) {
			return courseScore;
		}

		// collect mapping (questionId -> max value)
		for (ViewResult.Row row : maximumValueResult) {
			final String contentId = row.getKeyAsNode().get(1).asText();
			final JsonNode value = row.getValueAsNode();
			final int questionScore = value.get("value").asInt();
			final String questionVariant = value.get("questionVariant").asText();
			final int piRound = value.get("piRound").asInt();
			courseScore.addQuestion(contentId, questionVariant, piRound, questionScore);
		}
		// collect mapping (questionId -> (user -> value))
		for (ViewResult.Row row : answerSumResult) {
			final String username = row.getKeyAsNode().get(1).asText();
			final JsonNode value = row.getValueAsNode();
			final String contentId = value.get("questionId").asText();
			final int userscore = value.get("score").asInt();
			final int piRound = value.get("piRound").asInt();
			courseScore.addAnswer(contentId, piRound, username, userscore);
		}
		return courseScore;
	}
}
