/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.	 If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.dao;

import com.fourspaces.couchdb.Database;
import com.fourspaces.couchdb.Document;
import com.fourspaces.couchdb.Results;
import com.fourspaces.couchdb.RowResult;
import com.fourspaces.couchdb.View;
import com.fourspaces.couchdb.ViewResults;
import com.google.common.collect.Lists;
import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.domain.CourseScore;
import de.thm.arsnova.entities.*;
import de.thm.arsnova.entities.transport.AnswerQueueElement;
import de.thm.arsnova.events.NewAnswerEvent;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.persistance.LogEntryRepository;
import de.thm.arsnova.persistance.MotdRepository;
import de.thm.arsnova.persistance.SessionRepository;
import de.thm.arsnova.services.ISessionService;
import net.sf.ezmorph.Morpher;
import net.sf.ezmorph.MorpherRegistry;
import net.sf.ezmorph.bean.BeanMorpher;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Database implementation based on CouchDB.
 *
 * Note to developers:
 *
 * This class makes use of Spring Framework's caching annotations. When you are about to add new functionality,
 * you should also think about the possibility of caching. Ideally, your methods should be dependent on domain
 * objects like Session or Question, which can be used as cache keys. Relying on plain String objects as a key, e.g.
 * by passing only a Session's keyword, will make your cache annotations less readable. You will also need to think
 * about cases where your cache needs to be updated and evicted.
 *
 * In order to use cached methods from within this object, you have to use the getDatabaseDao() method instead of
 * using the "this" pointer. This is because caching is only available if the method is called through a Spring Proxy,
 * which is not the case when using "this".
 *
 * @see <a href="http://docs.spring.io/spring/docs/current/spring-framework-reference/html/cache.html">Spring Framework's Cache Abstraction</a>
 * @see <a href="https://github.com/thm-projects/arsnova-backend/wiki/Caching">Caching in ARSnova explained</a>
 */
@Profile("!test")
@Service("databaseDao")
public class CouchDBDao implements IDatabaseDao, ApplicationEventPublisherAware {

	private static final int BULK_PARTITION_SIZE = 500;

	@Autowired
	private ISessionService sessionService;

	@Autowired
	private LogEntryRepository dbLogger;

	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private MotdRepository motdRepository;

	private String databaseHost;
	private int databasePort;
	private String databaseName;
	private Database database;

	private ApplicationEventPublisher publisher;

	private final Queue<AbstractMap.SimpleEntry<Document, AnswerQueueElement>> answerQueue = new ConcurrentLinkedQueue<>();

	private static final Logger logger = LoggerFactory.getLogger(CouchDBDao.class);

	@Value("${couchdb.host}")
	public void setDatabaseHost(final String newDatabaseHost) {
		databaseHost = newDatabaseHost;
	}

	@Value("${couchdb.port}")
	public void setDatabasePort(final String newDatabasePort) {
		databasePort = Integer.parseInt(newDatabasePort);
	}

	@Value("${couchdb.name}")
	public void setDatabaseName(final String newDatabaseName) {
		databaseName = newDatabaseName;
	}

	public void setSessionService(final ISessionService service) {
		sessionService = service;
	}

