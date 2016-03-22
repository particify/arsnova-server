/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2016 The ARSnova Team
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
import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.domain.CourseScore;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.DbUser;
import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Motd;
import de.thm.arsnova.entities.MotdList;
import de.thm.arsnova.entities.PossibleAnswer;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.SessionInfo;
import de.thm.arsnova.entities.SortOrder;
import de.thm.arsnova.entities.Statistics;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.VisitedSession;
import de.thm.arsnova.entities.transport.AnswerQueueElement;
import de.thm.arsnova.entities.transport.ImportExportSession;
import de.thm.arsnova.entities.transport.ImportExportSession.ImportExportQuestion;
import de.thm.arsnova.events.NewAnswerEvent;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.services.ISessionService;
import net.sf.ezmorph.Morpher;
import net.sf.ezmorph.MorpherRegistry;
import net.sf.ezmorph.bean.BeanMorpher;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
@Component("databaseDao")
public class CouchDBDao implements IDatabaseDao, ApplicationEventPublisherAware {

	@Autowired
	private ISessionService sessionService;

	private String databaseHost;
	private int databasePort;
	private String databaseName;
	private Database database;

	private ApplicationEventPublisher publisher;

	private final Queue<AbstractMap.SimpleEntry<Document, AnswerQueueElement>> answerQueue = new ConcurrentLinkedQueue<AbstractMap.SimpleEntry<Document, AnswerQueueElement>>();

