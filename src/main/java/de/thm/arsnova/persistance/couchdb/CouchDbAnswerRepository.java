package de.thm.arsnova.persistance.couchdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.AnswerQueueElement;
import de.thm.arsnova.events.NewAnswerEvent;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.persistance.AnswerRepository;
import de.thm.arsnova.persistance.ContentRepository;
import de.thm.arsnova.persistance.LogEntryRepository;
import de.thm.arsnova.persistance.SessionRepository;
import org.ektorp.BulkDeleteDocument;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.DocumentOperationResult;
import org.ektorp.UpdateConflictException;
import org.ektorp.ViewResult;
import org.ektorp.support.CouchDbRepositorySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CouchDbAnswerRepository extends CouchDbRepositorySupport<Answer> implements AnswerRepository, ApplicationEventPublisherAware {
	private static final int BULK_PARTITION_SIZE = 500;
	private static final Logger logger = LoggerFactory.getLogger(CouchDbAnswerRepository.class);

	private final Queue<AnswerQueueElement> answerQueue = new ConcurrentLinkedQueue<>();

	@Autowired
	private LogEntryRepository dbLogger;

	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private ContentRepository contentRepository;

	private ApplicationEventPublisher publisher;

	public CouchDbAnswerRepository(CouchDbConnector db, boolean createIfNotExists) {
		super(Answer.class, db, createIfNotExists);
	}

	@Scheduled(fixedDelay = 5000)
	public void flushAnswerQueue() {
		if (answerQueue.isEmpty()) {
			// no need to send an empty bulk request.
			return;
		}

		final List<Answer> answerList = new ArrayList<>();
		final List<AnswerQueueElement> elements = new ArrayList<>();
		AnswerQueueElement entry;
		while ((entry = this.answerQueue.poll()) != null) {
			final Answer answer = entry.getAnswer();
			answerList.add(answer);
			elements.add(entry);
		}
		try {
			db.executeBulk(answerList);

			// Send NewAnswerEvents ...
			for (AnswerQueueElement e : elements) {
				this.publisher.publishEvent(new NewAnswerEvent(this, e.getSession(), e.getAnswer(), e.getUser(), e.getQuestion()));
			}
		} catch (DbAccessException e) {
			logger.error("Could not bulk save answers from queue.", e);
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@CacheEvict("answers")
	@Override
	public int deleteAnswers(final Content content) {
		try {
			final ViewResult result = db.queryView(createQuery("by_questionid")
					.key(content.getId()));
			final List<List<ViewResult.Row>> partitions = Lists.partition(result.getRows(), BULK_PARTITION_SIZE);

			int count = 0;
			for (List<ViewResult.Row> partition: partitions) {
				List<BulkDeleteDocument> answersToDelete = new ArrayList<>();
				for (final ViewResult.Row a : partition) {
					final BulkDeleteDocument d = new BulkDeleteDocument(a.getId(), a.getValueAsNode().get("_rev").asText());
					answersToDelete.add(d);
				}
				List<DocumentOperationResult> errors = db.executeBulk(answersToDelete);
				count += partition.size() - errors.size();
				if (errors.size() > 0) {
					logger.error("Could not bulk delete {} of {} answers.", errors.size(), partition.size());
				}
			}
			dbLogger.log("delete", "type", "answer", "answerCount", count);

			return count;
		} catch (final DbAccessException e) {
			logger.error("Could not delete answers for content {}.", content.getId(), e);
		}

		return 0;
	}

	@Override
	public Answer getMyAnswer(final User me, final String questionId, final int piRound) {
		final List<Answer> answerList = queryView("by_questionid_user_piround",
				ComplexKey.of(questionId, me.getUsername(), piRound));
		return answerList.isEmpty() ? null : answerList.get(0);
	}

	@Override
	public List<Answer> getAnswers(final Content content, final int piRound) {
		final String questionId = content.getId();
		final ViewResult result = db.queryView(createQuery("by_questionid_piround_text_subject")
						.group(true)
						.startKey(ComplexKey.of(questionId, piRound))
						.endKey(ComplexKey.of(questionId, piRound, ComplexKey.emptyObject())));
		final int abstentionCount = getAbstentionAnswerCount(questionId);

		List<Answer> answers = new ArrayList<>();
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
	public List<Answer> getAllAnswers(final Content content) {
		final String questionId = content.getId();
		final ViewResult result = db.queryView(createQuery("by_questionid_piround_text_subject")
				.group(true)
				.startKey(ComplexKey.of(questionId))
				.endKey(ComplexKey.of(questionId, ComplexKey.emptyObject())));
		final int abstentionCount = getAbstentionAnswerCount(questionId);

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

	@Cacheable("answers")
	@Override
	public List<Answer> getAnswers(final Content content) {
		return this.getAnswers(content, content.getPiRound());
	}

	@Override
	public int getAbstentionAnswerCount(final String questionId) {
		final ViewResult result = db.queryView(createQuery("by_questionid_piround_text_subject")
				//.group(true)
				.startKey(ComplexKey.of(questionId))
				.endKey(ComplexKey.of(questionId, ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int getAnswerCount(final Content content, final int piRound) {
		final ViewResult result = db.queryView(createQuery("by_questionid_piround_text_subject")
				//.group(true)
				.startKey(ComplexKey.of(content.getId(), piRound))
				.endKey(ComplexKey.of(content.getId(), piRound, ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int getTotalAnswerCountByQuestion(final Content content) {
		final ViewResult result = db.queryView(createQuery("by_questionid_piround_text_subject")
				//.group(true)
				.startKey(ComplexKey.of(content.getId()))
				.endKey(ComplexKey.of(content.getId(), ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public List<Answer> getFreetextAnswers(final String questionId, final int start, final int limit) {
		final int qSkip = start > 0 ? start : -1;
		final int qLimit = limit > 0 ? limit : -1;

		final List<Answer> answers = db.queryView(createQuery("by_questionid_timestamp")
						.skip(qSkip)
						.limit(qLimit)
						//.includeDocs(true)
						.startKey(ComplexKey.of(questionId))
						.endKey(ComplexKey.of(questionId, ComplexKey.emptyObject()))
						.descending(true),
				Answer.class);

		return answers;
	}

	@Override
	public List<Answer> getMyAnswers(final User me, final Session s) {
		return queryView("by_user_sessionid", ComplexKey.of(me.getUsername(), s.getId()));
	}

	@Override
	public int getTotalAnswerCount(final String sessionKey) {
		final Session s = sessionRepository.getSessionFromKeyword(sessionKey);
		if (s == null) {
			throw new NotFoundException();
		}
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant").key(s.getId()));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@CacheEvict(value = "answers", key = "#content")
	@Override
	public Answer saveAnswer(final Answer answer, final User user, final Content content, final Session session) {
		db.create(answer);
		this.answerQueue.offer(new AnswerQueueElement(session, content, answer, user));

		return answer;
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public Answer updateAnswer(final Answer answer) {
		try {
			update(answer);
			return answer;
		} catch (final UpdateConflictException e) {
			logger.error("Could not update answer {}.", answer, e);
		}

		return null;
	}

	/* TODO: Only evict cache entry for the answer's session. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public void deleteAnswer(final String answerId) {
		try {
			/* TODO: use id and rev instead of loading the answer */
			db.delete(get(answerId));
			dbLogger.log("delete", "type", "answer");
		} catch (final DbAccessException e) {
			logger.error("Could not delete answer {}.", answerId, e);
		}
	}

	@Override
	public int countLectureQuestionAnswers(final Session session) {
		return countQuestionVariantAnswers(session, "lecture");
	}

	@Override
	public int countPreparationQuestionAnswers(final Session session) {
		return countQuestionVariantAnswers(session, "preparation");
	}

	private int countQuestionVariantAnswers(final Session session, final String variant) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant")
				.key(ComplexKey.of(session.getId(), variant)));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public int deleteAllQuestionsAnswers(final Session session) {
		final List<Content> contents = contentRepository.getQuestions(session.getId());
		contentRepository.resetQuestionsRoundState(session, contents);

		return deleteAllAnswersForQuestions(contents);
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public int deleteAllPreparationAnswers(final Session session) {
		final List<Content> contents = contentRepository.getQuestions(session.getId(), "preparation");
		contentRepository.resetQuestionsRoundState(session, contents);

		return deleteAllAnswersForQuestions(contents);
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public int deleteAllLectureAnswers(final Session session) {
		final List<Content> contents = contentRepository.getQuestions(session.getId(), "lecture");
		contentRepository.resetQuestionsRoundState(session, contents);

		return deleteAllAnswersForQuestions(contents);
	}

	public int deleteAllAnswersForQuestions(List<Content> contents) {
		List<String> questionIds = new ArrayList<>();
		for (Content q : contents) {
			questionIds.add(q.getId());
		}
		final ViewResult result = db.queryView(createQuery("by_questionid")
				.keys(questionIds));
		final List<BulkDeleteDocument> allAnswers = new ArrayList<>();
		for (ViewResult.Row a : result.getRows()) {
			final BulkDeleteDocument d = new BulkDeleteDocument(a.getId(), a.getValueAsNode().get("_rev").asText());
			allAnswers.add(d);
		}
		try {
			List<DocumentOperationResult> errors = db.executeBulk(allAnswers);

			return allAnswers.size() - errors.size();
		} catch (DbAccessException e) {
			logger.error("Could not bulk delete answers.", e);
		}

		return 0;
	}

	public int[] deleteAllAnswersWithQuestions(List<Content> contents) {
		List<String> questionIds = new ArrayList<>();
		final List<BulkDeleteDocument> allQuestions = new ArrayList<>();
		for (Content q : contents) {
			final BulkDeleteDocument d = new BulkDeleteDocument(q.getId(), q.getRevision());
			questionIds.add(q.getId());
			allQuestions.add(d);
		}

		final ViewResult result = db.queryView(createQuery("by_questionid")
				.key(questionIds));
		final List<BulkDeleteDocument> allAnswers = new ArrayList<>();
		for (ViewResult.Row a : result.getRows()) {
			final BulkDeleteDocument d = new BulkDeleteDocument(a.getId(), a.getValueAsNode().get("_rev").asText());
			allAnswers.add(d);
		}

		try {
			List<BulkDeleteDocument> deleteList = new ArrayList<>(allAnswers);
			deleteList.addAll(allQuestions);
			List<DocumentOperationResult> errors = db.executeBulk(deleteList);

			/* TODO: subtract errors from count */
			return new int[] {allQuestions.size(), allAnswers.size()};
		} catch (DbAccessException e) {
			logger.error("Could not bulk delete contents and answers.", e);
		}

		return new int[] {0, 0};
	}
}
