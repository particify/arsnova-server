package de.thm.arsnova.persistance.couchdb;

import com.fasterxml.jackson.databind.JsonNode;
import de.thm.arsnova.services.score.Score;
import de.thm.arsnova.entities.migration.v2.Session;
import de.thm.arsnova.persistance.SessionStatisticsRepository;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewResult;
import org.ektorp.support.CouchDbRepositorySupport;

public class CouchDbSessionStatisticsRepository extends CouchDbRepositorySupport implements SessionStatisticsRepository {
	public CouchDbSessionStatisticsRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Object.class, db, "learning_progress", createIfNotExists);
	}

	@Override
	public Score getLearningProgress(final Session session) {
		final ViewResult maximumValueResult = db.queryView(createQuery("maximum_value_of_question")
				.startKey(ComplexKey.of(session.getId()))
				.endKey(ComplexKey.of(session.getId(), ComplexKey.emptyObject())));
		final ViewResult answerSumResult = db.queryView(createQuery("question_value_achieved_for_user")
				.startKey(ComplexKey.of(session.getId()))
				.endKey(ComplexKey.of(session.getId(), ComplexKey.emptyObject())));
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