	public static final Logger LOGGER = LoggerFactory.getLogger(CouchDBDao.class);

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
	 * Allows access to the proxy object. It has to be used instead of <code>this</code> for local calls to public
	 * methods for caching purposes. This is an ugly but necessary temporary workaround until a better solution is
	 * implemented (e.g. use of AspectJ's weaving).
	 * @return the proxy for CouchDBDao
	 */
	private IDatabaseDao getDatabaseDao() {
		return (IDatabaseDao) AopContext.currentProxy();
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public List<Session> getMySessions(final User user, final int start, final int limit) {
		final NovaView view = new NovaView("session/by_creator");
		if (start > 0) {
			view.setSkip(start);
		}
		if (limit > 0) {
			view.setLimit(limit);
		}
		view.setStartKeyArray(user.getUsername());
		view.setEndKeyArray(user.getUsername(), "{}");

		final Results<Session> results = getDatabase().queryView(view, Session.class);

		final List<Session> result = new ArrayList<Session>();
		for (final RowResult<Session> row : results.getRows()) {
			final Session session = row.getValue();
			session.setCreator(row.getKey().getString(0));
			session.setName(row.getKey().getString(1));
			session.set_id(row.getId());
			result.add(session);
		}
		return result;
	}

	@Override
	public List<Session> getPublicPoolSessions() {
		final NovaView view = new NovaView("session/public_pool_by_subject");

		final ViewResults sessions = getDatabase().view(view);

		final List<Session> result = new ArrayList<Session>();

		for (final Document d : sessions.getResults()) {
			final Session session = (Session) JSONObject.toBean(
					d.getJSONObject().getJSONObject("value"),
					Session.class
					);
			session.set_id(d.getId());
			result.add(session);
		}
		return result;
	}

	@Override
	public List<SessionInfo> getPublicPoolSessionsInfo() {
		final List<Session> sessions = this.getPublicPoolSessions();
		return getInfosForSessions(sessions);
	}

	@Override
	public List<Session> getMyPublicPoolSessions(final User user) {
		final NovaView view = new NovaView("session/public_pool_by_creator");
		view.setStartKeyArray(user.getUsername());
		view.setEndKeyArray(user.getUsername(), "{}");

		final ViewResults sessions = getDatabase().view(view);

		final List<Session> result = new ArrayList<Session>();
		for (final Document d : sessions.getResults()) {
			final Session session = (Session) JSONObject.toBean(
					d.getJSONObject().getJSONObject("value"),
					Session.class
					);
			session.setCreator(d.getJSONObject().getJSONArray("key").getString(0));
			session.setName(d.getJSONObject().getJSONArray("key").getString(1));
			session.set_id(d.getId());
			result.add(session);
		}
		return result;
	}

	@Override
	public List<SessionInfo> getMyPublicPoolSessionsInfo(final User user) {
		final List<Session> sessions = this.getMyPublicPoolSessions(user);
		if (sessions.isEmpty()) {
			return new ArrayList<SessionInfo>();
		}
		return getInfosForSessions(sessions);
	}

	@Override
	public List<SessionInfo> getMySessionsInfo(final User user, final int start, final int limit) {
		final List<Session> sessions = this.getMySessions(user, start, limit);
		if (sessions.isEmpty()) {
			return new ArrayList<SessionInfo>();
		}
		return getInfosForSessions(sessions);
	}

	private List<SessionInfo> getInfosForSessions(final List<Session> sessions) {
		final ExtendedView questionCountView = new ExtendedView("skill_question/count_by_session");
		final ExtendedView answerCountView = new ExtendedView("skill_question/count_answers_by_session");
		final ExtendedView interposedCountView = new ExtendedView("interposed_question/count_by_session");
		final ExtendedView unredInterposedCountView = new ExtendedView("interposed_question/count_by_session_reading");

		interposedCountView.setSessionIdKeys(sessions);
		interposedCountView.setGroup(true);
		questionCountView.setSessionIdKeys(sessions);
		questionCountView.setGroup(true);
		answerCountView.setSessionIdKeys(sessions);
		answerCountView.setGroup(true);
		List<String> unredInterposedQueryKeys = new ArrayList<String>();
		for (Session s : sessions) {
			unredInterposedQueryKeys.add("[\"" + s.get_id() + "\",\"unread\"]");
		}
		unredInterposedCountView.setKeys(unredInterposedQueryKeys);
		unredInterposedCountView.setGroup(true);
		return getSessionInfoData(sessions, questionCountView, answerCountView, interposedCountView, unredInterposedCountView);
	}

	private List<SessionInfo> getInfosForVisitedSessions(final List<Session> sessions, final User user) {
		final ExtendedView answeredQuestionsView = new ExtendedView("answer/by_user");
		final ExtendedView questionIdsView = new ExtendedView("skill_question/by_session_only_id_for_all");
		questionIdsView.setSessionIdKeys(sessions);
		List<String> answeredQuestionQueryKeys = new ArrayList<String>();
		for (Session s : sessions) {
			answeredQuestionQueryKeys.add("[\"" + user.getUsername() + "\",\"" + s.get_id() + "\"]");
		}
		answeredQuestionsView.setKeys(answeredQuestionQueryKeys);
		return getVisitedSessionInfoData(sessions, answeredQuestionsView, questionIdsView);
	}

	private List<SessionInfo> getVisitedSessionInfoData(List<Session> sessions,
			ExtendedView answeredQuestionsView, ExtendedView questionIdsView) {
		final Map<String, Set<String>> answeredQuestionsMap = new HashMap<String, Set<String>>();
		final Map<String, Set<String>> questionIdMap = new HashMap<String, Set<String>>();
		final ViewResults answeredQuestionsViewResults = getDatabase().view(answeredQuestionsView);
		final ViewResults questionIdsViewResults = getDatabase().view(questionIdsView);

		// Maps a session ID to a set of question IDs of answered questions of that session
		for (final Document d : answeredQuestionsViewResults.getResults()) {
			final String sessionId = d.getJSONArray("key").getString(1);
			final String questionId = d.getString("value");
			Set<String> questionIdsInSession = answeredQuestionsMap.get(sessionId);
			if (questionIdsInSession == null) {
				questionIdsInSession = new HashSet<String>();
			}
			questionIdsInSession.add(questionId);
			answeredQuestionsMap.put(sessionId, questionIdsInSession);
		}

		// Maps a session ID to a set of question IDs of that session
		for (final Document d : questionIdsViewResults.getResults()) {
			final String sessionId = d.getString("key");
			final String questionId = d.getId();
			Set<String> questionIdsInSession = questionIdMap.get(sessionId);
			if (questionIdsInSession == null) {
				questionIdsInSession = new HashSet<String>();
			}
			questionIdsInSession.add(questionId);
			questionIdMap.put(sessionId, questionIdsInSession);
		}

		// For each session, count the question IDs that are not yet answered
		Map<String, Integer> unansweredQuestionsCountMap = new HashMap<String, Integer>();
		for (final Session s : sessions) {
			if (!questionIdMap.containsKey(s.get_id())) {
				continue;
			}
			// Note: create a copy of the first set so that we don't modify the contents in the original set
			Set<String> questionIdsInSession = new HashSet<String>(questionIdMap.get(s.get_id()));
			Set<String> answeredQuestionIdsInSession = answeredQuestionsMap.get(s.get_id());
			if (answeredQuestionIdsInSession == null) {
				answeredQuestionIdsInSession = new HashSet<String>();
			}
			questionIdsInSession.removeAll(answeredQuestionIdsInSession);
			unansweredQuestionsCountMap.put(s.get_id(), questionIdsInSession.size());
		}

		List<SessionInfo> sessionInfos = new ArrayList<SessionInfo>();
		for (Session session : sessions) {
			int numUnanswered = 0;

			if (unansweredQuestionsCountMap.containsKey(session.get_id())) {
				numUnanswered = unansweredQuestionsCountMap.get(session.get_id());
			}
			SessionInfo info = new SessionInfo(session);
			info.setNumUnanswered(numUnanswered);
			sessionInfos.add(info);
		}
		return sessionInfos;
	}

	private List<SessionInfo> getSessionInfoData(final List<Session> sessions,
			final ExtendedView questionCountView,
			final ExtendedView answerCountView,
			final ExtendedView interposedCountView,
			final ExtendedView unredInterposedCountView) {
		final ViewResults questionCountViewResults = getDatabase().view(questionCountView);
		final ViewResults answerCountViewResults = getDatabase().view(answerCountView);
		final ViewResults interposedCountViewResults = getDatabase().view(interposedCountView);
		final ViewResults unredInterposedCountViewResults = getDatabase().view(unredInterposedCountView);

		Map<String, Integer> questionCountMap = new HashMap<String, Integer>();
		for (final Document d : questionCountViewResults.getResults()) {
			questionCountMap.put(d.getString("key"), d.getInt("value"));
		}
		Map<String, Integer> answerCountMap = new HashMap<String, Integer>();
		for (final Document d : answerCountViewResults.getResults()) {
			answerCountMap.put(d.getString("key"), d.getInt("value"));
		}
		Map<String, Integer> interposedCountMap = new HashMap<String, Integer>();
		for (final Document d : interposedCountViewResults.getResults()) {
			interposedCountMap.put(d.getString("key"), d.getInt("value"));
		}
		Map<String, Integer> unredInterposedCountMap = new HashMap<String, Integer>();
		for (final Document d : unredInterposedCountViewResults.getResults()) {
			unredInterposedCountMap.put(d.getJSONArray("key").getString(0), d.getInt("value"));
		}

		List<SessionInfo> sessionInfos = new ArrayList<SessionInfo>();
		for (Session session : sessions) {
			int numQuestions = 0;
			int numAnswers = 0;
			int numInterposed = 0;
			int numUnredInterposed = 0;
			if (questionCountMap.containsKey(session.get_id())) {
				numQuestions = questionCountMap.get(session.get_id());
			}
			if (answerCountMap.containsKey(session.get_id())) {
				numAnswers = answerCountMap.get(session.get_id());
			}
			if (interposedCountMap.containsKey(session.get_id())) {
				numInterposed = interposedCountMap.get(session.get_id());
			}
			if (unredInterposedCountMap.containsKey(session.get_id())) {
				numUnredInterposed = unredInterposedCountMap.get(session.get_id());
			}

			SessionInfo info = new SessionInfo(session);
			info.setNumQuestions(numQuestions);
			info.setNumAnswers(numAnswers);
			info.setNumInterposed(numInterposed);
			info.setNumUnredInterposed(numUnredInterposed);
			sessionInfos.add(info);
		}
		return sessionInfos;
	}

	/**
	 * @deprecated The decision to load data depending on the user should be made by a service class, not this
	 * database class. Please use getSkillQuestionsForUsers or getSkillQuestionsForTeachers as this will enable
	 * caching.
	 */
	@Deprecated
	@Override
	public List<Question> getSkillQuestions(final User user, final Session session) {
		String viewName;
		if (session.getCreator().equals(user.getUsername())) {
			viewName = "skill_question/by_session_sorted_by_subject_and_text";
		} else {
			viewName = "skill_question/by_session_for_all_full";
		}
		return getQuestions(new NovaView(viewName), session);
	}

	@Cacheable("skillquestions")
	@Override
	public List<Question> getSkillQuestionsForUsers(final Session session) {
		String viewName = "skill_question/by_session_for_all_full";
		NovaView view = new NovaView(viewName);

		return getQuestions(view, session);
	}

	@Cacheable("skillquestions")
	@Override
	public List<Question> getSkillQuestionsForTeachers(final Session session) {
		String viewName = "skill_question/by_session_sorted_by_subject_and_text";
		NovaView view = new NovaView(viewName);

		return getQuestions(view, session);
	}

	@Override
	public int getSkillQuestionCount(final Session session) {
		return getQuestionCount(new NovaView("skill_question/count_by_session"), session);
	}

	@Override
	@Cacheable("sessions")
	public Session getSessionFromKeyword(final String keyword) {
		final NovaView view = new NovaView("session/by_keyword");
		view.setKey(keyword);
		final ViewResults results = getDatabase().view(view);

		if (results.getJSONArray("rows").optJSONObject(0) == null) {
			throw new NotFoundException();
		}
		return (Session) JSONObject.toBean(
				results.getJSONArray("rows").optJSONObject(0).optJSONObject("value"),
				Session.class
				);
	}

	@Override
	@Cacheable("sessions")
	public Session getSessionFromId(final String sessionId) {
		final NovaView view = new NovaView("session/by_id");
		view.setKey(sessionId);

		final ViewResults sessions = getDatabase().view(view);

		if (sessions.getJSONArray("rows").optJSONObject(0) == null) {
			return null;
		}
		return (Session) JSONObject.toBean(
				sessions.getJSONArray("rows").optJSONObject(0).optJSONObject("value"),
				Session.class
				);
	}

	@Override
	public Session saveSession(final User user, final Session session) {
		final Document sessionDocument = new Document();
		sessionDocument.put("type", "session");
		sessionDocument.put("name", session.getName());
		sessionDocument.put("shortName", session.getShortName());
		sessionDocument.put("keyword", sessionService.generateKeyword());
		sessionDocument.put("creator", user.getUsername());
		sessionDocument.put("active", true);
		sessionDocument.put("courseType", session.getCourseType());
		sessionDocument.put("courseId", session.getCourseId());
		sessionDocument.put("creationTime", session.getCreationTime());
		sessionDocument.put("learningProgressOptions", JSONObject.fromObject(session.getLearningProgressOptions()));
		sessionDocument.put("ppAuthorName", session.getPpAuthorName());
		sessionDocument.put("ppAuthorMail", session.getPpAuthorMail());
		sessionDocument.put("ppUniversity", session.getPpUniversity());
		sessionDocument.put("ppLogo", session.getPpLogo());
		sessionDocument.put("ppSubject", session.getPpSubject());
		sessionDocument.put("ppLicense", session.getPpLicense());
		sessionDocument.put("ppDescription", session.getPpDescription());
		sessionDocument.put("ppFaculty", session.getPpFaculty());
		sessionDocument.put("ppLevel", session.getPpLevel());
		sessionDocument.put("sessionType", session.getSessionType());
		sessionDocument.put("features", JSONObject.fromObject(session.getFeatures()));
		sessionDocument.put("feedbackLock", false);
		try {
			database.saveDocument(sessionDocument);
		} catch (final IOException e) {
			return null;
		}
		// session caching is done by loading the created session
		return getSessionFromKeyword(sessionDocument.getString("keyword"));
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public boolean sessionKeyAvailable(final String keyword) {
		final View view = new View("session/by_keyword");
		final ViewResults results = getDatabase().view(view);

		return !results.containsKey(keyword);
	}

	private String getSessionKeyword(final String internalSessionId) throws IOException {
		final Document document = getDatabase().getDocument(internalSessionId);
		if (document.has("keyword")) {
			return (String) document.get("keyword");
		}
		LOGGER.error("No session found for internal id: {}", internalSessionId);
		return null;
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
				LOGGER.error(
						"Cannot connect to CouchDB database '" + databaseName
						+ "' on host '" + databaseHost
						+ "' using port " + databasePort
						);
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
			LOGGER.error("Could not save question {}", question);
		}
		return null;
	}

	private Document toQuestionDocument(final Session session, final Question question) {
		Document q = new Document();

		question.updateRoundManagementState();
		q.put("type", "skill_question");
		q.put("questionType", question.getQuestionType());
		q.put("questionVariant", question.getQuestionVariant());
		q.put("sessionId", session.get_id());
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
			LOGGER.error("Could not update question {}", question);
		}

		return null;
	}

	@Override
	public InterposedQuestion saveQuestion(final Session session, final InterposedQuestion question, User user) {
		final Document q = new Document();
		q.put("type", "interposed_question");
		q.put("sessionId", session.get_id());
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
			LOGGER.error("Could not save interposed question {}", question);
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
			question.setPossibleAnswers(new ArrayList<PossibleAnswer>(answers));
			question.setSessionKeyword(getSessionKeyword(question.getSessionId()));
			return question;
		} catch (final IOException e) {
			LOGGER.error("Could not get question with id {}", id);
		}
		return null;
	}

