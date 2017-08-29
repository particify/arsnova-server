package de.thm.arsnova.persistance.couchdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import de.thm.arsnova.entities.migration.v2.Answer;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.persistance.AnswerRepository;
import de.thm.arsnova.persistance.LogEntryRepository;
import org.ektorp.BulkDeleteDocument;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.DocumentOperationResult;
import org.ektorp.ViewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import java.util.ArrayList;
import java.util.List;

public class CouchDbAnswerRepository extends CouchDbCrudRepository<Answer> implements AnswerRepository, ApplicationEventPublisherAware {
	private static final int BULK_PARTITION_SIZE = 500;
	private static final Logger logger = LoggerFactory.getLogger(CouchDbAnswerRepository.class);

	@Autowired
	private LogEntryRepository dbLogger;

	private ApplicationEventPublisher publisher;

	public CouchDbAnswerRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Answer.class, db, "by_sessionid", createIfNotExists);
	}

	@Override
	public void setApplicationEventPublisher(final ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public int deleteByContentId(final String contentId) {
		try {
			final ViewResult result = db.queryView(createQuery("by_questionid")
					.key(contentId));
			final List<List<ViewResult.Row>> partitions = Lists.partition(result.getRows(), BULK_PARTITION_SIZE);

			int count = 0;
			for (final List<ViewResult.Row> partition: partitions) {
				final List<BulkDeleteDocument> answersToDelete = new ArrayList<>();
				for (final ViewResult.Row a : partition) {
					final BulkDeleteDocument d = new BulkDeleteDocument(a.getId(), a.getValueAsNode().get("_rev").asText());
					answersToDelete.add(d);
				}
				final List<DocumentOperationResult> errors = db.executeBulk(answersToDelete);
				count += partition.size() - errors.size();
				if (errors.size() > 0) {
					logger.error("Could not bulk delete {} of {} answers.", errors.size(), partition.size());
				}
			}
			dbLogger.log("delete", "type", "answer", "answerCount", count);

			return count;
		} catch (final DbAccessException e) {
			logger.error("Could not delete answers for content {}.", contentId, e);
		}

		return 0;
	}

	@Override
	public Answer findByQuestionIdUserPiRound(final String contentId, final User user, final int piRound) {
		final List<Answer> answerList = queryView("by_questionid_user_piround",
				ComplexKey.of(contentId, user.getUsername(), piRound));
		return answerList.isEmpty() ? null : answerList.get(0);
	}

	@Override
	public List<Answer> findByContentIdPiRound(final String contentId, final int piRound) {
		final String questionId = contentId;
		final ViewResult result = db.queryView(createQuery("by_questionid_piround_text_subject")
						.group(true)
						.startKey(ComplexKey.of(questionId, piRound))
						.endKey(ComplexKey.of(questionId, piRound, ComplexKey.emptyObject())));
		final int abstentionCount = countByContentId(questionId);

		final List<Answer> answers = new ArrayList<>();
		for (final ViewResult.Row d : result) {
			final Answer a = new Answer();
			a.setAnswerCount(d.getValueAsInt());
			a.setAbstentionCount(abstentionCount);
			a.setQuestionId(d.getKeyAsNode().get(0).asText());
			a.setPiRound(piRound);
			final JsonNode answerTextNode = d.getKeyAsNode().get(3);
			a.setAnswerText(answerTextNode.isNull() ? null : answerTextNode.asText());
			answers.add(a);
		}

		return answers;
	}

	@Override
	public List<Answer> findByContentId(final String contentId) {
		final ViewResult result = db.queryView(createQuery("by_questionid_piround_text_subject")
				.group(true)
				.startKey(ComplexKey.of(contentId))
				.endKey(ComplexKey.of(contentId, ComplexKey.emptyObject())));
		final int abstentionCount = countByContentId(contentId);

		final List<Answer> answers = new ArrayList<>();
		for (final ViewResult.Row d : result.getRows()) {
			final Answer a = new Answer();
			a.setAnswerCount(d.getValueAsInt());
			a.setAbstentionCount(abstentionCount);
			a.setQuestionId(d.getKeyAsNode().get(0).asText());
			final JsonNode answerTextNode = d.getKeyAsNode().get(3);
			final JsonNode answerSubjectNode = d.getKeyAsNode().get(4);
			final boolean successfulFreeTextAnswer = d.getKeyAsNode().get(5).asBoolean();
			a.setAnswerText(answerTextNode.isNull() ? null : answerTextNode.asText());
			a.setAnswerSubject(answerSubjectNode.isNull() ? null : answerSubjectNode.asText());
			a.setSuccessfulFreeTextAnswer(successfulFreeTextAnswer);
			answers.add(a);
		}

		return answers;
	}

	@Override
	public int countByContentId(final String contentId) {
		final ViewResult result = db.queryView(createQuery("by_questionid_piround_text_subject")
				.reduce(true)
				.startKey(ComplexKey.of(contentId))
				.endKey(ComplexKey.of(contentId, ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int countByContentIdRound(final String contentId, final int round) {
		final ViewResult result = db.queryView(createQuery("by_questionid_piround_text_subject")
				.reduce(true)
				.startKey(ComplexKey.of(contentId, round))
				.endKey(ComplexKey.of(contentId, round, ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public List<Answer> findByContentId(final String contentId, final int start, final int limit) {
		final int qSkip = start > 0 ? start : -1;
		final int qLimit = limit > 0 ? limit : -1;

		final List<Answer> answers = db.queryView(createQuery("by_questionid_timestamp")
						.skip(qSkip)
						.limit(qLimit)
						//.includeDocs(true)
						.startKey(ComplexKey.of(contentId))
						.endKey(ComplexKey.of(contentId, ComplexKey.emptyObject()))
						.descending(true),
				Answer.class);

		return answers;
	}

	@Override
	public List<Answer> findByUserSessionId(final User user, final String sessionId) {
		return queryView("by_user_sessionid", ComplexKey.of(user.getUsername(), sessionId));
	}

	@Override
	public int countBySessionKey(final String sessionKey) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant").key(sessionKey));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int countBySessionIdLectureVariant(final String sessionId) {
		return countBySessionIdVariant(sessionId, "lecture");
	}

	@Override
	public int countBySessionIdPreparationVariant(final String sessionId) {
		return countBySessionIdVariant(sessionId, "preparation");
	}

	private int countBySessionIdVariant(final String sessionId, final String variant) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant")
				.key(ComplexKey.of(sessionId, variant)));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int deleteAllAnswersForQuestions(final List<String> contentIds) {
		final ViewResult result = db.queryView(createQuery("by_questionid")
				.keys(contentIds));
		final List<BulkDeleteDocument> allAnswers = new ArrayList<>();
		for (final ViewResult.Row a : result.getRows()) {
			final BulkDeleteDocument d = new BulkDeleteDocument(a.getId(), a.getValueAsNode().get("_rev").asText());
			allAnswers.add(d);
		}
		try {
			final List<DocumentOperationResult> errors = db.executeBulk(allAnswers);

			return allAnswers.size() - errors.size();
		} catch (final DbAccessException e) {
			logger.error("Could not bulk delete answers.", e);
		}

		return 0;
	}

	@Override
	public int deleteByContentIds(final List<String> contentIds) {
		final ViewResult result = db.queryView(createQuery("by_questionid")
				.keys(contentIds));
		final List<BulkDeleteDocument> deleteDocs = new ArrayList<>();
		for (final ViewResult.Row a : result.getRows()) {
			final BulkDeleteDocument d = new BulkDeleteDocument(a.getId(), a.getValueAsNode().get("_rev").asText());
			deleteDocs.add(d);
		}

		try {
			final List<DocumentOperationResult> errors = db.executeBulk(deleteDocs);

			return deleteDocs.size() - errors.size();
		} catch (final DbAccessException e) {
			logger.error("Could not bulk delete answers.", e);
		}

		return 0;
	}
}
