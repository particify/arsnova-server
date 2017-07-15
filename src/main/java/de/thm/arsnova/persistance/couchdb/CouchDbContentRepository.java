package de.thm.arsnova.persistance.couchdb;

import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.persistance.AnswerRepository;
import de.thm.arsnova.persistance.ContentRepository;
import de.thm.arsnova.persistance.LogEntryRepository;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.support.CouchDbRepositorySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CouchDbContentRepository extends CouchDbRepositorySupport<Content> implements ContentRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbContentRepository.class);

	@Autowired
	private LogEntryRepository dbLogger;

	@Autowired
	private AnswerRepository answerRepository;

	public CouchDbContentRepository(Class<Content> type, CouchDbConnector db, boolean createIfNotExists) {
		super(type, db, createIfNotExists);
	}

	@Cacheable("skillquestions")
	@Override
	public List<Content> getSkillQuestionsForUsers(final Session session) {
		final List<Content> contents = new ArrayList<>();
		final List<Content> questions1 = getQuestions(session.getId(), "lecture", true);
		final List<Content> questions2 = getQuestions(session.getId(), "preparation", true);
		final List<Content> questions3 = getQuestions(session.getId(), "flashcard", true);
		contents.addAll(questions1);
		contents.addAll(questions2);
		contents.addAll(questions3);

		return contents;
	}

	@Cacheable("skillquestions")
	@Override
	public List<Content> getSkillQuestionsForTeachers(final Session session) {
		return getQuestions(new Object[] {session.getId()}, session);
	}

	@Override
	public int getSkillQuestionCount(final Session session) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(session.getId()))
				.endKey(ComplexKey.of(session.getId(), ComplexKey.emptyObject())));

		return result.getSize();
	}

	@Caching(evict = {@CacheEvict(value = "skillquestions", key = "#session"),
			@CacheEvict(value = "lecturequestions", key = "#session", condition = "#content.getQuestionVariant().equals('lecture')"),
			@CacheEvict(value = "preparationquestions", key = "#session", condition = "#content.getQuestionVariant().equals('preparation')"),
			@CacheEvict(value = "flashcardquestions", key = "#session", condition = "#content.getQuestionVariant().equals('flashcard')") },
			put = {@CachePut(value = "questions", key = "#content.id")})
	@Override
	public Content saveQuestion(final Session session, final Content content) {
		content.setSessionId(session.getId());
		try {
			db.create(content);

			return content;
		} catch (final IllegalArgumentException e) {
			logger.error("Could not save content {}.", content, e);
		}

		return null;
	}

	/* TODO: Only evict cache entry for the content's session. This requires some refactoring. */
	@Caching(evict = {@CacheEvict(value = "skillquestions", allEntries = true),
			@CacheEvict(value = "lecturequestions", allEntries = true, condition = "#content.getQuestionVariant().equals('lecture')"),
			@CacheEvict(value = "preparationquestions", allEntries = true, condition = "#content.getQuestionVariant().equals('preparation')"),
			@CacheEvict(value = "flashcardquestions", allEntries = true, condition = "#content.getQuestionVariant().equals('flashcard')") },
			put = {@CachePut(value = "questions", key = "#content.id")})
	@Override
	public Content updateQuestion(final Content content) {
		try {
			/* TODO: Make sure that sessionId is valid before so the content does not need to be retrieved. */
			final Content oldContent = get(content.getId());
			content.setId(oldContent.getId());
			content.setRevision(oldContent.getRevision());
			content.updateRoundManagementState();
			update(content);

			return content;
		} catch (final UpdateConflictException e) {
			logger.error("Could not update content {}.", content, e);
		}

		return null;
	}

	@Cacheable("questions")
	@Override
	public Content getQuestion(final String id) {
		try {
			final Content content = get(id);
			content.updateRoundManagementState();
			//content.setSessionKeyword(sessionRepository.getSessionFromId(content.getSessionId()).getKeyword());

			return content;
		} catch (final DocumentNotFoundException e) {
			logger.error("Could not get question {}.", id, e);
		}

		return null;
	}

	@Override
	public List<String> getQuestionIds(final Session session, final User user) {
		return collectQuestionIds(db.queryView(createQuery("by_sessionid_variant_active").key(session.getId())));
	}

	/* TODO: Only evict cache entry for the content's session. This requires some refactoring. */
	@Caching(evict = { @CacheEvict(value = "questions", key = "#content.id"),
			@CacheEvict(value = "skillquestions", allEntries = true),
			@CacheEvict(value = "lecturequestions", allEntries = true, condition = "#content.getQuestionVariant().equals('lecture')"),
			@CacheEvict(value = "preparationquestions", allEntries = true, condition = "#content.getQuestionVariant().equals('preparation')"),
			@CacheEvict(value = "flashcardquestions", allEntries = true, condition = "#content.getQuestionVariant().equals('flashcard')") })
	@Override
	public int deleteQuestionWithAnswers(final Content content) {
		try {
			int count = answerRepository.deleteAnswers(content);
			db.delete(content);
			dbLogger.log("delete", "type", "content", "answerCount", count);

			return count;
		} catch (final IllegalArgumentException e) {
			logger.error("Could not delete content {}.", content.getId(), e);
		}

		return 0;
	}

	@Caching(evict = { @CacheEvict(value = "questions", allEntries = true),
			@CacheEvict(value = "skillquestions", key = "#session"),
			@CacheEvict(value = "lecturequestions", key = "#session"),
			@CacheEvict(value = "preparationquestions", key = "#session"),
			@CacheEvict(value = "flashcardquestions", key = "#session") })
	@Override
	public int[] deleteAllQuestionsWithAnswers(final Session session) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(session.getId()))
				.endKey(ComplexKey.of(session.getId(), ComplexKey.emptyObject()))
				.reduce(false));

		return deleteAllQuestionDocumentsWithAnswers(result);
	}

	private int[] deleteAllQuestionDocumentsWithAnswers(final ViewResult viewResult) {
		List<Content> contents = new ArrayList<>();
		for (final ViewResult.Row row : viewResult.getRows()) {
			final Content q = new Content();
			q.setId(row.getId());
			q.setRevision(row.getValueAsNode().get("_rev").asText());
			contents.add(q);
		}

		int[] count = answerRepository.deleteAllAnswersWithQuestions(contents);
		dbLogger.log("delete", "type", "question", "questionCount", count[0]);
		dbLogger.log("delete", "type", "answer", "answerCount", count[1]);

		return count;
	}

	@Override
	public List<String> getUnAnsweredQuestionIds(final Session session, final User user) {
		final ViewResult result = db.queryView(createQuery("questionid_by_user_sessionid_variant")
				.designDocId("_design/Answer")
				.startKey(ComplexKey.of(user.getUsername(), session.getId()))
				.endKey(ComplexKey.of(user.getUsername(), session.getId(), ComplexKey.emptyObject())));
		List<String> answeredIds = new ArrayList<>();
		for (ViewResult.Row row : result.getRows()) {
			answeredIds.add(row.getId());
		}
		return collectUnansweredQuestionIds(getQuestionIds(session, user), answeredIds);
	}

	@Override
	public List<String> getUnAnsweredLectureQuestionIds(final Session session, final User user) {
		final ViewResult result = db.queryView(createQuery("questionid_piround_by_user_sessionid_variant")
				.designDocId("_design/Answer")
				.key(ComplexKey.of(user.getUsername(), session.getId(), "lecture")));
		Map<String, Integer> answeredQuestions = new HashMap<>();
		for (ViewResult.Row row : result.getRows()) {
			answeredQuestions.put(row.getId(), row.getKeyAsNode().get(2).asInt());
		}

		return collectUnansweredQuestionIdsByPiRound(getLectureQuestionsForUsers(session), answeredQuestions);
	}

	@Override
	public List<String> getUnAnsweredPreparationQuestionIds(final Session session, final User user) {
		final ViewResult result = db.queryView(createQuery("questionid_piround_by_user_sessionid_variant")
				.designDocId("_design/Answer")
				.key(ComplexKey.of(user.getUsername(), session.getId(), "preparation")));
		Map<String, Integer> answeredQuestions = new HashMap<>();
		for (ViewResult.Row row : result.getRows()) {
			answeredQuestions.put(row.getId(), row.getKeyAsNode().get(2).asInt());
		}

		return collectUnansweredQuestionIdsByPiRound(getPreparationQuestionsForUsers(session), answeredQuestions);
	}

	@Cacheable("lecturequestions")
	@Override
	public List<Content> getLectureQuestionsForUsers(final Session session) {
		return getQuestions(session.getId(), "lecture", true);
	}

	@Override
	public List<Content> getLectureQuestionsForTeachers(final Session session) {
		return getQuestions(session.getId(), "lecture");
	}

	@Cacheable("flashcardquestions")
	@Override
	public List<Content> getFlashcardsForUsers(final Session session) {
		return getQuestions(session.getId(), "flashcard", true);
	}

	@Override
	public List<Content> getFlashcardsForTeachers(final Session session) {
		return getQuestions(session.getId(), "flashcard");
	}

	@Cacheable("preparationquestions")
	@Override
	public List<Content> getPreparationQuestionsForUsers(final Session session) {
		return getQuestions(session.getId(), "preparation", true);
	}

	@Override
	public List<Content> getPreparationQuestionsForTeachers(final Session session) {
		return getQuestions(session.getId(), "preparation");
	}

	@Override
	public List<Content> getAllSkillQuestions(final Session session) {
		return getQuestions(session.getId());
	}

	@Override
	public List<Content> getQuestions(final Object... keys) {
		Object[] endKeys = Arrays.copyOf(keys, keys.length + 1);
		endKeys[keys.length] = ComplexKey.emptyObject();
		final List<Content> contents = db.queryView(createQuery("by_sessionid_variant_active")
						.includeDocs(true)
						.reduce(false)
						.startKey(ComplexKey.of(keys))
						.endKey(ComplexKey.of(endKeys)),
				Content.class);
		for (Content content : contents) {
			content.updateRoundManagementState();
			//content.setSessionKeyword(session.getKeyword());
		}

		return contents;
	}

	@Override
	public int getLectureQuestionCount(final Session session) {
		/* TODO: reduce code duplication */
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(session.getId(), "lecture"))
				.endKey(ComplexKey.of(session.getId(), "lecture", ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int getFlashcardCount(final Session session) {
		/* TODO: reduce code duplication */
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(session.getId(), "flashcard"))
				.endKey(ComplexKey.of(session.getId(), "flashcard", ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int getPreparationQuestionCount(final Session session) {
		/* TODO: reduce code duplication */
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(session.getId(), "preparation"))
				.endKey(ComplexKey.of(session.getId(), "preparation", ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@Caching(evict = { @CacheEvict(value = "questions", allEntries = true),
			@CacheEvict("skillquestions"),
			@CacheEvict("lecturequestions"),
			@CacheEvict(value = "answers", allEntries = true)})
	@Override
	public int[] deleteAllLectureQuestionsWithAnswers(final Session session) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(session.getId(), "lecture"))
				.endKey(ComplexKey.of(session.getId(), "lecture", ComplexKey.emptyObject()))
				.reduce(false));

		return deleteAllQuestionDocumentsWithAnswers(result);
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@Caching(evict = { @CacheEvict(value = "questions", allEntries = true),
			@CacheEvict("skillquestions"),
			@CacheEvict("flashcardquestions"),
			@CacheEvict(value = "answers", allEntries = true)})
	@Override
	public int[] deleteAllFlashcardsWithAnswers(final Session session) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(session.getId(), "flashcard"))
				.endKey(ComplexKey.of(session.getId(), "flashcard", ComplexKey.emptyObject()))
				.reduce(false));

		return deleteAllQuestionDocumentsWithAnswers(result);
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@Caching(evict = { @CacheEvict(value = "questions", allEntries = true),
			@CacheEvict("skillquestions"),
			@CacheEvict("preparationquestions"),
			@CacheEvict(value = "answers", allEntries = true)})
	@Override
	public int[] deleteAllPreparationQuestionsWithAnswers(final Session session) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(session.getId(), "preparation"))
				.endKey(ComplexKey.of(session.getId(), "preparation", ComplexKey.emptyObject()))
				.reduce(false));

		return deleteAllQuestionDocumentsWithAnswers(result);
	}

	private List<String> collectUnansweredQuestionIds(
			final List<String> questions,
			final List<String> answeredQuestions
	) {
		final List<String> unanswered = new ArrayList<>();
		for (final String questionId : questions) {
			if (!answeredQuestions.contains(questionId)) {
				unanswered.add(questionId);
			}
		}
		return unanswered;
	}

	private List<String> collectUnansweredQuestionIdsByPiRound(
			final List<Content> contents,
			final Map<String, Integer> answeredQuestions
	) {
		final List<String> unanswered = new ArrayList<>();

		for (final Content content : contents) {
			if (!"slide".equals(content.getQuestionType()) && (!answeredQuestions.containsKey(content.getId())
					|| (answeredQuestions.containsKey(content.getId()) && answeredQuestions.get(content.getId()) != content.getPiRound()))) {
				unanswered.add(content.getId());
			}
		}

		return unanswered;
	}

	private List<String> collectQuestionIds(final ViewResult viewResult) {
		final List<String> ids = new ArrayList<>();
		for (final ViewResult.Row row : viewResult.getRows()) {
			ids.add(row.getId());
		}
		return ids;
	}

	@Override
	public List<Content> publishAllQuestions(final Session session, final boolean publish) {
		final List<Content> contents = db.queryView(createQuery("by_sessionid_variant_active")
						.startKey(ComplexKey.of(session.getId()))
						.endKey(ComplexKey.of(session.getId(), ComplexKey.emptyObject())),
				Content.class);
		/* FIXME: caching */
		publishQuestions(session, publish, contents);

		return contents;
	}

	@Caching(evict = { @CacheEvict(value = "contents", allEntries = true),
			@CacheEvict(value = "skillquestions", key = "#session"),
			@CacheEvict(value = "lecturequestions", key = "#session"),
			@CacheEvict(value = "preparationquestions", key = "#session"),
			@CacheEvict(value = "flashcardquestions", key = "#session") })
	@Override
	public void publishQuestions(final Session session, final boolean publish, List<Content> contents) {
		for (final Content content : contents) {
			content.setActive(publish);
		}
		try {
			db.executeBulk(contents);
		} catch (final DbAccessException e) {
			logger.error("Could not bulk publish all contents.", e);
		}
	}

	@Override
	public List<Content> setVotingAdmissionForAllQuestions(final Session session, final boolean disableVoting) {
		final List<Content> contents = db.queryView(createQuery("by_sessionid_variant_active")
						.startKey(ComplexKey.of(session.getId()))
						.endKey(ComplexKey.of(session.getId(), ComplexKey.emptyObject()))
						.includeDocs(true),
				Content.class);
		/* FIXME: caching */
		setVotingAdmissions(session, disableVoting, contents);

		return contents;
	}

	@Caching(evict = { @CacheEvict(value = "contents", allEntries = true),
			@CacheEvict(value = "skillquestions", key = "#session"),
			@CacheEvict(value = "lecturequestions", key = "#session"),
			@CacheEvict(value = "preparationquestions", key = "#session"),
			@CacheEvict(value = "flashcardquestions", key = "#session") })
	@Override
	public void setVotingAdmissions(final Session session, final boolean disableVoting, List<Content> contents) {
		for (final Content q : contents) {
			if (!"flashcard".equals(q.getQuestionType())) {
				q.setVotingDisabled(disableVoting);
			}
		}

		try {
			db.executeBulk(contents);
		} catch (final DbAccessException e) {
			logger.error("Could not bulk set voting admission for all contents.", e);
		}
	}

	/* TODO: remove if this method is no longer used */
	@Override
	public List<String> getQuestionIdsBySubject(Session session, String questionVariant, String subject) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(session.getId(), questionVariant, 1, subject))
				.endKey(ComplexKey.of(session.getId(), questionVariant, 1, subject, ComplexKey.emptyObject())));

		List<String> qids = new ArrayList<>();

		for (final ViewResult.Row row : result.getRows()) {
			final String s = row.getId();
			qids.add(s);
		}

		return qids;
	}

	@Override
	public List<Content> getQuestionsByIds(List<String> ids, final Session session) {
		return db.queryView(new ViewQuery().allDocs().keys(ids).includeDocs(true), Content.class);
	}

	@Override
	public List<String> getSubjects(Session session, String questionVariant) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(session.getId(), questionVariant))
				.endKey(ComplexKey.of(session.getId(), questionVariant, ComplexKey.emptyObject())));

		Set<String> uniqueSubjects = new HashSet<>();

		for (final ViewResult.Row row : result.getRows()) {
			uniqueSubjects.add(row.getKeyAsNode().get(3).asText());
		}

		return new ArrayList<>(uniqueSubjects);
	}

	@Caching(evict = { @CacheEvict(value = "contents", allEntries = true),
			@CacheEvict(value = "skillquestions", key = "#session"),
			@CacheEvict(value = "lecturequestions", key = "#session"),
			@CacheEvict(value = "preparationquestions", key = "#session"),
			@CacheEvict(value = "flashcardquestions", key = "#session") })
	@Override
	public void resetQuestionsRoundState(final Session session, List<Content> contents) {
		for (final Content q : contents) {
			q.setSessionId(session.getId());
			q.resetQuestionState();
		}
		try {
			db.executeBulk(contents);
		} catch (final DbAccessException e) {
			logger.error("Could not bulk reset all contents round state.", e);
		}
	}
}