	@Override
	public LoggedIn registerAsOnlineUser(final User user, final Session session) {
		try {
			final NovaView view = new NovaView("logged_in/all");
			view.setKey(user.getUsername());
			final ViewResults results = getDatabase().view(view);

			LoggedIn loggedIn = new LoggedIn();
			if (results.getJSONArray("rows").optJSONObject(0) != null) {
				final JSONObject json = results.getJSONArray("rows").optJSONObject(0).optJSONObject("value");
				loggedIn = (LoggedIn) JSONObject.toBean(json, LoggedIn.class);
				final JSONArray vs = json.optJSONArray("visitedSessions");
				if (vs != null) {
					@SuppressWarnings("unchecked")
					final Collection<VisitedSession> visitedSessions = JSONArray.toCollection(vs, VisitedSession.class);
					loggedIn.setVisitedSessions(new ArrayList<VisitedSession>(visitedSessions));
				}

				/* Do not clutter CouchDB. Only update once every 3 hours per session. */
				if (loggedIn.getSessionId().equals(session.get_id()) && loggedIn.getTimestamp() > System.currentTimeMillis() - 3 * 3600000) {
					return loggedIn;
				}
			}

			loggedIn.setUser(user.getUsername());
			loggedIn.setSessionId(session.get_id());
			loggedIn.addVisitedSession(session);
			loggedIn.updateTimestamp();

			final JSONObject json = JSONObject.fromObject(loggedIn);
			final Document doc = new Document(json);
			if (doc.getId().isEmpty()) {
				// If this is a new user without a logged_in document, we have
				// to remove the following
				// pre-filled fields. Otherwise, CouchDB will take these empty
				// fields as genuine
				// identifiers, and will throw errors afterwards.
				doc.remove("_id");
				doc.remove("_rev");
			}
			getDatabase().saveDocument(doc);

			final LoggedIn l = (LoggedIn) JSONObject.toBean(doc.getJSONObject(), LoggedIn.class);
			final JSONArray vs = doc.getJSONObject().optJSONArray("visitedSessions");
			if (vs != null) {
				@SuppressWarnings("unchecked")
				final Collection<VisitedSession> visitedSessions = JSONArray.toCollection(vs, VisitedSession.class);
				l.setVisitedSessions(new ArrayList<VisitedSession>(visitedSessions));
			}
			return l;
		} catch (final IOException e) {
			return null;
		}
	}

	@Override
	@CachePut(value = "sessions")
	public Session updateSessionOwnerActivity(final Session session) {
		try {
			/* Do not clutter CouchDB. Only update once every 3 hours. */
			if (session.getLastOwnerActivity() > System.currentTimeMillis() - 3 * 3600000) {
				return session;
			}

			session.setLastOwnerActivity(System.currentTimeMillis());
			final JSONObject json = JSONObject.fromObject(session);
			getDatabase().saveDocument(new Document(json));
			return session;
		} catch (final IOException e) {
			LOGGER.error("Failed to update lastOwnerActivity for Session {}", session);
			return session;
		}
	}

	@Override
	public List<String> getQuestionIds(final Session session, final User user) {
		NovaView view = new NovaView("skill_question/by_session_only_id_for_all");
		view.setKey(session.get_id());
		return collectQuestionIds(view);
	}

	/* TODO: Only evict cache entry for the question's session. This requires some refactoring. */
	@Caching(evict = { @CacheEvict(value = "questions", key = "#question._id"),
			@CacheEvict(value = "skillquestions", allEntries = true),
			@CacheEvict(value = "lecturequestions", allEntries = true, condition = "#question.getQuestionVariant().equals('lecture')"),
			@CacheEvict(value = "preparationquestions", allEntries = true, condition = "#question.getQuestionVariant().equals('preparation')"),
			@CacheEvict(value = "flashcardquestions", allEntries = true, condition = "#question.getQuestionVariant().equals('flashcard')") })
	@Override
	public void deleteQuestionWithAnswers(final Question question) {
		try {
			deleteAnswers(question);
			deleteDocument(question.get_id());
		} catch (final IOException e) {
			LOGGER.error("IOException: Could not delete question {}", question.get_id());
		}
	}

	@Caching(evict = { @CacheEvict(value = "questions", allEntries = true),
			@CacheEvict(value = "skillquestions", key = "#session"),
			@CacheEvict(value = "lecturequestions", key = "#session"),
			@CacheEvict(value = "preparationquestions", key = "#session"),
			@CacheEvict(value = "flashcardquestions", key = "#session") })
	@Override
	public void deleteAllQuestionsWithAnswers(final Session session) {
		final NovaView view = new NovaView("skill_question/by_session");
		deleteAllQuestionDocumentsWithAnswers(session, view);
	}

	private void deleteAllQuestionDocumentsWithAnswers(final Session session, final NovaView view) {
		view.setStartKeyArray(session.get_id());
		view.setEndKey(session.get_id(), "{}");
		final ViewResults results = getDatabase().view(view);

		List<Question> questions = new ArrayList<Question>();
		for (final Document d : results.getResults()) {
			final Question q = new Question();
			q.set_id(d.getId());
			q.set_rev(d.getJSONObject("value").getString("_rev"));
			questions.add(q);
		}
		deleteAllAnswersWithQuestions(questions);
	}

	private void deleteDocument(final String documentId) throws IOException {
		final Document d = getDatabase().getDocument(documentId);
		getDatabase().deleteDocument(d);
	}

	@CacheEvict("answers")
	@Override
	public void deleteAnswers(final Question question) {
		try {
			final NovaView view = new NovaView("answer/cleanup");
			view.setKey(question.get_id());
			view.setIncludeDocs(true);
			final ViewResults results = getDatabase().view(view);

			List<Document> answersToDelete = new ArrayList<Document>();
			for (final Document a : results.getResults()) {
				final Document d = new Document(a.getJSONObject("doc"));
				d.put("_deleted", true);
				answersToDelete.add(d);
			}
			database.bulkSaveDocuments(answersToDelete.toArray(new Document[answersToDelete.size()]));
		} catch (final IOException e) {
			LOGGER.error("IOException: Could not delete answers for question {}", question.get_id());
		}
	}

	@Override
	public List<String> getUnAnsweredQuestionIds(final Session session, final User user) {
		final NovaView view = new NovaView("answer/by_user");
		view.setKey(user.getUsername(), session.get_id());
		return collectUnansweredQuestionIds(getQuestionIds(session, user), view);
	}

	@Override
	public Answer getMyAnswer(final User me, final String questionId, final int piRound) {

		final NovaView view = new NovaView("answer/by_question_and_user_and_piround");
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
		T obj = null;
		try {
			final Document doc = getDatabase().getDocument(documentId);
			if (doc == null) {
				return null;
			}
			// TODO: This needs some more error checking...
			obj = (T) JSONObject.toBean(doc.getJSONObject(), klass);
		} catch (IOException e) {
			return null;
		} catch (ClassCastException e) {
			return null;
		} catch (net.sf.json.JSONException e) {
			return null;
		}
		return obj;
	}

	@Override
	public List<Answer> getAnswers(final Question question, final int piRound) {
		final String questionId = question.get_id();
		final NovaView view = new NovaView("skill_question/count_answers_by_question_and_piround");
		if (2 == piRound) {
			view.setStartKey(questionId, "2");
			view.setEndKey(questionId, "2", "{}");
		} else {
			/* needed for legacy questions whose piRound property has not been set */
			view.setStartKeyArray(questionId);
			view.setEndKeyArray(questionId, "1", "{}");
		}
		view.setGroup(true);
		final ViewResults results = getDatabase().view(view);
		final int abstentionCount = getDatabaseDao().getAbstentionAnswerCount(questionId);
		final List<Answer> answers = new ArrayList<Answer>();

		for (final Document d : results.getResults()) {
			final Answer a = new Answer();
			a.setAnswerCount(d.getInt("value"));
			a.setAbstentionCount(abstentionCount);
			a.setQuestionId(d.getJSONObject().getJSONArray("key").getString(0));
			a.setPiRound(piRound);
			final String answerText = d.getJSONObject().getJSONArray("key").getString(2);
			a.setAnswerText("null".equals(answerText) ? null : answerText);
			answers.add(a);
		}
		return answers;
	}