	/**
	 * <strike>
	 * Allows access to the proxy object. It has to be used instead of <code>this</code> for local calls to public
	 * methods for caching purposes. This is an ugly but necessary temporary workaround until a better solution is
	 * implemented (e.g. use of AspectJ's weaving).
	 * @return the proxy for CouchDBDao
	 * </strike>
	 */
	private @NonNull IDatabaseDao getDatabaseDao() {
		return this;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Cacheable("skillquestions")
	@Override
	public List<Question> getSkillQuestionsForUsers(final Session session) {
		final List<Question> questions = new ArrayList<>();
		final String viewName = "content/doc_by_sessionid_variant_active";
		final View view1 = new View(viewName);
		final View view2 = new View(viewName);
		final View view3 = new View(viewName);
		view1.setStartKey(session.getId(), "lecture", true);
		view1.setEndKey(session.getId(), "lecture", true, "{}");
		view2.setStartKey(session.getId(), "preparation", true);
		view2.setEndKey(session.getId(), "preparation", true, "{}");
		view3.setStartKey(session.getId(), "flashcard", true);
		view3.setEndKey(session.getId(), "flashcard", true, "{}");
		questions.addAll(getQuestions(view1, session));
		questions.addAll(getQuestions(view2, session));
		questions.addAll(getQuestions(view3, session));

		return questions;
	}

	@Cacheable("skillquestions")
	@Override
	public List<Question> getSkillQuestionsForTeachers(final Session session) {
		final View view = new View("content/doc_by_sessionid_variant_active");
		view.setStartKey(session.getId());
		view.setEndKey(session.getId(), "{}");

		return getQuestions(view, session);
	}

	@Override
	public int getSkillQuestionCount(final Session session) {
		final View view = new View("content/by_sessionid_variant_active");
		view.setStartKey(session.getId());
		view.setEndKey(session.getId(), "{}");

		return getQuestionCount(view);
	}

	private Database getDatabase() {
		if (database == null) {
			try {
				final com.fourspaces.couchdb.Session session = new com.fourspaces.couchdb.Session(
						databaseHost,
						databasePort
						);
				database = session.getDatabase(databaseName);
			} catch (final Exception e) {
				logger.error("Cannot connect to CouchDB database '{}' on host '{}' using port {}.",
						databaseName, databaseHost, databasePort, e);
			}
		}

		return database;
	}

	@Caching(evict = {@CacheEvict(value = "skillquestions", key = "#session"),
			@CacheEvict(value = "lecturequestions", key = "#session", condition = "#question.getQuestionVariant().equals('lecture')"),
			@CacheEvict(value = "preparationquestions", key = "#session", condition = "#question.getQuestionVariant().equals('preparation')"),
			@CacheEvict(value = "flashcardquestions", key = "#session", condition = "#question.getQuestionVariant().equals('flashcard')") },
			put = {@CachePut(value = "questions", key = "#question._id")})
	@Override
	public Question saveQuestion(final Session session, final Question question) {
		final Document q = toQuestionDocument(session, question);
		try {
			database.saveDocument(q);
			question.set_id(q.getId());
			question.set_rev(q.getRev());
			return question;
		} catch (final IOException e) {
			logger.error("Could not save question {}.", question, e);
		}
		return null;
	}

	private Document toQuestionDocument(final Session session, final Question question) {
		Document q = new Document();

		question.updateRoundManagementState();
		q.put("type", "skill_question");
		q.put("questionType", question.getQuestionType());
		q.put("ignoreCaseSensitive", question.isIgnoreCaseSensitive());
		q.put("ignoreWhitespaces", question.isIgnoreWhitespaces());
		q.put("ignorePunctuation", question.isIgnorePunctuation());
		q.put("fixedAnswer", question.isFixedAnswer());
		q.put("strictMode", question.isStrictMode());
		q.put("rating", question.getRating());
		q.put("correctAnswer", question.getCorrectAnswer());
		q.put("questionVariant", question.getQuestionVariant());
		q.put("sessionId", session.getId());
		q.put("subject", question.getSubject());
		q.put("text", question.getText());
		q.put("active", question.isActive());
		q.put("votingDisabled", question.isVotingDisabled());
		q.put("number", 0); // TODO: This number is now unused. A clean up is necessary.
		q.put("releasedFor", question.getReleasedFor());
		q.put("possibleAnswers", question.getPossibleAnswers());
		q.put("noCorrect", question.isNoCorrect());
		q.put("piRound", question.getPiRound());
		q.put("piRoundStartTime", question.getPiRoundStartTime());
		q.put("piRoundEndTime", question.getPiRoundEndTime());
		q.put("piRoundFinished", question.isPiRoundFinished());
		q.put("piRoundActive", question.isPiRoundActive());
		q.put("showStatistic", question.isShowStatistic());
		q.put("showAnswer", question.isShowAnswer());
		q.put("abstention", question.isAbstention());
		q.put("image", question.getImage());
		q.put("fcImage", question.getFcImage());
		q.put("gridSize", question.getGridSize());
		q.put("offsetX", question.getOffsetX());
		q.put("offsetY", question.getOffsetY());
		q.put("zoomLvl", question.getZoomLvl());
		q.put("gridOffsetX", question.getGridOffsetX());
		q.put("gridOffsetY", question.getGridOffsetY());
		q.put("gridZoomLvl", question.getGridZoomLvl());
		q.put("gridSizeX", question.getGridSizeX());
		q.put("gridSizeY", question.getGridSizeY());
		q.put("gridIsHidden", question.getGridIsHidden());
		q.put("imgRotation", question.getImgRotation());
		q.put("toggleFieldsLeft", question.getToggleFieldsLeft());
		q.put("numClickableFields", question.getNumClickableFields());
		q.put("thresholdCorrectAnswers", question.getThresholdCorrectAnswers());
		q.put("cvIsColored", question.getCvIsColored());
		q.put("gridLineColor", question.getGridLineColor());
		q.put("numberOfDots", question.getNumberOfDots());
		q.put("gridType", question.getGridType());
		q.put("scaleFactor", question.getScaleFactor());
		q.put("gridScaleFactor", question.getGridScaleFactor());
		q.put("imageQuestion", question.isImageQuestion());
		q.put("textAnswerEnabled", question.isTextAnswerEnabled());
		q.put("timestamp", question.getTimestamp());
		q.put("hint", question.getHint());
		q.put("solution", question.getSolution());
		return q;
	}

	/* TODO: Only evict cache entry for the question's session. This requires some refactoring. */
	@Caching(evict = {@CacheEvict(value = "skillquestions", allEntries = true),
			@CacheEvict(value = "lecturequestions", allEntries = true, condition = "#question.getQuestionVariant().equals('lecture')"),
			@CacheEvict(value = "preparationquestions", allEntries = true, condition = "#question.getQuestionVariant().equals('preparation')"),
			@CacheEvict(value = "flashcardquestions", allEntries = true, condition = "#question.getQuestionVariant().equals('flashcard')") },
			put = {@CachePut(value = "questions", key = "#question._id")})
	@Override
	public Question updateQuestion(final Question question) {
		try {
			final Document q = database.getDocument(question.get_id());

			question.updateRoundManagementState();
			q.put("subject", question.getSubject());
			q.put("text", question.getText());
			q.put("active", question.isActive());
			q.put("votingDisabled", question.isVotingDisabled());
			q.put("releasedFor", question.getReleasedFor());
			q.put("possibleAnswers", question.getPossibleAnswers());
			q.put("noCorrect", question.isNoCorrect());
			q.put("piRound", question.getPiRound());
			q.put("piRoundStartTime", question.getPiRoundStartTime());
			q.put("piRoundEndTime", question.getPiRoundEndTime());
			q.put("piRoundFinished", question.isPiRoundFinished());
			q.put("piRoundActive", question.isPiRoundActive());
			q.put("showStatistic", question.isShowStatistic());
			q.put("ignoreCaseSensitive", question.isIgnoreCaseSensitive());
			q.put("ignoreWhitespaces", question.isIgnoreWhitespaces());
			q.put("ignorePunctuation", question.isIgnorePunctuation());
			q.put("fixedAnswer", question.isFixedAnswer());
			q.put("strictMode", question.isStrictMode());
			q.put("rating", question.getRating());
			q.put("correctAnswer", question.getCorrectAnswer());
			q.put("showAnswer", question.isShowAnswer());
			q.put("abstention", question.isAbstention());
			q.put("image", question.getImage());
			q.put("fcImage", question.getFcImage());
			q.put("gridSize", question.getGridSize());
			q.put("offsetX", question.getOffsetX());
			q.put("offsetY", question.getOffsetY());
			q.put("zoomLvl", question.getZoomLvl());
			q.put("gridOffsetX", question.getGridOffsetX());
			q.put("gridOffsetY", question.getGridOffsetY());
			q.put("gridZoomLvl", question.getGridZoomLvl());
			q.put("gridSizeX", question.getGridSizeX());
			q.put("gridSizeY", question.getGridSizeY());
			q.put("gridIsHidden", question.getGridIsHidden());
			q.put("imgRotation", question.getImgRotation());
			q.put("toggleFieldsLeft", question.getToggleFieldsLeft());
			q.put("numClickableFields", question.getNumClickableFields());
			q.put("thresholdCorrectAnswers", question.getThresholdCorrectAnswers());
			q.put("cvIsColored", question.getCvIsColored());
			q.put("gridLineColor", question.getGridLineColor());
			q.put("numberOfDots", question.getNumberOfDots());
			q.put("gridType", question.getGridType());
			q.put("scaleFactor", question.getScaleFactor());
			q.put("gridScaleFactor", question.getGridScaleFactor());
			q.put("imageQuestion", question.isImageQuestion());
			q.put("hint", question.getHint());
			q.put("solution", question.getSolution());

			database.saveDocument(q);
			question.set_rev(q.getRev());

			return question;
		} catch (final IOException e) {
			logger.error("Could not update question {}.", question, e);
		}

		return null;
	}

	@Override
	public InterposedQuestion saveQuestion(final Session session, final InterposedQuestion question, User user) {
		final Document q = new Document();
		q.put("type", "interposed_question");
		q.put("sessionId", session.getId());
		q.put("subject", question.getSubject());
		q.put("text", question.getText());
		if (question.getTimestamp() != 0) {
			q.put("timestamp", question.getTimestamp());
		} else {
			q.put("timestamp", System.currentTimeMillis());
		}
		q.put("read", false);
		q.put("creator", user.getUsername());
		try {
			database.saveDocument(q);
			question.set_id(q.getId());
			question.set_rev(q.getRev());

			return question;
		} catch (final IOException e) {
			logger.error("Could not save interposed question {}.", question, e);
		}

		return null;
	}

	@Cacheable("questions")
	@Override
	public Question getQuestion(final String id) {
		try {
			final Document q = getDatabase().getDocument(id);
			if (q == null) {
				return null;
			}
			final Question question = (Question) JSONObject.toBean(q.getJSONObject(), Question.class);
			final JSONArray possibleAnswers = q.getJSONObject().getJSONArray("possibleAnswers");
			@SuppressWarnings("unchecked")
			final Collection<PossibleAnswer> answers = JSONArray.toCollection(possibleAnswers, PossibleAnswer.class);

			question.updateRoundManagementState();
			question.setPossibleAnswers(new ArrayList<>(answers));
			question.setSessionKeyword(sessionRepository.getSessionFromId(question.getSessionId()).getKeyword());
			return question;
		} catch (final IOException e) {
			logger.error("Could not get question {}.", id, e);
		}
		return null;
	}

	@Override
	public List<String> getQuestionIds(final Session session, final User user) {
		View view = new View("content/by_sessionid_variant_active");
		view.setKey(session.getId());
		return collectQuestionIds(view);
	}

	/* TODO: Only evict cache entry for the question's session. This requires some refactoring. */
	@Caching(evict = { @CacheEvict(value = "questions", key = "#question._id"),
			@CacheEvict(value = "skillquestions", allEntries = true),
			@CacheEvict(value = "lecturequestions", allEntries = true, condition = "#question.getQuestionVariant().equals('lecture')"),
			@CacheEvict(value = "preparationquestions", allEntries = true, condition = "#question.getQuestionVariant().equals('preparation')"),
			@CacheEvict(value = "flashcardquestions", allEntries = true, condition = "#question.getQuestionVariant().equals('flashcard')") })
	@Override
	public int deleteQuestionWithAnswers(final Question question) {
		try {
			int count = deleteAnswers(question);
			deleteDocument(question.get_id());
			dbLogger.log("delete", "type", "question", "answerCount", count);

			return count;
		} catch (final IOException e) {
			logger.error("Could not delete question {}.", question.get_id(), e);
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
		final View view = new View("content/by_sessionid_variant_active");
		view.setStartKeyArray(session.getId());
		view.setEndKey(session.getId(), "{}");

		return deleteAllQuestionDocumentsWithAnswers(view);
	}

	private int[] deleteAllQuestionDocumentsWithAnswers(final View view) {
		final ViewResults results = getDatabase().view(view);

		List<Question> questions = new ArrayList<>();
		for (final Document d : results.getResults()) {
			final Question q = new Question();
			q.set_id(d.getId());
			q.set_rev(d.getString("value"));
			questions.add(q);
		}

		int[] count = deleteAllAnswersWithQuestions(questions);
		dbLogger.log("delete", "type", "question", "questionCount", count[0]);
		dbLogger.log("delete", "type", "answer", "answerCount", count[1]);

		return count;
	}

	private void deleteDocument(final String documentId) throws IOException {
		final Document d = getDatabase().getDocument(documentId);
		getDatabase().deleteDocument(d);
	}

	@CacheEvict("answers")
	@Override
	public int deleteAnswers(final Question question) {
		try {
			final View view = new View("answer/by_questionid");
			view.setKey(question.get_id());
			view.setIncludeDocs(true);
			final ViewResults results = getDatabase().view(view);
			final List<List<Document>> partitions = Lists.partition(results.getResults(), BULK_PARTITION_SIZE);

			int count = 0;
			for (List<Document> partition: partitions) {
				List<Document> answersToDelete = new ArrayList<>();
				for (final Document a : partition) {
					final Document d = new Document(a.getJSONObject("doc"));
					d.put("_deleted", true);
					answersToDelete.add(d);
				}
				if (database.bulkSaveDocuments(answersToDelete.toArray(new Document[answersToDelete.size()]))) {
					count += partition.size();
				} else {
					logger.error("Could not bulk delete answers.");
				}
			}
			dbLogger.log("delete", "type", "answer", "answerCount", count);

			return count;
		} catch (final IOException e) {
			logger.error("Could not delete answers for question {}.", question.get_id(), e);
		}

		return 0;
	}

	@Override
	public List<String> getUnAnsweredQuestionIds(final Session session, final User user) {
		final View view = new View("answer/questionid_by_user_sessionid_variant");
		view.setStartKeyArray(user.getUsername(), session.getId());
		view.setEndKeyArray(user.getUsername(), session.getId(), "{}");
		return collectUnansweredQuestionIds(getQuestionIds(session, user), view);
	}

	@Override
	public Answer getMyAnswer(final User me, final String questionId, final int piRound) {

		final View view = new View("answer/doc_by_questionid_user_piround");
		if (2 == piRound) {
			view.setKey(questionId, me.getUsername(), "2");
		} else {
			/* needed for legacy questions whose piRound property has not been set */
			view.setStartKey(questionId, me.getUsername());
			view.setEndKey(questionId, me.getUsername(), "1");
		}
		final ViewResults results = getDatabase().view(view);
		if (results.getResults().isEmpty()) {
			return null;
		}
		return (Answer) JSONObject.toBean(
				results.getJSONArray("rows").optJSONObject(0).optJSONObject("value"),
				Answer.class
				);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getObjectFromId(final String documentId, final Class<T> klass) {
		try {
			final Document doc = getDatabase().getDocument(documentId);
			if (doc == null) {
				return null;
			}
			// TODO: This needs some more error checking...
			return (T) JSONObject.toBean(doc.getJSONObject(), klass);
		} catch (ClassCastException | IOException | net.sf.json.JSONException e) {
			return null;
		}
	}

	@Override
	public List<Answer> getAnswers(final Question question, final int piRound) {
		final String questionId = question.get_id();
		final View view = new View("answer/by_questionid_piround_text_subject");
		if (2 == piRound) {
			view.setStartKey(questionId, 2);
			view.setEndKey(questionId, 2, "{}");
		} else {
			/* needed for legacy questions whose piRound property has not been set */
			view.setStartKeyArray(questionId);
			view.setEndKeyArray(questionId, 1, "{}");
		}
		view.setGroup(true);
		final ViewResults results = getDatabase().view(view);
		final int abstentionCount = getDatabaseDao().getAbstentionAnswerCount(questionId);
		final List<Answer> answers = new ArrayList<>();

		for (final Document d : results.getResults()) {
			final Answer a = new Answer();
			a.setAnswerCount(d.getInt("value"));
			a.setAbstentionCount(abstentionCount);
			a.setQuestionId(d.getJSONObject().getJSONArray("key").getString(0));
			a.setPiRound(piRound);
			final String answerText = d.getJSONObject().getJSONArray("key").getString(3);
			a.setAnswerText("null".equals(answerText) ? null : answerText);
			answers.add(a);
		}
		return answers;
	}

	@Override
	public List<Answer> getAllAnswers(final Question question) {
		final String questionId = question.get_id();
		final View view = new View("answer/by_questionid_piround_text_subject");
		view.setStartKeyArray(questionId);
		view.setEndKeyArray(questionId, "{}");
		view.setGroup(true);
		final ViewResults results = getDatabase().view(view);
		final int abstentionCount = getDatabaseDao().getAbstentionAnswerCount(questionId);

		final List<Answer> answers = new ArrayList<>();
		for (final Document d : results.getResults()) {
			final Answer a = new Answer();
			a.setAnswerCount(d.getInt("value"));
			a.setAbstentionCount(abstentionCount);
			a.setQuestionId(d.getJSONObject().getJSONArray("key").getString(0));
			final String answerText = d.getJSONObject().getJSONArray("key").getString(3);
			final String answerSubject = d.getJSONObject().getJSONArray("key").getString(4);
			final boolean successfulFreeTextAnswer = d.getJSONObject().getJSONArray("key").getBoolean(5);
			a.setAnswerText("null".equals(answerText) ? null : answerText);
			a.setAnswerSubject("null".equals(answerSubject) ? null : answerSubject);
			a.setSuccessfulFreeTextAnswer(successfulFreeTextAnswer);
			answers.add(a);
		}
		return answers;
	}

	@Cacheable("answers")
	@Override
	public List<Answer> getAnswers(final Question question) {
		return this.getAnswers(question, question.getPiRound());
	}

	@Override
	public int getAbstentionAnswerCount(final String questionId) {
		final View view = new View("answer/by_questionid_piround_text_subject");
		view.setStartKeyArray(questionId);
		view.setEndKeyArray(questionId, "{}");
		view.setGroup(true);
		final ViewResults results = getDatabase().view(view);
		if (results.getResults().isEmpty()) {
			return 0;
		}
		return results.getJSONArray("rows").optJSONObject(0).optInt("value");
	}

	@Override
	public int getAnswerCount(final Question question, final int piRound) {
		final View view = new View("answer/by_questionid_piround_text_subject");
		view.setStartKey(question.get_id(), piRound);
		view.setEndKey(question.get_id(), piRound, "{}");
		view.setGroup(true);
		final ViewResults results = getDatabase().view(view);
		if (results.getResults().isEmpty()) {
			return 0;
		}

		return results.getJSONArray("rows").optJSONObject(0).optInt("value");
	}

	@Override
	public int getTotalAnswerCountByQuestion(final Question question) {
		final View view = new View("answer/by_questionid_piround_text_subject");
		view.setStartKeyArray(question.get_id());
		view.setEndKeyArray(question.get_id(), "{}");
		view.setGroup(true);
		final ViewResults results = getDatabase().view(view);

		if (results.getResults().isEmpty()) {
			return 0;
		}

		return results.getJSONArray("rows").optJSONObject(0).optInt("value");
	}

	private boolean isEmptyResults(final ViewResults results) {
		return results == null || results.getResults().isEmpty() || results.getJSONArray("rows").isEmpty();
	}

	@Override
	public List<Answer> getFreetextAnswers(final String questionId, final int start, final int limit) {
		final List<Answer> answers = new ArrayList<>();
		final View view = new View("answer/doc_by_questionid_timestamp");
		if (start > 0) {
			view.setSkip(start);
		}
		if (limit > 0) {
			view.setLimit(limit);
		}
		view.setDescending(true);
		view.setStartKeyArray(questionId, "{}");
		view.setEndKeyArray(questionId);
		final ViewResults results = getDatabase().view(view);
		if (results.getResults().isEmpty()) {
			return answers;
		}
		for (final Document d : results.getResults()) {
			final Answer a = (Answer) JSONObject.toBean(d.getJSONObject().getJSONObject("value"), Answer.class);
			a.setQuestionId(questionId);
			answers.add(a);
		}
		return answers;
	}

	@Override
	public List<Answer> getMyAnswers(final User me, final Session s) {
		final View view = new View("answer/doc_by_user_sessionid");
		view.setKey(me.getUsername(), s.getId());
		final ViewResults results = getDatabase().view(view);
		final List<Answer> answers = new ArrayList<>();
		if (results == null || results.getResults() == null || results.getResults().isEmpty()) {
			return answers;
		}
		for (final Document d : results.getResults()) {
			final Answer a = (Answer) JSONObject.toBean(d.getJSONObject().getJSONObject("value"), Answer.class);
			a.set_id(d.getId());
			a.set_rev(d.getRev());
			a.setUser(me.getUsername());
			a.setSessionId(s.getId());
			answers.add(a);
		}
		return answers;
	}

	@Override
	public int getTotalAnswerCount(final String sessionKey) {
		final Session s = sessionRepository.getSessionFromKeyword(sessionKey);
		if (s == null) {
			throw new NotFoundException();
		}

		final View view = new View("answer/by_sessionid_variant");
		view.setKey(s.getId());
		final ViewResults results = getDatabase().view(view);
		if (results.getResults().isEmpty()) {
			return 0;
		}
		return results.getJSONArray("rows").optJSONObject(0).optInt("value");
	}

	@Override
	public int getInterposedCount(final String sessionKey) {
		final Session s = sessionRepository.getSessionFromKeyword(sessionKey);
		if (s == null) {
			throw new NotFoundException();
		}

		final View view = new View("comment/by_sessionid");
		view.setKey(s.getId());
		view.setGroup(true);
		final ViewResults results = getDatabase().view(view);
		if (results.isEmpty() || results.getResults().isEmpty()) {
			return 0;
		}
		return results.getJSONArray("rows").optJSONObject(0).optInt("value");
	}

	@Override
	public InterposedReadingCount getInterposedReadingCount(final Session session) {
		final View view = new View("comment/by_sessionid_read");
		view.setStartKeyArray(session.getId());
		view.setEndKeyArray(session.getId(), "{}");
		view.setGroup(true);
		return getInterposedReadingCount(view);
	}

	@Override
	public InterposedReadingCount getInterposedReadingCount(final Session session, final User user) {
		final View view = new View("comment/by_sessionid_creator_read");
		view.setStartKeyArray(session.getId(), user.getUsername());
		view.setEndKeyArray(session.getId(), user.getUsername(), "{}");
		view.setGroup(true);
		return getInterposedReadingCount(view);
	}

	private InterposedReadingCount getInterposedReadingCount(final View view) {
		final ViewResults results = getDatabase().view(view);
		if (results.isEmpty() || results.getResults().isEmpty()) {
			return new InterposedReadingCount();
		}
		// A complete result looks like this. Note that the second row is optional, and that the first one may be
		// 'unread' or 'read', i.e., results may be switched around or only one result may be present.
		// count = {"rows":[
		// {"key":["cecebabb21b096e592d81f9c1322b877","Guestc9350cf4a3","read"],"value":1},
		// {"key":["cecebabb21b096e592d81f9c1322b877","Guestc9350cf4a3","unread"],"value":1}
		// ]}
		int read = 0, unread = 0;
		boolean isRead = false;
		final JSONObject fst = results.getJSONArray("rows").getJSONObject(0);
		final JSONObject snd = results.getJSONArray("rows").optJSONObject(1);

		final JSONArray fstkey = fst.getJSONArray("key");
		if (fstkey.size() == 2) {
			isRead = fstkey.getBoolean(1);
		} else if (fstkey.size() == 3) {
			isRead = fstkey.getBoolean(2);
		}
		if (isRead) {
			read = fst.optInt("value");
		} else {
			unread = fst.optInt("value");
		}

		if (snd != null) {
			final JSONArray sndkey = snd.getJSONArray("key");
			if (sndkey.size() == 2) {
				isRead = sndkey.getBoolean(1);
			} else {
				isRead = sndkey.getBoolean(2);
			}
			if (isRead) {
				read = snd.optInt("value");
			} else {
				unread = snd.optInt("value");
			}
		}
		return new InterposedReadingCount(read, unread);
	}

	@Override
	public List<InterposedQuestion> getInterposedQuestions(final Session session, final int start, final int limit) {
		final View view = new View("comment/doc_by_sessionid_timestamp");
		if (start > 0) {
			view.setSkip(start);
		}
		if (limit > 0) {
			view.setLimit(limit);
		}
		view.setDescending(true);
		view.setStartKeyArray(session.getId(), "{}");
		view.setEndKeyArray(session.getId());
		final ViewResults questions = getDatabase().view(view);
		if (questions == null || questions.isEmpty()) {
			return null;
		}
		return createInterposedList(session, questions);
	}

	@Override
	public List<InterposedQuestion> getInterposedQuestions(final Session session, final User user, final int start, final int limit) {
		final View view = new View("comment/doc_by_sessionid_creator_timestamp");
		if (start > 0) {
			view.setSkip(start);
		}
		if (limit > 0) {
			view.setLimit(limit);
		}
		view.setDescending(true);
		view.setStartKeyArray(session.getId(), user.getUsername(), "{}");
		view.setEndKeyArray(session.getId(), user.getUsername());
		final ViewResults questions = getDatabase().view(view);
		if (questions == null || questions.isEmpty()) {
			return null;
		}
		return createInterposedList(session, questions);
	}

	private List<InterposedQuestion> createInterposedList(
			final Session session, final ViewResults questions) {
		final List<InterposedQuestion> result = new ArrayList<>();
		for (final Document document : questions.getResults()) {
			final InterposedQuestion question = (InterposedQuestion) JSONObject.toBean(
					document.getJSONObject().getJSONObject("value"),
					InterposedQuestion.class
					);
			question.setSessionId(session.getKeyword());
			question.set_id(document.getId());
			result.add(question);
		}
		return result;
	}

	@Cacheable("statistics")
	@Override
	public Statistics getStatistics() {
		final Statistics stats = new Statistics();
		try {
			final View statsView = new View("statistics/statistics");
			final View creatorView = new View("statistics/unique_session_creators");
			final View studentUserView = new View("statistics/active_student_users");
			statsView.setGroup(true);
			creatorView.setGroup(true);
			studentUserView.setGroup(true);

			final ViewResults statsResults = getDatabase().view(statsView);
			final ViewResults creatorResults = getDatabase().view(creatorView);
			final ViewResults studentUserResults = getDatabase().view(studentUserView);

			if (!isEmptyResults(statsResults)) {
				final JSONArray rows = statsResults.getJSONArray("rows");
				for (int i = 0; i < rows.size(); i++) {
					final JSONObject row = rows.getJSONObject(i);
					final int value = row.getInt("value");
					switch (row.getString("key")) {
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
					}
				}
			}
			if (!isEmptyResults(creatorResults)) {
				final JSONArray rows = creatorResults.getJSONArray("rows");
				Set<String> creators = new HashSet<>();
				for (int i = 0; i < rows.size(); i++) {
					final JSONObject row = rows.getJSONObject(i);
					creators.add(row.getString("key"));
				}
				stats.setCreators(creators.size());
			}
			if (!isEmptyResults(studentUserResults)) {
				final JSONArray rows = studentUserResults.getJSONArray("rows");
				Set<String> students = new HashSet<>();
				for (int i = 0; i < rows.size(); i++) {
					final JSONObject row = rows.getJSONObject(i);
					students.add(row.getString("key"));
				}
				stats.setActiveStudents(students.size());
			}
			return stats;
		} catch (final Exception e) {
			logger.error("Could not retrieve session count.", e);
		}
		return stats;
	}

	@Override
	public InterposedQuestion getInterposedQuestion(final String questionId) {
		try {
			final Document document = getDatabase().getDocument(questionId);
			final InterposedQuestion question = (InterposedQuestion) JSONObject.toBean(document.getJSONObject(),
					InterposedQuestion.class);
			/* TODO: Refactor code so the next line can be removed */
			question.setSessionId(sessionRepository.getSessionFromKeyword(question.getSessionId()).getId());
			return question;
		} catch (final IOException e) {
			logger.error("Could not load interposed question {}.", questionId, e);
		}
		return null;
	}

	@Override
	public void markInterposedQuestionAsRead(final InterposedQuestion question) {
		try {
			question.setRead(true);
			final Document document = getDatabase().getDocument(question.get_id());
			document.put("read", question.isRead());
			getDatabase().saveDocument(document);
		} catch (final IOException e) {
			logger.error("Could not mark interposed question as read {}.", question.get_id(), e);
		}
	}

	@CacheEvict(value = "answers", key = "#question")
	@Override
	public Answer saveAnswer(final Answer answer, final User user, final Question question, final Session session) {
		final Document a = new Document();
		a.put("type", "skill_question_answer");
		a.put("sessionId", answer.getSessionId());
		a.put("questionId", answer.getQuestionId());
		a.put("answerSubject", answer.getAnswerSubject());
		a.put("questionVariant", answer.getQuestionVariant());
		a.put("questionValue", answer.getQuestionValue());
		a.put("answerText", answer.getAnswerText());
		a.put("answerTextRaw", answer.getAnswerTextRaw());
		a.put("successfulFreeTextAnswer", answer.isSuccessfulFreeTextAnswer());
		a.put("timestamp", answer.getTimestamp());
		a.put("user", user.getUsername());
		a.put("piRound", answer.getPiRound());
		a.put("abstention", answer.isAbstention());
		a.put("answerImage", answer.getAnswerImage());
		a.put("answerThumbnailImage", answer.getAnswerThumbnailImage());
		AnswerQueueElement answerQueueElement = new AnswerQueueElement(session, question, answer, user);
		this.answerQueue.offer(new AbstractMap.SimpleEntry<>(a, answerQueueElement));
		return answer;
	}

	@Scheduled(fixedDelay = 5000)
	public void flushAnswerQueue() {
		final Map<Document, Answer> map = new HashMap<>();
		final List<Document> answerList = new ArrayList<>();
		final List<AnswerQueueElement> elements = new ArrayList<>();
		AbstractMap.SimpleEntry<Document, AnswerQueueElement> entry;
		while ((entry = this.answerQueue.poll()) != null) {
			final Document doc = entry.getKey();
			final Answer answer = entry.getValue().getAnswer();
			map.put(doc, answer);
			answerList.add(doc);
			elements.add(entry.getValue());
		}
		if (answerList.isEmpty()) {
			// no need to send an empty bulk request. ;-)
			return;
		}
		try {
			getDatabase().bulkSaveDocuments(answerList.toArray(new Document[answerList.size()]));
			for (Document d : answerList) {
				final Answer answer = map.get(d);
				answer.set_id(d.getId());
				answer.set_rev(d.getRev());
			}
			// Send NewAnswerEvents ...
			for (AnswerQueueElement e : elements) {
				this.publisher.publishEvent(new NewAnswerEvent(this, e.getSession(), e.getAnswer(), e.getUser(), e.getQuestion()));
			}
		} catch (IOException e) {
			logger.error("Could not bulk save answers from queue.", e);
		}
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public Answer updateAnswer(final Answer answer) {
		try {
			final Document a = database.getDocument(answer.get_id());
			a.put("answerSubject", answer.getAnswerSubject());
			a.put("answerText", answer.getAnswerText());
			a.put("answerTextRaw", answer.getAnswerTextRaw());
			a.put("successfulFreeTextAnswer", answer.isSuccessfulFreeTextAnswer());
			a.put("timestamp", answer.getTimestamp());
			a.put("abstention", answer.isAbstention());
			a.put("questionValue", answer.getQuestionValue());
			a.put("answerImage", answer.getAnswerImage());
			a.put("answerThumbnailImage", answer.getAnswerThumbnailImage());
			a.put("read", answer.isRead());
			database.saveDocument(a);
			answer.set_rev(a.getRev());
			return answer;
		} catch (final IOException e) {
			logger.error("Could not update answer {}.", answer, e);
		}
		return null;
	}

	/* TODO: Only evict cache entry for the answer's session. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public void deleteAnswer(final String answerId) {
		try {
			database.deleteDocument(database.getDocument(answerId));
			dbLogger.log("delete", "type", "answer");
		} catch (final IOException e) {
			logger.error("Could not delete answer {}.", answerId, e);
		}
	}

	@Override
	public void deleteInterposedQuestion(final InterposedQuestion question) {
		try {
			deleteDocument(question.get_id());
			dbLogger.log("delete", "type", "comment");
		} catch (final IOException e) {
			logger.error("Could not delete interposed question {}.", question.get_id(), e);
		}
	}

	/**
	 * Adds convenience methods to CouchDB4J's view class.
	 */
	private static class ExtendedView extends View {

		ExtendedView(final String fullname) {
			super(fullname);
		}

		void setCourseIdKeys(final List<Course> courses) {
			List<String> courseIds = new ArrayList<>();
			for (Course c : courses) {
				courseIds.add(c.getId());
			}
			setKeys(courseIds);
		}

		void setSessionIdKeys(final List<Session> sessions) {
			List<String> sessionIds = new ArrayList<>();
			for (Session s : sessions) {
				sessionIds.add(s.getId());
			}
			setKeys(sessionIds);
		}
	}

	@Override
	public int deleteInactiveGuestVisitedSessionLists(long lastActivityBefore) {
		try {
			View view = new View("logged_in/by_last_activity_for_guests");
			view.setEndKey(lastActivityBefore);
			List<Document> results = this.getDatabase().view(view).getResults();

			int count = 0;
			List<List<Document>> partitions = Lists.partition(results, BULK_PARTITION_SIZE);
			for (List<Document> partition: partitions) {
				final List<Document> newDocs = new ArrayList<>();
				for (final Document oldDoc : partition) {
					final Document newDoc = new Document();
					newDoc.setId(oldDoc.getId());
					newDoc.setRev(oldDoc.getJSONObject("value").getString("_rev"));
					newDoc.put("_deleted", true);
					newDocs.add(newDoc);
					logger.debug("Marked logged_in document {} for deletion.", oldDoc.getId());
					/* Use log type 'user' since effectively the user is deleted in case of guests */
					dbLogger.log("delete", "type", "user", "id", oldDoc.getId());
				}

				if (!newDocs.isEmpty()) {
					if (getDatabase().bulkSaveDocuments(newDocs.toArray(new Document[newDocs.size()]))) {
						count += newDocs.size();
					} else {
						logger.error("Could not bulk delete visited session lists.");
					}
				}
			}

			if (count > 0) {
				logger.info("Deleted {} visited session lists of inactive users.", count);
				dbLogger.log("cleanup", "type", "visitedsessions", "count", count);
			}

			return count;
		} catch (IOException e) {
			logger.error("Could not delete visited session lists of inactive users.", e);
		}

		return 0;
	}

	@Cacheable("lecturequestions")
	@Override
	public List<Question> getLectureQuestionsForUsers(final Session session) {
		final View view = new View("content/doc_by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), "lecture", true);
		view.setEndKeyArray(session.getId(), "lecture", true, "{}");

		return getQuestions(view, session);
	}

	@Override
	public List<Question> getLectureQuestionsForTeachers(final Session session) {
		final View view = new View("content/doc_by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), "lecture");
		view.setEndKeyArray(session.getId(), "lecture", "{}");

		return getQuestions(view, session);
	}

	@Cacheable("flashcardquestions")
	@Override
	public List<Question> getFlashcardsForUsers(final Session session) {
		final View view = new View("content/doc_by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), "flashcard", true);
		view.setEndKeyArray(session.getId(), "flashcard", true, "{}");

		return getQuestions(view, session);
	}

	@Override
	public List<Question> getFlashcardsForTeachers(final Session session) {
		final View view = new View("content/doc_by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), "flashcard");
		view.setEndKeyArray(session.getId(), "flashcard", "{}");

		return getQuestions(view, session);
	}

	@Cacheable("preparationquestions")
	@Override
	public List<Question> getPreparationQuestionsForUsers(final Session session) {
		final View view = new View("content/doc_by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), "preparation", true);
		view.setEndKeyArray(session.getId(), "preparation", true, "{}");

		return getQuestions(view, session);
	}

	@Override
	public List<Question> getPreparationQuestionsForTeachers(final Session session) {
		final View view = new View("content/doc_by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), "preparation");
		view.setEndKeyArray(session.getId(), "preparation", "{}");

		return getQuestions(view, session);
	}

	@Override
	public List<Question> getAllSkillQuestions(final Session session) {
		final View view = new View("content/doc_by_sessionid_variant_active");
		view.setStartKeyArray(session.getId());
		view.setEndKeyArray(session.getId(), "{}");

		return getQuestions(view, session);
	}

	private List<Question> getQuestions(final View view, final Session session) {
		final ViewResults viewResults = getDatabase().view(view);
		if (viewResults == null || viewResults.isEmpty()) {
			return null;
		}

		final List<Question> questions = new ArrayList<>();

		Results<Question> results = getDatabase().queryView(view, Question.class);
		for (final RowResult<Question> row : results.getRows()) {
			Question question = row.getValue();
			question.updateRoundManagementState();
			question.setSessionKeyword(session.getKeyword());
			if (!"freetext".equals(question.getQuestionType()) && 0 == question.getPiRound()) {
				/* needed for legacy questions whose piRound property has not been set */
				question.setPiRound(1);
			}

			questions.add(question);
		}
		return questions;
	}

	@Override
	public int getLectureQuestionCount(final Session session) {
		final View view = new View("content/by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), "lecture");
		view.setEndKeyArray(session.getId(), "lecture", "{}");

		return getQuestionCount(view);
	}

	@Override
	public int getFlashcardCount(final Session session) {
		final View view = new View("content/by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), "flashcard");
		view.setEndKeyArray(session.getId(), "flashcard", "{}");

		return getQuestionCount(view);
	}

	@Override
	public int getPreparationQuestionCount(final Session session) {
		final View view = new View("content/by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), "preparation");
		view.setEndKeyArray(session.getId(), "preparation", "{}");

		return getQuestionCount(view);
	}

	private int getQuestionCount(final View view) {
		view.setReduce(true);
		final ViewResults results = getDatabase().view(view);
		if (results.getJSONArray("rows").optJSONObject(0) == null) {
			return 0;
		}
		return results.getJSONArray("rows").optJSONObject(0).optInt("value");
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
		final View view = new View("answer/by_sessionid_variant");
		view.setKey(session.getId(), variant);
		view.setReduce(true);
		final ViewResults results = getDatabase().view(view);
		if (results.getResults().isEmpty()) {
			return 0;
		}
		return results.getJSONArray("rows").optJSONObject(0).optInt("value");
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@Caching(evict = { @CacheEvict(value = "questions", allEntries = true),
			@CacheEvict("skillquestions"),
			@CacheEvict("lecturequestions"),
			@CacheEvict(value = "answers", allEntries = true)})
	@Override
	public int[] deleteAllLectureQuestionsWithAnswers(final Session session) {
		final View view = new View("content/by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), "lecture");
		view.setEndKey(session.getId(), "lecture", "{}");

		return deleteAllQuestionDocumentsWithAnswers(view);
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@Caching(evict = { @CacheEvict(value = "questions", allEntries = true),
			@CacheEvict("skillquestions"),
			@CacheEvict("flashcardquestions"),
			@CacheEvict(value = "answers", allEntries = true)})
	@Override
	public int[] deleteAllFlashcardsWithAnswers(final Session session) {
		final View view = new View("content/by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), "flashcard");
		view.setEndKey(session.getId(), "flashcard", "{}");

		return deleteAllQuestionDocumentsWithAnswers(view);
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@Caching(evict = { @CacheEvict(value = "questions", allEntries = true),
			@CacheEvict("skillquestions"),
			@CacheEvict("preparationquestions"),
			@CacheEvict(value = "answers", allEntries = true)})
	@Override
	public int[] deleteAllPreparationQuestionsWithAnswers(final Session session) {
		final View view = new View("content/by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), "preparation");
		view.setEndKey(session.getId(), "preparation", "{}");

		return deleteAllQuestionDocumentsWithAnswers(view);
	}

	@Override
	public List<String> getUnAnsweredLectureQuestionIds(final Session session, final User user) {
		final View view = new View("answer/questionid_piround_by_user_sessionid_variant");
		view.setKey(user.getUsername(), session.getId(), "lecture");
		return collectUnansweredQuestionIdsByPiRound(getDatabaseDao().getLectureQuestionsForUsers(session), view);
	}

	@Override
	public List<String> getUnAnsweredPreparationQuestionIds(final Session session, final User user) {
		final View view = new View("answer/questionid_piround_by_user_sessionid_variant");
		view.setKey(user.getUsername(), session.getId(), "preparation");
		return collectUnansweredQuestionIdsByPiRound(getDatabaseDao().getPreparationQuestionsForUsers(session), view);
	}

	private List<String> collectUnansweredQuestionIds(
			final List<String> questions,
			final View view
			) {
		final ViewResults answeredQuestions = getDatabase().view(view);

		final List<String> answered = new ArrayList<>();
		for (final Document d : answeredQuestions.getResults()) {
			answered.add(d.getString("value"));
		}

		final List<String> unanswered = new ArrayList<>();
		for (final String questionId : questions) {
			if (!answered.contains(questionId)) {
				unanswered.add(questionId);
			}
		}
		return unanswered;
	}

	private List<String> collectUnansweredQuestionIdsByPiRound(
			final List<Question> questions,
			final View view
			) {
		final ViewResults answeredQuestions = getDatabase().view(view);

		final Map<String, Integer> answered = new HashMap<>();
		for (final Document d : answeredQuestions.getResults()) {
			answered.put(d.getJSONArray("value").getString(0), d.getJSONArray("value").getInt(1));
		}

		final List<String> unanswered = new ArrayList<>();

		for (final Question question : questions) {
			if (!"slide".equals(question.getQuestionType()) && (!answered.containsKey(question.get_id())
					|| (answered.containsKey(question.get_id()) && answered.get(question.get_id()) != question.getPiRound()))) {
				unanswered.add(question.get_id());
			}
		}

		return unanswered;
	}

	private List<String> collectQuestionIds(final View view) {
		final ViewResults results = getDatabase().view(view);
		if (results.getResults().isEmpty()) {
			return new ArrayList<>();
		}
		final List<String> ids = new ArrayList<>();
		for (final Document d : results.getResults()) {
			ids.add(d.getId());
		}
		return ids;
	}

	@Override
	public int deleteAllInterposedQuestions(final Session session) {
		final View view = new View("comment/by_sessionid");
		view.setKey(session.getId());
		final ViewResults questions = getDatabase().view(view);

		return deleteAllInterposedQuestions(session, questions);
	}

	@Override
	public int deleteAllInterposedQuestions(final Session session, final User user) {
		final View view = new View("comment/by_sessionid_creator_read");
		view.setStartKeyArray(session.getId(), user.getUsername());
		view.setEndKeyArray(session.getId(), user.getUsername(), "{}");
		final ViewResults questions = getDatabase().view(view);

		return deleteAllInterposedQuestions(session, questions);
	}

	private int deleteAllInterposedQuestions(final Session session, final ViewResults questions) {
		if (questions == null || questions.isEmpty()) {
			return 0;
		}
		List<Document> results = questions.getResults();
		/* TODO: use bulk delete */
		for (final Document document : results) {
			try {
				deleteDocument(document.getId());
			} catch (final IOException e) {
				logger.error("Could not delete all interposed questions {}.", session, e);
			}
		}

		/* This does account for failed deletions */
		dbLogger.log("delete", "type", "comment", "commentCount", results.size());

		return results.size();
	}

	@Override
	public List<Question> publishAllQuestions(final Session session, final boolean publish) {
		final View view = new View("content/doc_by_sessionid_variant_active");
		view.setStartKeyArray(session.getId());
		view.setEndKeyArray(session.getId(), "{}");
		final List<Question> questions = getQuestions(view, session);
		getDatabaseDao().publishQuestions(session, publish, questions);

		return questions;
	}

	@Caching(evict = { @CacheEvict(value = "questions", allEntries = true),
			@CacheEvict(value = "skillquestions", key = "#session"),
			@CacheEvict(value = "lecturequestions", key = "#session"),
			@CacheEvict(value = "preparationquestions", key = "#session"),
			@CacheEvict(value = "flashcardquestions", key = "#session") })
	@Override
	public void publishQuestions(final Session session, final boolean publish, List<Question> questions) {
		for (final Question q : questions) {
			q.setActive(publish);
		}
		final List<Document> documents = new ArrayList<>();
		for (final Question q : questions) {
			final Document d = toQuestionDocument(session, q);
			d.setId(q.get_id());
			d.setRev(q.get_rev());
			documents.add(d);
		}
		try {
			database.bulkSaveDocuments(documents.toArray(new Document[documents.size()]));
		} catch (final IOException e) {
			logger.error("Could not bulk publish all questions.", e);
		}
	}

	@Override
	public List<Question> setVotingAdmissionForAllQuestions(final Session session, final boolean disableVoting) {
		final View view = new View("content/doc_by_sessionid_variant_active");
		view.setStartKeyArray(session.getId());
		view.setEndKeyArray(session.getId(), "{}");
		final List<Question> questions = getQuestions(view, session);
		getDatabaseDao().setVotingAdmissions(session, disableVoting, questions);

		return questions;
	}

	@Caching(evict = { @CacheEvict(value = "questions", allEntries = true),
			@CacheEvict(value = "skillquestions", key = "#session"),
			@CacheEvict(value = "lecturequestions", key = "#session"),
			@CacheEvict(value = "preparationquestions", key = "#session"),
			@CacheEvict(value = "flashcardquestions", key = "#session") })
	@Override
	public void setVotingAdmissions(final Session session, final boolean disableVoting, List<Question> questions) {
		for (final Question q : questions) {
			if (!"flashcard".equals(q.getQuestionType())) {
				q.setVotingDisabled(disableVoting);
			}
		}
		final List<Document> documents = new ArrayList<>();
		for (final Question q : questions) {
			final Document d = toQuestionDocument(session, q);
			d.setId(q.get_id());
			d.setRev(q.get_rev());
			documents.add(d);
		}

		try {
			database.bulkSaveDocuments(documents.toArray(new Document[documents.size()]));
		} catch (final IOException e) {
			logger.error("Could not bulk set voting admission for all questions.", e);
		}
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public int deleteAllQuestionsAnswers(final Session session) {
		final View view = new View("content/doc_by_sessionid_variant_active");
		view.setStartKeyArray(session.getId());
		view.setEndKeyArray(session.getId(), "{}");
		final List<Question> questions = getQuestions(view, session);
		getDatabaseDao().resetQuestionsRoundState(session, questions);

		return deleteAllAnswersForQuestions(questions);
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public int deleteAllPreparationAnswers(final Session session) {
		final View view = new View("content/doc_by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), "preparation");
		view.setEndKeyArray(session.getId(), "preparation", "{}");
		final List<Question> questions = getQuestions(view, session);
		getDatabaseDao().resetQuestionsRoundState(session, questions);

		return deleteAllAnswersForQuestions(questions);
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public int deleteAllLectureAnswers(final Session session) {
		final View view = new View("content/doc_by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), "lecture");
		view.setEndKeyArray(session.getId(), "lecture", "{}");
		final List<Question> questions = getQuestions(view, session);
		getDatabaseDao().resetQuestionsRoundState(session, questions);

		return deleteAllAnswersForQuestions(questions);
	}

	@Caching(evict = { @CacheEvict(value = "questions", allEntries = true),
			@CacheEvict(value = "skillquestions", key = "#session"),
			@CacheEvict(value = "lecturequestions", key = "#session"),
			@CacheEvict(value = "preparationquestions", key = "#session"),
			@CacheEvict(value = "flashcardquestions", key = "#session") })
	@Override
	public void resetQuestionsRoundState(final Session session, List<Question> questions) {
		for (final Question q : questions) {
			q.resetQuestionState();
		}
		final List<Document> documents = new ArrayList<>();
		for (final Question q : questions) {
			final Document d = toQuestionDocument(session, q);
			d.setId(q.get_id());
			d.setRev(q.get_rev());
			documents.add(d);
		}
		try {
			database.bulkSaveDocuments(documents.toArray(new Document[documents.size()]));
		} catch (final IOException e) {
			logger.error("Could not bulk reset all questions round state.", e);
		}
	}

	private int deleteAllAnswersForQuestions(List<Question> questions) {
		List<String> questionIds = new ArrayList<>();
		for (Question q : questions) {
			questionIds.add(q.get_id());
		}
		final View bulkView = new View("answer/by_questionid");
		bulkView.setKeys(questionIds);
		bulkView.setIncludeDocs(true);
		final List<Document> result = getDatabase().view(bulkView).getResults();
		final List<Document> allAnswers = new ArrayList<>();
		for (Document a : result) {
			final Document d = new Document(a.getJSONObject("doc"));
			d.put("_deleted", true);
			allAnswers.add(d);
		}
		try {
			getDatabase().bulkSaveDocuments(allAnswers.toArray(new Document[allAnswers.size()]));

			return allAnswers.size();
		} catch (IOException e) {
			logger.error("Could not bulk delete answers.", e);
		}

		return 0;
	}

	private int[] deleteAllAnswersWithQuestions(List<Question> questions) {
		List<String> questionIds = new ArrayList<>();
		final List<Document> allQuestions = new ArrayList<>();
		for (Question q : questions) {
			final Document d = new Document();
			d.put("_id", q.get_id());
			d.put("_rev", q.get_rev());
			d.put("_deleted", true);
			questionIds.add(q.get_id());
			allQuestions.add(d);
		}
		final View bulkView = new View("answer/by_questionid");
		bulkView.setKeys(questionIds);
		bulkView.setIncludeDocs(true);
		final List<Document> result = getDatabase().view(bulkView).getResults();

		final List<Document> allAnswers = new ArrayList<>();
		for (Document a : result) {
			final Document d = new Document(a.getJSONObject("doc"));
			d.put("_deleted", true);
			allAnswers.add(d);
		}

		try {
			List<Document> deleteList = new ArrayList<>(allAnswers);
			deleteList.addAll(allQuestions);
			getDatabase().bulkSaveDocuments(deleteList.toArray(new Document[deleteList.size()]));

			return new int[] {deleteList.size(), result.size()};
		} catch (IOException e) {
			logger.error("Could not bulk delete questions and answers.", e);
		}

		return new int[] {0, 0};
	}

	@Cacheable("learningprogress")
	@Override
	public CourseScore getLearningProgress(final Session session) {
		final View maximumValueView = new View("learning_progress/maximum_value_of_question");
		final View answerSumView = new View("learning_progress/question_value_achieved_for_user");
		maximumValueView.setStartKeyArray(session.getId());
		maximumValueView.setEndKeyArray(session.getId(), "{}");
		answerSumView.setStartKeyArray(session.getId());
		answerSumView.setEndKeyArray(session.getId(), "{}");

		final List<Document> maximumValueResult = getDatabase().view(maximumValueView).getResults();
		final List<Document> answerSumResult = getDatabase().view(answerSumView).getResults();

		CourseScore courseScore = new CourseScore();

		// no results found
		if (maximumValueResult.isEmpty() && answerSumResult.isEmpty()) {
			return courseScore;
		}

		// collect mapping (questionId -> max value)
		for (Document d : maximumValueResult) {
			String questionId = d.getJSONArray("key").getString(1);
			JSONObject value = d.getJSONObject("value");
			int questionScore = value.getInt("value");
			String questionVariant = value.getString("questionVariant");
			int piRound = value.getInt("piRound");
			courseScore.addQuestion(questionId, questionVariant, piRound, questionScore);
		}
		// collect mapping (questionId -> (user -> value))
		for (Document d : answerSumResult) {
			String username = d.getJSONArray("key").getString(1);
			JSONObject value = d.getJSONObject("value");
			String questionId = value.getString("questionId");
			int userscore = value.getInt("score");
			int piRound = value.getInt("piRound");
			courseScore.addAnswer(questionId, piRound, username, userscore);
		}
		return courseScore;
	}

	@Override
	public List<String> getSubjects(Session session, String questionVariant) {
		final View view = new View("content/by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), questionVariant);
		view.setEndKeyArray(session.getId(), questionVariant, "{}");
		ViewResults results = this.getDatabase().view(view);

		if (results.getJSONArray("rows").optJSONObject(0) == null) {
			return null;
		}

		Set<String> uniqueSubjects = new HashSet<>();

		for (final Document d : results.getResults()) {
			uniqueSubjects.add(d.getJSONArray("key").getString(3));
		}

		return new ArrayList<>(uniqueSubjects);
	}

	/* TODO: remove if this method is no longer used */
	@Override
	public List<String> getQuestionIdsBySubject(Session session, String questionVariant, String subject) {
		final View view = new View("content/by_sessionid_variant_active");
		view.setStartKeyArray(session.getId(), questionVariant, 1,	subject);
		view.setEndKeyArray(session.getId(), questionVariant, 1, subject, "{}");
		ViewResults results = this.getDatabase().view(view);

		if (results.getJSONArray("rows").optJSONObject(0) == null) {
			return null;
		}

		List<String> qids = new ArrayList<>();

		for (final Document d : results.getResults()) {
			final String s = d.getId();
			qids.add(s);
		}

		return qids;
	}

	@Override
	public List<Question> getQuestionsByIds(List<String> ids, final Session session) {
		View view = new View("_all_docs");
		view.setKeys(ids);
		view.setIncludeDocs(true);
		final List<Document> questiondocs = getDatabase().view(view).getResults();
		if (questiondocs == null || questiondocs.isEmpty()) {

			return null;
		}
		final List<Question> result = new ArrayList<>();
		final MorpherRegistry morpherRegistry = JSONUtils.getMorpherRegistry();
		final Morpher dynaMorpher = new BeanMorpher(PossibleAnswer.class, morpherRegistry);
		morpherRegistry.registerMorpher(dynaMorpher);
		for (final Document document : questiondocs) {
			if (!"".equals(document.optString("error"))) {
				// Skip documents we could not load. Maybe they were deleted.
				continue;
			}
			final Question question = (Question) JSONObject.toBean(
					document.getJSONObject().getJSONObject("doc"),
					Question.class
					);
			@SuppressWarnings("unchecked")
			final Collection<PossibleAnswer> answers = JSONArray.toCollection(
					document.getJSONObject().getJSONObject("doc").getJSONArray("possibleAnswers"),
					PossibleAnswer.class
					);
			question.setPossibleAnswers(new ArrayList<>(answers));
			question.setSessionKeyword(session.getKeyword());
			if (!"freetext".equals(question.getQuestionType()) && 0 == question.getPiRound()) {
				/* needed for legacy questions whose piRound property has not been set */
				question.setPiRound(1);
			}

			if (question.getImage() != null) {
				question.setImage("true");
			}

			result.add(question);
		}
		return result;
	}

	@Override
	@Cacheable(cacheNames = "motdlist", key = "#p0")
	public MotdList getMotdListForUser(final String username) {
		View view = new View("motdlist/doc_by_username");
		view.setKey(username);

		ViewResults results = this.getDatabase().view(view);

		MotdList motdlist = new MotdList();
		for (final Document d : results.getResults()) {
			motdlist.set_id(d.getId());
			motdlist.set_rev(d.getJSONObject("value").getString("_rev"));
			motdlist.setUsername(d.getJSONObject("value").getString("username"));
			motdlist.setMotdkeys(d.getJSONObject("value").getString("motdkeys"));
		}
		return motdlist;
	}

	@Override
	@CachePut(cacheNames = "motdlist", key = "#p0.username")
	public MotdList createOrUpdateMotdList(MotdList motdlist) {
		try {
			String id = motdlist.get_id();
			String rev = motdlist.get_rev();
			Document d = new Document();

			if (null != id) {
				d = database.getDocument(id, rev);
			}
			d.put("type", "motdlist");
			d.put("username", motdlist.getUsername());
			d.put("motdkeys", motdlist.getMotdkeys());

			database.saveDocument(d, id);
			motdlist.set_id(d.getId());
			motdlist.set_rev(d.getRev());

			return motdlist;
		} catch (IOException e) {
			logger.error("Could not save MotD list {}.", motdlist, e);
		}

		return null;
	}

}
