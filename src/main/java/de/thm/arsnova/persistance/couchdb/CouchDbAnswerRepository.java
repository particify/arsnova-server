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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

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

	public CouchDbAnswerRepository(final CouchDbConnector db, final boolean createIfNotExists) {
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
		} catch (final DbAccessException e) {
			logger.error("Could not bulk save answers from queue.", e);
		}
	}

	@Override
	public void setApplicationEventPublisher(final ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@CacheEvict("answers")
	@Override
	public int deleteAnswers(final String contentId) {
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
	public Answer getMyAnswer(final User me, final String contentId, final int piRound) {
		final List<Answer> answerList = queryView("by_questionid_user_piround",
				ComplexKey.of(contentId, me.getUsername(), piRound));
		return answerList.isEmpty() ? null : answerList.get(0);
	}

	@Override
	public List<Answer> getAnswers(final String contentId, final int piRound) {
		final String questionId = contentId;
		final ViewResult result = db.queryView(createQuery("by_questionid_piround_text_subject")
						.group(true)
						.startKey(ComplexKey.of(questionId, piRound))
						.endKey(ComplexKey.of(questionId, piRound, ComplexKey.emptyObject())));
		final int abstentionCount = getAbstentionAnswerCount(questionId);

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
	public List<Answer> getAllAnswers(final String contentId) {
		final ViewResult result = db.queryView(createQuery("by_questionid_piround_text_subject")
				.group(true)
				.startKey(ComplexKey.of(contentId))
				.endKey(ComplexKey.of(contentId, ComplexKey.emptyObject())));
		final int abstentionCount = getAbstentionAnswerCount(contentId);

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
	public int getAbstentionAnswerCount(final String contentId) {
		final ViewResult result = db.queryView(createQuery("by_questionid_piround_text_subject")
				//.group(true)
				.startKey(ComplexKey.of(contentId))
				.endKey(ComplexKey.of(contentId, ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int getAnswerCount(final String contentId, final int round) {
		final ViewResult result = db.queryView(createQuery("by_questionid_piround_text_subject")
				//.group(true)
				.startKey(ComplexKey.of(contentId, round))
				.endKey(ComplexKey.of(contentId, round, ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int getTotalAnswerCountByQuestion(final String contentId) {
		final ViewResult result = db.queryView(createQuery("by_questionid_piround_text_subject")
				//.group(true)
				.startKey(ComplexKey.of(contentId))
				.endKey(ComplexKey.of(contentId, ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public List<Answer> getFreetextAnswers(final String contentId, final int start, final int limit) {
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
	public List<Answer> getMyAnswers(final User user, final String sessionId) {
		return queryView("by_user_sessionid", ComplexKey.of(user.getUsername(), sessionId));
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
	public int countLectureQuestionAnswers(final String sessionId) {
		return countQuestionVariantAnswers(sessionId, "lecture");
	}

	@Override
	public int countPreparationQuestionAnswers(final String sessionId) {
		return countQuestionVariantAnswers(sessionId, "preparation");
	}

	private int countQuestionVariantAnswers(final String sessionId, final String variant) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant")
				.key(ComplexKey.of(sessionId, variant)));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public int deleteAllQuestionsAnswers(final String sessionId) {
		final List<Content> contents = contentRepository.getQuestions(sessionId);
		contentRepository.resetQuestionsRoundState(sessionId, contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());

		return deleteAllAnswersForQuestions(contentIds);
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public int deleteAllPreparationAnswers(final String sessionId) {
		final List<Content> contents = contentRepository.getQuestions(sessionId, "preparation");
		contentRepository.resetQuestionsRoundState(sessionId, contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());

		return deleteAllAnswersForQuestions(contentIds);
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public int deleteAllLectureAnswers(final String sessionId) {
		final List<Content> contents = contentRepository.getQuestions(sessionId, "lecture");
		contentRepository.resetQuestionsRoundState(sessionId, contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());

		return deleteAllAnswersForQuestions(contentIds);
	}

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

	/* TODO: Split up - the combined action should be handled on the service level. */
	public int[] deleteAllAnswersWithQuestions(final List<Content> contents) {
		List<String> questionIds = new ArrayList<>();
		final List<BulkDeleteDocument> allQuestions = new ArrayList<>();
		for (final Content q : contents) {
			final BulkDeleteDocument d = new BulkDeleteDocument(q.getId(), q.getRevision());
			questionIds.add(q.getId());
			allQuestions.add(d);
		}

		final ViewResult result = db.queryView(createQuery("by_questionid")
				.key(questionIds));
		final List<BulkDeleteDocument> allAnswers = new ArrayList<>();
		for (final ViewResult.Row a : result.getRows()) {
			final BulkDeleteDocument d = new BulkDeleteDocument(a.getId(), a.getValueAsNode().get("_rev").asText());
			allAnswers.add(d);
		}

		try {
			final List<BulkDeleteDocument> deleteList = new ArrayList<>(allAnswers);
			deleteList.addAll(allQuestions);
			final List<DocumentOperationResult> errors = db.executeBulk(deleteList);

			/* TODO: subtract errors from count */
			return new int[] {allQuestions.size(), allAnswers.size()};
		} catch (final DbAccessException e) {
			logger.error("Could not bulk delete contents and answers.", e);
		}

		return new int[] {0, 0};
	}
}