	@Override
	public List<Answer> getAllAnswers(final Question question) {
		final String questionId = question.get_id();
		final NovaView view = new NovaView("skill_question/count_all_answers_by_question");
		view.setStartKeyArray(questionId);
		view.setEndKeyArray(questionId, "{}");
		view.setGroup(true);
		final ViewResults results = getDatabase().view(view);
		final int abstentionCount = getDatabaseDao().getAbstentionAnswerCount(questionId);

		final List<Answer> answers = new ArrayList<Answer>();
		for (final Document d : results.getResults()) {
			final Answer a = new Answer();
			a.setAnswerCount(d.getInt("value"));
			a.setAbstentionCount(abstentionCount);
			a.setQuestionId(d.getJSONObject().getJSONArray("key").getString(0));
			final String answerText = d.getJSONObject().getJSONArray("key").getString(1);
			a.setAnswerText("null".equals(answerText) ? null : answerText);
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
		final NovaView view = new NovaView("skill_question/count_abstention_answers_by_question");
		view.setKey(questionId);
		view.setGroup(true);
		final ViewResults results = getDatabase().view(view);
		if (results.getResults().size() == 0) {
			return 0;
		}
		return results.getJSONArray("rows").optJSONObject(0).optInt("value");
	}

	@Override
	public int getAnswerCount(final Question question, final int piRound) {
		final NovaView view = new NovaView("skill_question/count_total_answers_by_question_and_piround");
		view.setGroup(true);
		view.setStartKey(question.get_id(), String.valueOf(piRound));
		view.setEndKey(question.get_id(), String.valueOf(piRound), "{}");
		final ViewResults results = getDatabase().view(view);
		if (results.getResults().size() == 0) {
			return 0;
		}

		return results.getJSONArray("rows").optJSONObject(0).optInt("value");
	}

	@Override
	public int getTotalAnswerCountByQuestion(final Question question) {
		final NovaView view = new NovaView("skill_question/count_total_answers_by_question");
		view.setGroup(true);
		view.setKey(question.get_id());
		final ViewResults results = getDatabase().view(view);

		if (results.getResults().size() == 0) {
			return 0;
		}

		return results.getJSONArray("rows").optJSONObject(0).optInt("value");
	}

	private boolean isEmptyResults(final ViewResults results) {
		return results == null || results.getResults().isEmpty() || results.getJSONArray("rows").size() == 0;
	}

	@Override
	public List<Answer> getFreetextAnswers(final String questionId, final int start, final int limit) {
		final List<Answer> answers = new ArrayList<Answer>();
		final NovaView view = new NovaView("skill_question/freetext_answers_full");
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
		final NovaView view = new NovaView("answer/by_user_and_session_full");
		view.setKey(me.getUsername(), s.get_id());
		final ViewResults results = getDatabase().view(view);
		final List<Answer> answers = new ArrayList<Answer>();
		if (results == null || results.getResults() == null || results.getResults().isEmpty()) {
			return answers;
		}
		for (final Document d : results.getResults()) {
			final Answer a = (Answer) JSONObject.toBean(d.getJSONObject().getJSONObject("value"), Answer.class);
			a.set_id(d.getId());
			a.set_rev(d.getRev());
			a.setUser(me.getUsername());
			a.setSessionId(s.get_id());
			answers.add(a);
		}
		return answers;
	}

	@Override
	public int getTotalAnswerCount(final String sessionKey) {
		final Session s = getDatabaseDao().getSessionFromKeyword(sessionKey);
		if (s == null) {
			throw new NotFoundException();
		}

		final NovaView view = new NovaView("skill_question/count_answers_by_session");
		view.setKey(s.get_id());
		final ViewResults results = getDatabase().view(view);
		if (results.getResults().size() == 0) {
			return 0;
		}
		return results.getJSONArray("rows").optJSONObject(0).optInt("value");
	}

	@Override
	public int getInterposedCount(final String sessionKey) {
		final Session s = getDatabaseDao().getSessionFromKeyword(sessionKey);
		if (s == null) {
			throw new NotFoundException();
		}

		final NovaView view = new NovaView("interposed_question/count_by_session");
		view.setKey(s.get_id());
		view.setGroup(true);
		final ViewResults results = getDatabase().view(view);
		if (results.size() == 0 || results.getResults().size() == 0) {
			return 0;
		}
		return results.getJSONArray("rows").optJSONObject(0).optInt("value");
	}

	@Override
	public InterposedReadingCount getInterposedReadingCount(final Session session) {
		final NovaView view = new NovaView("interposed_question/count_by_session_reading");
		view.setStartKeyArray(session.get_id());
		view.setEndKeyArray(session.get_id(), "{}");
		view.setGroup(true);
		return getInterposedReadingCount(view);
	}

	@Override
	public InterposedReadingCount getInterposedReadingCount(final Session session, final User user) {
		final NovaView view = new NovaView("interposed_question/count_by_session_reading_for_creator");
		view.setStartKeyArray(session.get_id(), user.getUsername());
		view.setEndKeyArray(session.get_id(), user.getUsername(), "{}");
		view.setGroup(true);
		return getInterposedReadingCount(view);
	}

	private InterposedReadingCount getInterposedReadingCount(final NovaView view) {
		final ViewResults results = getDatabase().view(view);
		if (results.size() == 0 || results.getResults().size() == 0) {
			return new InterposedReadingCount();
		}
		// A complete result looks like this. Note that the second row is optional, and that the first one may be
		// 'unread' or 'read', i.e., results may be switched around or only one result may be present.
		// count = {"rows":[
		// {"key":["cecebabb21b096e592d81f9c1322b877","Guestc9350cf4a3","read"],"value":1},
		// {"key":["cecebabb21b096e592d81f9c1322b877","Guestc9350cf4a3","unread"],"value":1}
		// ]}
		int read = 0, unread = 0;
		String type = "";
		final JSONObject fst = results.getJSONArray("rows").getJSONObject(0);
		final JSONObject snd = results.getJSONArray("rows").optJSONObject(1);

		final JSONArray fstkey = fst.getJSONArray("key");
		if (fstkey.size() == 2) {
			type = fstkey.getString(1);
		} else if (fstkey.size() == 3) {
			type = fstkey.getString(2);
		}
		if (type.equals("read")) {
			read = fst.optInt("value");
		} else if (type.equals("unread")) {
			unread = fst.optInt("value");
		}

		if (snd != null) {
			final JSONArray sndkey = snd.getJSONArray("key");
			if (sndkey.size() == 2) {
				type = sndkey.getString(1);
			} else {
				type = sndkey.getString(2);
			}
			if (type.equals("read")) {
				read = snd.optInt("value");
			} else if (type.equals("unread")) {
				unread = snd.optInt("value");
			}
		}
		return new InterposedReadingCount(read, unread);
	}

	@Override
	public List<InterposedQuestion> getInterposedQuestions(final Session session, final int start, final int limit) {
		final NovaView view = new NovaView("interposed_question/by_session_full");
		if (start > 0) {
			view.setSkip(start);
		}
		if (limit > 0) {
			view.setLimit(limit);
		}
		view.setDescending(true);
		view.setStartKeyArray(session.get_id(), "{}");
		view.setEndKeyArray(session.get_id());
		final ViewResults questions = getDatabase().view(view);
		if (questions == null || questions.isEmpty()) {
			return null;
		}
		return createInterposedList(session, questions);
	}

	@Override
	public List<InterposedQuestion> getInterposedQuestions(final Session session, final User user, final int start, final int limit) {
		final NovaView view = new NovaView("interposed_question/by_session_and_creator");
		if (start > 0) {
			view.setSkip(start);
		}
		if (limit > 0) {
			view.setLimit(limit);
		}
		view.setDescending(true);
		view.setStartKeyArray(session.get_id(), user.getUsername(), "{}");
		view.setEndKeyArray(session.get_id(), user.getUsername());
		final ViewResults questions = getDatabase().view(view);
		if (questions == null || questions.isEmpty()) {
			return null;
		}
		return createInterposedList(session, questions);
	}

	private List<InterposedQuestion> createInterposedList(
			final Session session, final ViewResults questions) {
		final List<InterposedQuestion> result = new ArrayList<InterposedQuestion>();
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
					}
				}
			}
			if (!isEmptyResults(creatorResults)) {
				final JSONArray rows = creatorResults.getJSONArray("rows");
				Set<String> creators = new HashSet<String>();
				for (int i = 0; i < rows.size(); i++) {
					final JSONObject row = rows.getJSONObject(i);
					creators.add(row.getString("key"));
				}
				stats.setCreators(creators.size());
			}
			if (!isEmptyResults(studentUserResults)) {
				final JSONArray rows = studentUserResults.getJSONArray("rows");
				Set<String> students = new HashSet<String>();
				for (int i = 0; i < rows.size(); i++) {
					final JSONObject row = rows.getJSONObject(i);
					students.add(row.getString("key"));
				}
				stats.setActiveStudents(students.size());
			}
			return stats;
		} catch (final Exception e) {
			LOGGER.error("Error while retrieving session count", e);
		}
		return stats;
	}

	@Override
	public InterposedQuestion getInterposedQuestion(final String questionId) {
		try {
			final Document document = getDatabase().getDocument(questionId);
			final InterposedQuestion question = (InterposedQuestion) JSONObject.toBean(document.getJSONObject(),
					InterposedQuestion.class);
			question.setSessionId(getSessionKeyword(question.getSessionId()));
			return question;
		} catch (final IOException e) {
			LOGGER.error("Could not load interposed question {}", questionId);
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
			LOGGER.error("Coulg not mark interposed question as read {}", question.get_id());
		}
	}

	@Override
	public List<Session> getMyVisitedSessions(final User user, final int start, final int limit) {
		final NovaView view = new NovaView("logged_in/visited_sessions_by_user");
		if (start > 0) {
			view.setSkip(start);
		}
		if (limit > 0) {
			view.setLimit(limit);
		}
		view.setKey(user.getUsername());
		final ViewResults sessions = getDatabase().view(view);
		final List<Session> allSessions = new ArrayList<Session>();
		for (final Document d : sessions.getResults()) {
			// Not all users have visited sessions
			if (d.getJSONObject().optJSONArray("value") != null) {
				@SuppressWarnings("unchecked")
				final Collection<Session> visitedSessions =	 JSONArray.toCollection(
					d.getJSONObject().getJSONArray("value"),
					Session.class
				);
				allSessions.addAll(visitedSessions);
			}
		}
		// Filter sessions that don't exist anymore, also filter my own sessions
		final List<Session> result = new ArrayList<Session>();
		final List<Session> filteredSessions = new ArrayList<Session>();
		for (final Session s : allSessions) {
			try {
				final Session session = getDatabaseDao().getSessionFromKeyword(s.getKeyword());
				if (session != null && !session.isCreator(user)) {
					result.add(session);
				} else {
					filteredSessions.add(s);
				}
			} catch (final NotFoundException e) {
				filteredSessions.add(s);
			}
		}
		if (filteredSessions.isEmpty()) {
			return result;
		}
		// Update document to remove sessions that don't exist anymore
		try {
			List<VisitedSession> visitedSessions = new ArrayList<VisitedSession>();
			for (final Session s : result) {
				visitedSessions.add(new VisitedSession(s));
			}
			final LoggedIn loggedIn = new LoggedIn();
			final Document loggedInDocument = getDatabase().getDocument(sessions.getResults().get(0).getString("id"));
			loggedIn.setSessionId(loggedInDocument.getString("sessionId"));
			loggedIn.setUser(user.getUsername());
			loggedIn.setTimestamp(loggedInDocument.getLong("timestamp"));
			loggedIn.setType(loggedInDocument.getString("type"));
			loggedIn.setVisitedSessions(visitedSessions);
			loggedIn.set_id(loggedInDocument.getId());
			loggedIn.set_rev(loggedInDocument.getRev());

			final JSONObject json = JSONObject.fromObject(loggedIn);
			final Document doc = new Document(json);
			getDatabase().saveDocument(doc);
		} catch (IOException e) {
			LOGGER.error("Could not clean up logged_in document of {}", user.getUsername());
		}
		return result;
	}

	@Override
	public List<SessionInfo> getMyVisitedSessionsInfo(final User user, final int start, final int limit) {
		List<Session> sessions = this.getMyVisitedSessions(user, start, limit);
		if (sessions.isEmpty()) {
			return new ArrayList<SessionInfo>();
		}
		return this.getInfosForVisitedSessions(sessions, user);
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
		a.put("timestamp", answer.getTimestamp());
		a.put("user", user.getUsername());
		a.put("piRound", answer.getPiRound());
		a.put("abstention", answer.isAbstention());
		a.put("answerImage", answer.getAnswerImage());
		a.put("answerThumbnailImage", answer.getAnswerThumbnailImage());
		AnswerQueueElement answerQueueElement = new AnswerQueueElement(session, question, answer, user);
		this.answerQueue.offer(new AbstractMap.SimpleEntry<Document, AnswerQueueElement>(a, answerQueueElement));
		return answer;
	}

	@Scheduled(fixedDelay = 5000)
	public void flushAnswerQueue() {
		final Map<Document, Answer> map = new HashMap<Document, Answer>();
		final List<Document> answerList = new ArrayList<Document>();
		final List<AnswerQueueElement> elements = new ArrayList<AnswerQueueElement>();
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
			LOGGER.error("Could not bulk save answers from queue");
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
			LOGGER.error("Could not save answer {}", answer);
		}
		return null;
	}

	/* TODO: Only evict cache entry for the answer's session. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public void deleteAnswer(final String answerId) {
		try {
			database.deleteDocument(database.getDocument(answerId));
		} catch (final IOException e) {
			LOGGER.error("Could not delete answer {} because of {}", answerId, e.getMessage());
		}
	}

	@Override
	public void deleteInterposedQuestion(final InterposedQuestion question) {
		try {
			deleteDocument(question.get_id());
		} catch (final IOException e) {
			LOGGER.error("Could not delete interposed question {} because of {}", question.get_id(), e.getMessage());
		}
	}

	@Override
	public List<Session> getCourseSessions(final List<Course> courses) {
		final ExtendedView view = new ExtendedView("session/by_courseid");
		view.setCourseIdKeys(courses);

		final ViewResults sessions = getDatabase().view(view);

		final List<Session> result = new ArrayList<Session>();
		for (final Document d : sessions.getResults()) {
			final Session session = (Session) JSONObject.toBean(
					d.getJSONObject().getJSONObject("value"),
					Session.class
					);
			result.add(session);
		}
		return result;
	}

	/**
	 * Adds convenience methods to CouchDB4J's view class.
	 */
	private static class ExtendedView extends NovaView {

		public ExtendedView(final String fullname) {
			super(fullname);
		}

		public void setCourseIdKeys(final List<Course> courses) {
			List<String> courseIds = new ArrayList<String>();
			for (Course c : courses) {
				courseIds.add(c.getId());
			}
			setKeys(courseIds);
		}

		public void setSessionIdKeys(final List<Session> sessions) {
			List<String> sessionIds = new ArrayList<String>();
			for (Session s : sessions) {
				sessionIds.add(s.get_id());
			}
			setKeys(sessionIds);
		}
	}

	@Override
	@CachePut(value = "sessions")
	public Session updateSession(final Session session) {
		try {
			final Document s = database.getDocument(session.get_id());
			s.put("name", session.getName());
			s.put("shortName", session.getShortName());
			s.put("active", session.isActive());
			s.put("ppAuthorName", session.getPpAuthorName());
			s.put("ppAuthorMail", session.getPpAuthorMail());
			s.put("ppUniversity", session.getPpUniversity());
			s.put("ppLogo", session.getPpLogo());
			s.put("ppSubject", session.getPpSubject());
			s.put("ppLicense", session.getPpLicense());
			s.put("ppDescription", session.getPpDescription());
			s.put("ppFaculty", session.getPpFaculty());
			s.put("ppLevel", session.getPpLevel());
			s.put("learningProgressOptions", JSONObject.fromObject(session.getLearningProgressOptions()));
			s.put("features", JSONObject.fromObject(session.getFeatures()));
			s.put("feedbackLock", session.getFeedbackLock());
			database.saveDocument(s);
			session.set_rev(s.getRev());

			return session;
		} catch (final IOException e) {
			LOGGER.error("Could not lock session {}", session);
		}

		return null;
	}

	@Override
	@CacheEvict("sessions")
	public void deleteSession(final Session session) {
		try {
			deleteDocument(session.get_id());
		} catch (final IOException e) {
			LOGGER.error("Could not delete session {}", session);
		}
	}

	@Cacheable("lecturequestions")
	@Override
	public List<Question> getLectureQuestionsForUsers(final Session session) {
		String viewName = "skill_question/lecture_question_by_session_for_all";
		NovaView view = new NovaView(viewName);

		return getQuestions(view, session);
	}

	@Override
	public List<Question> getLectureQuestionsForTeachers(final Session session) {
		String viewName = "skill_question/lecture_question_by_session";
		NovaView view = new NovaView(viewName);

		return getQuestions(view, session);
	}

	@Cacheable("flashcardquestions")
	@Override
	public List<Question> getFlashcardsForUsers(final Session session) {
		String viewName = "skill_question/flashcard_by_session_for_all";
		NovaView view = new NovaView(viewName);

		return getQuestions(view, session);
	}

	@Override
	public List<Question> getFlashcardsForTeachers(final Session session) {
		String viewName = "skill_question/flashcard_by_session";
		NovaView view = new NovaView(viewName);

		return getQuestions(view, session);
	}

	@Cacheable("preparationquestions")
	@Override
	public List<Question> getPreparationQuestionsForUsers(final Session session) {
		String viewName = "skill_question/preparation_question_by_session_for_all";
		NovaView view = new NovaView(viewName);

		return getQuestions(view, session);
	}

	@Override
	public List<Question> getPreparationQuestionsForTeachers(final Session session) {
		String viewName = "skill_question/preparation_question_by_session";
		NovaView view = new NovaView(viewName);

		return getQuestions(view, session);
	}

	@Override
	public List<Question> getAllSkillQuestions(final Session session) {
		final List<Question> questions = getQuestions(new NovaView("skill_question/by_session"), session);
		return questions;
	}

	private List<Question> getQuestions(final NovaView view, final Session session) {
		view.setStartKeyArray(session.get_id());
		view.setEndKeyArray(session.get_id(), "{}");
		final ViewResults viewResults = getDatabase().view(view);
		if (viewResults == null || viewResults.isEmpty()) {
			return null;
		}

		final List<Question> questions = new ArrayList<Question>();

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
		return getQuestionCount(new NovaView("skill_question/lecture_question_count_by_session"), session);
	}

	@Override
	public int getFlashcardCount(final Session session) {
		return getQuestionCount(new NovaView("skill_question/flashcard_count_by_session"), session);
	}

	@Override
	public int getPreparationQuestionCount(final Session session) {
		return getQuestionCount(new NovaView("skill_question/preparation_question_count_by_session"), session);
	}

	private int getQuestionCount(final NovaView view, final Session session) {
		view.setKey(session.get_id());
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
		final NovaView view = new NovaView("skill_question/count_answers_by_session_and_question_variant");
		view.setKey(session.get_id(), variant);
		final ViewResults results = getDatabase().view(view);
		if (results.getResults().size() == 0) {
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
	public void deleteAllLectureQuestionsWithAnswers(final Session session) {
		final NovaView view = new NovaView("skill_question/lecture_question_by_session");
		deleteAllQuestionDocumentsWithAnswers(session, view);
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@Caching(evict = { @CacheEvict(value = "questions", allEntries = true),
			@CacheEvict("skillquestions"),
			@CacheEvict("flashcardquestions"),
			@CacheEvict(value = "answers", allEntries = true)})
	@Override
	public void deleteAllFlashcardsWithAnswers(final Session session) {
		final NovaView view = new NovaView("skill_question/flashcard_by_session");
		deleteAllQuestionDocumentsWithAnswers(session, view);
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@Caching(evict = { @CacheEvict(value = "questions", allEntries = true),
			@CacheEvict("skillquestions"),
			@CacheEvict("preparationquestions"),
			@CacheEvict(value = "answers", allEntries = true)})
	@Override
	public void deleteAllPreparationQuestionsWithAnswers(final Session session) {
		final NovaView view = new NovaView("skill_question/preparation_question_by_session");
		deleteAllQuestionDocumentsWithAnswers(session, view);
	}

	@Override
	public List<String> getUnAnsweredLectureQuestionIds(final Session session, final User user) {
		final NovaView view = new NovaView("answer/variant_by_user_and_piround");
		view.setKey(user.getUsername(), session.get_id(), "lecture");
		return collectUnansweredQuestionIdsByPiRound(getDatabaseDao().getLectureQuestionsForUsers(session), view);
	}

	@Override
	public List<String> getUnAnsweredPreparationQuestionIds(final Session session, final User user) {
		final NovaView view = new NovaView("answer/variant_by_user_and_piround");
		view.setKey(user.getUsername(), session.get_id(), "preparation");
		return collectUnansweredQuestionIdsByPiRound(getDatabaseDao().getPreparationQuestionsForUsers(session), view);
	}

	private List<String> collectUnansweredQuestionIds(
			final List<String> questions,
			final NovaView view
			) {
		final ViewResults answeredQuestions = getDatabase().view(view);

		final List<String> answered = new ArrayList<String>();
		for (final Document d : answeredQuestions.getResults()) {
			answered.add(d.getString("value"));
		}

		final List<String> unanswered = new ArrayList<String>();
		for (final String questionId : questions) {
			if (!answered.contains(questionId)) {
				unanswered.add(questionId);
			}
		}
		return unanswered;
	}

	private List<String> collectUnansweredQuestionIdsByPiRound(
			final List<Question> questions,
			final NovaView view
			) {
		final ViewResults answeredQuestions = getDatabase().view(view);

		final Map<String, Integer> answered = new HashMap<String, Integer>();
		for (final Document d : answeredQuestions.getResults()) {
			answered.put(d.getJSONArray("value").getString(0), d.getJSONArray("value").getInt(1));
		}

		final List<String> unanswered = new ArrayList<String>();

		for (final Question question : questions) {
			if (!answered.containsKey(question.get_id())
					|| (answered.containsKey(question.get_id()) && answered.get(question.get_id()) != question.getPiRound())) {
				unanswered.add(question.get_id());
			}
		}

		return unanswered;
	}

	private List<String> collectQuestionIds(final NovaView view) {
		final ViewResults results = getDatabase().view(view);
		if (results.getResults().size() == 0) {
			return new ArrayList<String>();
		}
		final List<String> ids = new ArrayList<String>();
		for (final Document d : results.getResults()) {
			ids.add(d.getId());
		}
		return ids;
	}

	@Override
	public void deleteAllInterposedQuestions(final Session session) {
		final NovaView view = new NovaView("interposed_question/by_session");
		view.setKey(session.get_id());
		final ViewResults questions = getDatabase().view(view);
		deleteAllInterposedQuestions(session, questions);
	}

	@Override
	public void deleteAllInterposedQuestions(final Session session, final User user) {
		final NovaView view = new NovaView("interposed_question/by_session_and_creator");
		view.setKey(session.get_id(), user.getUsername());
		final ViewResults questions = getDatabase().view(view);
		deleteAllInterposedQuestions(session, questions);
	}

	private void deleteAllInterposedQuestions(final Session session, final ViewResults questions) {
		if (questions == null || questions.isEmpty()) {
			return;
		}
		for (final Document document : questions.getResults()) {
			try {
				deleteDocument(document.getId());
			} catch (final IOException e) {
				LOGGER.error("Could not delete all interposed questions {}", session);
			}
		}
	}

	@Override
	public List<Question> publishAllQuestions(final Session session, final boolean publish) {
		final List<Question> questions = getQuestions(new NovaView("skill_question/by_session"), session);
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
		final List<Document> documents = new ArrayList<Document>();
		for (final Question q : questions) {
			final Document d = toQuestionDocument(session, q);
			d.setId(q.get_id());
			d.setRev(q.get_rev());
			documents.add(d);
		}
		try {
			database.bulkSaveDocuments(documents.toArray(new Document[documents.size()]));
		} catch (final IOException e) {
			LOGGER.error("Could not bulk publish all questions: {}", e.getMessage());
		}
	}

	@Override
	public List<Question> setVotingAdmissionForAllQuestions(final Session session, final boolean disableVoting) {
		final List<Question> questions = getQuestions(new NovaView("skill_question/by_session"), session);
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
			if (!q.getQuestionType().equals("flashcard")) {
				q.setVotingDisabled(disableVoting);
			}
		}
		final List<Document> documents = new ArrayList<Document>();
		for (final Question q : questions) {
			final Document d = toQuestionDocument(session, q);
			d.setId(q.get_id());
			d.setRev(q.get_rev());
			documents.add(d);
		}

		try {
			database.bulkSaveDocuments(documents.toArray(new Document[documents.size()]));
		} catch (final IOException e) {
			LOGGER.error("Could not bulk set voting admission for all questions: {}", e.getMessage());
		}
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public void deleteAllQuestionsAnswers(final Session session) {
		final List<Question> questions = getQuestions(new NovaView("skill_question/by_session"), session);
		getDatabaseDao().resetQuestionsRoundState(session, questions);
		deleteAllAnswersForQuestions(questions);
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public void deleteAllPreparationAnswers(final Session session) {
		final List<Question> questions = getQuestions(new NovaView("skill_question/preparation_question_by_session"), session);
		getDatabaseDao().resetQuestionsRoundState(session, questions);
		deleteAllAnswersForQuestions(questions);
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@CacheEvict(value = "answers", allEntries = true)
	@Override
	public void deleteAllLectureAnswers(final Session session) {
		final List<Question> questions = getQuestions(new NovaView("skill_question/lecture_question_by_session"), session);
		getDatabaseDao().resetQuestionsRoundState(session, questions);
		deleteAllAnswersForQuestions(questions);
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
		final List<Document> documents = new ArrayList<Document>();
		for (final Question q : questions) {
			final Document d = toQuestionDocument(session, q);
			d.setId(q.get_id());
			d.setRev(q.get_rev());
			documents.add(d);
		}
		try {
			database.bulkSaveDocuments(documents.toArray(new Document[documents.size()]));
		} catch (final IOException e) {
			LOGGER.error("Could not bulk reset all questions round state: {}", e.getMessage());
		}
	}

	private boolean deleteAllAnswersForQuestions(List<Question> questions) {
		List<String> questionIds = new ArrayList<String>();
		for (Question q : questions) {
			questionIds.add(q.get_id());
		}
		final NovaView bulkView = new NovaView("answer/cleanup");
		bulkView.setKeys(questionIds);
		bulkView.setIncludeDocs(true);
		final List<Document> result = getDatabase().view(bulkView).getResults();
		final List<Document> allAnswers = new ArrayList<Document>();
		for (Document a : result) {
			final Document d = new Document(a.getJSONObject("doc"));
			d.put("_deleted", true);
			allAnswers.add(d);
		}
		try {
			getDatabase().bulkSaveDocuments(allAnswers.toArray(new Document[allAnswers.size()]));
		} catch (IOException e) {
			LOGGER.error("Could not bulk delete answers: {}", e.getMessage());
			return false;
		}
		return true;
	}

	private boolean deleteAllAnswersWithQuestions(List<Question> questions) {
		List<String> questionIds = new ArrayList<String>();
		final List<Document> allQuestions = new ArrayList<Document>();
		for (Question q : questions) {
			final Document d = new Document();
			d.put("_id", q.get_id());
			d.put("_rev", q.get_rev());
			d.put("_deleted", true);
			questionIds.add(q.get_id());
			allQuestions.add(d);
		}
		final NovaView bulkView = new NovaView("answer/cleanup");
		bulkView.setKeys(questionIds);
		bulkView.setIncludeDocs(true);
		final List<Document> result = getDatabase().view(bulkView).getResults();

		final List<Document> allAnswers = new ArrayList<Document>();
		for (Document a : result) {
			final Document d = new Document(a.getJSONObject("doc"));
			d.put("_deleted", true);
			allAnswers.add(d);
		}

		try {
			List<Document> deleteList = new ArrayList<Document>(allAnswers);
			deleteList.addAll(allQuestions);
			getDatabase().bulkSaveDocuments(deleteList.toArray(new Document[deleteList.size()]));
		} catch (IOException e) {
			LOGGER.error("Could not bulk delete questions and answers: {}", e.getMessage());
			return false;
		}
		return true;
	}

	@Cacheable("learningprogress")
	@Override
	public CourseScore getLearningProgress(final Session session) {
		final NovaView maximumValueView = new NovaView("learning_progress/maximum_value_of_question");
		final NovaView answerSumView = new NovaView("learning_progress/question_value_achieved_for_user");
		maximumValueView.setStartKeyArray(session.get_id());
		maximumValueView.setEndKeyArray(session.get_id(), "{}");
		answerSumView.setStartKeyArray(session.get_id());
		answerSumView.setEndKeyArray(session.get_id(), "{}");

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
	public DbUser createOrUpdateUser(DbUser user) {
		try {
			String id = user.getId();
			String rev = user.getRev();
			Document d = new Document();

			if (null != id) {
				d = database.getDocument(id, rev);
			}

			d.put("type", "userdetails");
			d.put("username", user.getUsername());
			d.put("password", user.getPassword());
			d.put("activationKey", user.getActivationKey());
			d.put("passwordResetKey", user.getPasswordResetKey());
			d.put("passwordResetTime", user.getPasswordResetTime());
			d.put("creation", user.getCreation());
			d.put("lastLogin", user.getLastLogin());

			database.saveDocument(d, id);
			user.setId(d.getId());
			user.setRev(d.getRev());

			return user;
		} catch (IOException e) {
			LOGGER.error("Could not save user {}", user);
		}

		return null;
	}

	@Override
	public DbUser getUser(String username) {
		NovaView view = new NovaView("user/all");
		view.setKey(username);
		ViewResults results = this.getDatabase().view(view);

		if (results.getJSONArray("rows").optJSONObject(0) == null) {
			return null;
		}

		return (DbUser) JSONObject.toBean(
			results.getJSONArray("rows").optJSONObject(0).optJSONObject("value"),
			DbUser.class
		);
	}

	@Override
	public boolean deleteUser(DbUser dbUser) {
		try {
			this.deleteDocument(dbUser.getId());

			return true;
		} catch (IOException e) {
			LOGGER.error("Could not delete user {}", dbUser.getId());
		}

		return false;
	}

	@Override
	public SessionInfo importSession(User user, ImportExportSession importSession) {
		final Session session = this.saveSession(user, importSession.generateSessionEntity(user));
		List<Document> questions = new ArrayList<Document>();
		// We need to remember which answers belong to which question.
		// The answers need a questionId, so we first store the questions to get the IDs.
		// Then we update the answer objects and store them as well.
		Map<Document, ImportExportQuestion> mapping = new HashMap<Document, ImportExportQuestion>();
		// Later, generate all answer documents
		List<Document> answers = new ArrayList<Document>();
		// We can then push answers together with interposed questions in one large bulk request
		List<Document> interposedQuestions = new ArrayList<Document>();
		// Motds shouldn't be forgotten, too
		List<Document> motds = new ArrayList<Document>();
		try {
			// add session id to all questions and generate documents
			for (ImportExportQuestion question : importSession.getQuestions()) {
				Document doc = toQuestionDocument(session, question);
				question.setSessionId(session.get_id());
				questions.add(doc);
				mapping.put(doc, question);
			}
			database.bulkSaveDocuments(questions.toArray(new Document[questions.size()]));

			// bulk import answers together with interposed questions
			for (Entry<Document, ImportExportQuestion> entry : mapping.entrySet()) {
				final Document doc = entry.getKey();
				final ImportExportQuestion question = entry.getValue();
				question.set_id(doc.getId());
				question.set_rev(doc.getRev());
				for (de.thm.arsnova.entities.transport.Answer answer : question.getAnswers()) {
					final Answer a = answer.generateAnswerEntity(user, question);
					final Document answerDoc = new Document();
					answerDoc.put("type", "skill_question_answer");
					answerDoc.put("sessionId", a.getSessionId());
					answerDoc.put("questionId", a.getQuestionId());
					answerDoc.put("answerSubject", a.getAnswerSubject());
					answerDoc.put("questionVariant", a.getQuestionVariant());
					answerDoc.put("questionValue", a.getQuestionValue());
					answerDoc.put("answerText", a.getAnswerText());
					answerDoc.put("timestamp", a.getTimestamp());
					answerDoc.put("piRound", a.getPiRound());
					answerDoc.put("abstention", a.isAbstention());
					// we do not store the user's name
					answerDoc.put("user", "");
					answers.add(answerDoc);
				}
			}
			for (de.thm.arsnova.entities.transport.InterposedQuestion i : importSession.getFeedbackQuestions()) {
				final Document q = new Document();
				q.put("type", "interposed_question");
				q.put("sessionId", session.get_id());
				q.put("subject", i.getSubject());
				q.put("text", i.getText());
				q.put("timestamp", i.getTimestamp());
				q.put("read", i.isRead());
				// we do not store the creator's name
				q.put("creator", "");
				interposedQuestions.add(q);
			}
			for (Motd m : importSession.getMotds()) {
				final Document d = new Document();
				d.put("type", "motd");
				d.put("motdkey", m.getMotdkey());
				d.put("title", m.getTitle());
				d.put("text", m.getText());
				d.put("audience", m.getAudience());
				d.put("sessionkey", session.getKeyword());
				d.put("startdate", String.valueOf(m.getStartdate().getTime()));
				d.put("enddate", String.valueOf(m.getEnddate().getTime()));
				motds.add(d);
			}
			List<Document> documents = new ArrayList<Document>(answers);
			database.bulkSaveDocuments(interposedQuestions.toArray(new Document[interposedQuestions.size()]));
			database.bulkSaveDocuments(motds.toArray(new Document[motds.size()]));
			database.bulkSaveDocuments(documents.toArray(new Document[documents.size()]));
		} catch (IOException e) {
			LOGGER.error("Could not import this session: {}", e.getMessage());
			// Something went wrong, delete this session since we do not want a partial import
			this.deleteSession(session);
			return null;
		}
		return this.calculateSessionInfo(importSession, session);
	}

	@Override
	public ImportExportSession exportSession(String sessionkey, Boolean withAnswers, Boolean withFeedbackQuestions) {
		ImportExportSession importExportSession = new ImportExportSession();
		Session session = getDatabaseDao().getSessionFromKeyword(sessionkey);
		importExportSession.setSessionFromSessionObject(session);
		List<Question> questionList = getDatabaseDao().getAllSkillQuestions(session);
		for (Question question : questionList) {
			List<de.thm.arsnova.entities.transport.Answer> answerList = new ArrayList<de.thm.arsnova.entities.transport.Answer>();
			if (withAnswers) {
				for (Answer a : this.getDatabaseDao().getAllAnswers(question)) {
					de.thm.arsnova.entities.transport.Answer transportAnswer = new de.thm.arsnova.entities.transport.Answer(a);
					answerList.add(transportAnswer);
				}
				// getAllAnswers does not grep for whole answer object so i need to add empty entries for abstentions
				int i = this.getDatabaseDao().getAbstentionAnswerCount(question.get_id());
				for (int b = 0; b < i; b++) {
					de.thm.arsnova.entities.transport.Answer ans = new de.thm.arsnova.entities.transport.Answer();
					ans.setAnswerSubject("");
					ans.setAnswerImage("");
					ans.setAnswerText("");
					ans.setAbstention(true);
					answerList.add(ans);
				}
			}
			importExportSession.addQuestionWithAnswers(question, answerList);
		}
		if (withFeedbackQuestions) {
			List<de.thm.arsnova.entities.transport.InterposedQuestion> interposedQuestionList = new ArrayList<de.thm.arsnova.entities.transport.InterposedQuestion>();
			for (InterposedQuestion i : getDatabaseDao().getInterposedQuestions(session, 0, 0)) {
				de.thm.arsnova.entities.transport.InterposedQuestion transportInterposedQuestion = new de.thm.arsnova.entities.transport.InterposedQuestion(i);
				interposedQuestionList.add(transportInterposedQuestion);
			}
			importExportSession.setFeedbackQuestions(interposedQuestionList);
		}
		if (withAnswers) {
			importExportSession.setSessionInfo(this.calculateSessionInfo(importExportSession, session));
		}
		importExportSession.setMotds(getDatabaseDao().getMotdsForSession(session.getKeyword()));
		return importExportSession;
	}

	public SessionInfo calculateSessionInfo(ImportExportSession importExportSession, Session session) {
		int unreadInterposed = 0;
		int numUnanswered = 0;
		int numAnswers = 0;
		for (de.thm.arsnova.entities.transport.InterposedQuestion i : importExportSession.getFeedbackQuestions()) {
			if (!i.isRead()) {
				unreadInterposed++;
			}
		}
		for (ImportExportQuestion question : importExportSession.getQuestions()) {
			numAnswers += question.getAnswers().size();
			if (question.getAnswers().size() == 0) {
				numUnanswered++;
			}
		}
		final SessionInfo info = new SessionInfo(session);
		info.setNumQuestions(importExportSession.getQuestions().size());
		info.setNumUnanswered(numUnanswered);
		info.setNumAnswers(numAnswers);
		info.setNumInterposed(importExportSession.getFeedbackQuestions().size());
		info.setNumUnredInterposed(unreadInterposed);
		return info;
	}

	@Override
	public List<String> getSubjects(Session session, String questionVariant) {
		String viewString = "";
		if ("lecture".equals(questionVariant)) {
			viewString = "skill_question/lecture_question_subjects_by_session";
		} else {
			viewString = "skill_question/preparation_question_subjects_by_session";
		}
		NovaView view = new NovaView(viewString);
		view.setKey(session.get_id());
		ViewResults results = this.getDatabase().view(view);

		if (results.getJSONArray("rows").optJSONObject(0) == null) {
			return null;
		}

		Set<String> uniqueSubjects = new HashSet<>();

		for (final Document d : results.getResults()) {
			uniqueSubjects.add(d.getString("value"));
		}

		List<String> uniqueSubjectsList = new ArrayList<>(uniqueSubjects);

		return uniqueSubjectsList;
	}

	@Override
	public List<String> getQuestionIdsBySubject(Session session, String questionVariant, String subject) {
		String viewString = "";
		if ("lecture".equals(questionVariant)) {
			viewString = "skill_question/lecture_question_ids_by_session_and_subject";
		} else {
			viewString = "skill_question/preparation_question_ids_by_session_and_subject";
		}
		NovaView view = new NovaView(viewString);
		view.setKey(session.get_id(), subject);
		ViewResults results = this.getDatabase().view(view);

		if (results.getJSONArray("rows").optJSONObject(0) == null) {
			return null;
		}

		List<String> qids = new ArrayList<>();

		for (final Document d : results.getResults()) {
			final String s = d.getString("value");
			qids.add(s);
		}

		return qids;
	}

	@Cacheable(value = "sortorder", key = "#p0.concat('-').concat(#p1).concat('-').concat(#p2)")
	@Override
	public SortOrder getSortOrder(String sessionId, String questionVariant, String subject) {
		String viewString = "";
		if ("preparation".equals(questionVariant)) {
			viewString = "sort_order/preparation_question_sort_order_by_sessionid_and_subject";
		} else if ("lecture".equals(questionVariant)) {
			viewString = "sort_order/lecture_question_sort_order_by_sessionid_and_subject";
		}

		NovaView view = new NovaView(viewString);
		view.setKey(sessionId, subject);

		ViewResults results = this.getDatabase().view(view);

		if (isEmptyResults(results)) {
			return null;
		}

		SortOrder sortOrder = new SortOrder();

		for (final Document d : results.getResults()) {
			sortOrder.set_id(d.getJSONObject("value").getString("_id"));
			sortOrder.set_rev(d.getJSONObject("value").getString("_rev"));
			sortOrder.setSessionId(d.getJSONObject("value").getString("sessionId"));
			sortOrder.setSubject(d.getJSONObject("value").getString("subject"));
			sortOrder.setSortType(d.getJSONObject("value").getString("sortType"));
			sortOrder.setQuestionVariant(d.getJSONObject("value").getString("questionVariant"));
			List<String> sort = new ArrayList<String>();
			JSONArray json = d.getJSONObject("value").getJSONArray("sortOrder");
			int len = json.size();
			for (int i = 0; i < len; i++) {
				sort.add(json.getString(i));
			}
			sortOrder.setSortOrder(sort);
		}

		return sortOrder;
	}

	@CachePut(value = "sortorder", key = "#sortOrder.sessionId.concat('-').concat(#sortOrder.questionVariant).concat('-').concat(#sortOrder.subject)")
	@Override
	public SortOrder createOrUpdateSortOrder(SortOrder sortOrder) {
		try {
			SortOrder oldSortOrder = getDatabaseDao().getSortOrder(sortOrder.getSessionId(), sortOrder.getQuestionVariant(), sortOrder.getSubject());
			Document d = new Document();

			String id = "";
			String rev = "";
			if (oldSortOrder != null) {
				id = oldSortOrder.get_id();
				rev = oldSortOrder.get_rev();
				d = database.getDocument(id, rev);
			}

			d.put("type", "sort_order");
			d.put("sessionId", sortOrder.getSessionId());
			d.put("sortType", sortOrder.getSortType());
			d.put("questionVariant", sortOrder.getQuestionVariant());
			d.put("subject", sortOrder.getSubject());
			d.put("sortOrder", sortOrder.getSortOrder());

			database.saveDocument(d, id);
			sortOrder.set_id(d.getId());
			sortOrder.set_rev(d.getRev());

			return sortOrder;
		} catch (IOException e) {
			LOGGER.error("Could not save sort {}", sortOrder);
		}
		return null;
	}

	@CacheEvict(value = "sortorder", key = "#sortOrder.sessionId.concat('-').concat(#sortOrder.questionVariant).concat('-').concat(#sortOrder.subject)")
	@Override
	public void deleteSortOrder(SortOrder sortOrder) {
		try {
			this.deleteDocument(sortOrder.get_id());
		} catch (IOException e) {
			LOGGER.error("Could not delete SortOrder {}", sortOrder.get_id());
		}
	}

	@Override
	public List<Question> getQuestionsByIds(List<String> ids, final Session session) {
		NovaView view = new NovaView("_all_docs");
		view.setKeys(ids);
		view.setIncludeDocs(true);
		final List<Document> questiondocs = getDatabase().view(view).getResults();
		if (questiondocs == null || questiondocs.isEmpty()) {

			return null;
		}
		final List<Question> result = new ArrayList<Question>();
		final MorpherRegistry morpherRegistry = JSONUtils.getMorpherRegistry();
		final Morpher dynaMorpher = new BeanMorpher(PossibleAnswer.class, morpherRegistry);
		morpherRegistry.registerMorpher(dynaMorpher);
		for (final Document document : questiondocs) {
			if (!document.optString("error").equals("")) {
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
			question.setPossibleAnswers(new ArrayList<PossibleAnswer>(answers));
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
	public List<Motd> getAdminMotds() {
		NovaView view = new NovaView("motd/admin");
		return getMotds(view);
	}

	@Override
	@Cacheable(cacheNames = "motds", key = "'all'")
	public List<Motd> getMotdsForAll() {
		NovaView view = new NovaView("motd/for_all");
		return getMotds(view);
	}

	@Override
	@Cacheable(cacheNames = "motds", key = "'loggedIn'")
	public List<Motd> getMotdsForLoggedIn() {
		NovaView view = new NovaView("motd/for_loggedin");
		return getMotds(view);
	}

	@Override
	@Cacheable(cacheNames = "motds", key = "'tutors'")
	public List<Motd> getMotdsForTutors() {
		NovaView view = new NovaView("motd/for_tutors");
		return getMotds(view);
	}

	@Override
	@Cacheable(cacheNames = "motds", key = "'students'")
	public List<Motd> getMotdsForStudents() {
		NovaView view = new NovaView("motd/for_students");
		return getMotds(view);
	}

	@Override
	@Cacheable(cacheNames = "motds", key = "('session').concat(#p0)")
	public List<Motd> getMotdsForSession(final String sessionkey) {
		NovaView view = new NovaView("motd/by_sessionkey");
		view.setKey(sessionkey);
		return getMotds(view);
	}

	@Override
	public List<Motd> getMotds(NovaView view) {
		final ViewResults motddocs = this.getDatabase().view(view);
		List<Motd> motdlist = new ArrayList<Motd>();
		for (final Document d : motddocs.getResults()) {
			Motd motd = new Motd();
			motd.set_id(d.getId());
			motd.set_rev(d.getJSONObject("value").getString("_rev"));
			motd.setMotdkey(d.getJSONObject("value").getString("motdkey"));
			Date start = new Date(Long.parseLong(d.getJSONObject("value").getString("startdate")));
			motd.setStartdate(start);
			Date end = new Date(Long.parseLong(d.getJSONObject("value").getString("enddate")));
			motd.setEnddate(end);
			motd.setTitle(d.getJSONObject("value").getString("title"));
			motd.setText(d.getJSONObject("value").getString("text"));
			motd.setAudience(d.getJSONObject("value").getString("audience"));
			motd.setSessionkey(d.getJSONObject("value").getString("sessionkey"));
			motdlist.add(motd);
		}
		return motdlist;
	}

	@Override
	public Motd getMotdByKey(String key) {
		NovaView view = new NovaView("motd/by_keyword");
		view.setKey(key);
		Motd motd = new Motd();

		ViewResults results = this.getDatabase().view(view);

		for (final Document d : results.getResults()) {
			motd.set_id(d.getId());
			motd.set_rev(d.getJSONObject("value").getString("_rev"));
			motd.setMotdkey(d.getJSONObject("value").getString("motdkey"));
			Date start = new Date(Long.parseLong(d.getJSONObject("value").getString("startdate")));
			motd.setStartdate(start);
			Date end = new Date(Long.parseLong(d.getJSONObject("value").getString("enddate")));
			motd.setEnddate(end);
			motd.setTitle(d.getJSONObject("value").getString("title"));
			motd.setText(d.getJSONObject("value").getString("text"));
			motd.setAudience(d.getJSONObject("value").getString("audience"));
			motd.setSessionkey(d.getJSONObject("value").getString("sessionkey"));
		}

		return motd;
	}

	@Override
	@CacheEvict(cacheNames = "motds", key = "#p0.audience.concat(#p0.sessionkey)")
	public Motd createOrUpdateMotd(Motd motd) {
		try {
			String id = motd.get_id();
			String rev = motd.get_rev();
			Document d = new Document();

			if (null != id) {
				d = database.getDocument(id, rev);
			}
			if (motd.getMotdkey() == null) {
				motd.setMotdkey(sessionService.generateKeyword());
			}
			d.put("type", "motd");
			d.put("motdkey", motd.getMotdkey());
			d.put("startdate", String.valueOf(motd.getStartdate().getTime()));
			d.put("enddate", String.valueOf(motd.getEnddate().getTime()));
			d.put("title", motd.getTitle());
			d.put("text", motd.getText());
			d.put("audience", motd.getAudience());
			d.put("sessionkey", motd.getSessionkey());

			database.saveDocument(d, id);
			motd.set_id(d.getId());
			motd.set_rev(d.getRev());

			return motd;
		} catch (IOException e) {
			LOGGER.error("Could not save motd {}", motd);
		}

		return null;
	}

	@Override
	@CacheEvict(cacheNames = "motds", key = "#p0.audience.concat(#p0.sessionkey)")
	public void deleteMotd(Motd motd) {
		try {
			this.deleteDocument(motd.get_id());
		} catch (IOException e) {
			LOGGER.error("Could not delete Motd {}", motd.get_id());
		}
	}

	@Override
	@Cacheable(cacheNames = "motdlist", key = "#p0")
	public MotdList getMotdListForUser(final String username) {
		NovaView view = new NovaView("motd/list_by_username");
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
			LOGGER.error("Could not save motdlist {}", motdlist);
		}

		return null;
	}

}
