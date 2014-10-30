/*
 * Copyright (C) 2012 THM webMedia
 *
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.dao;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import net.sf.ezmorph.Morpher;
import net.sf.ezmorph.MorpherRegistry;
import net.sf.ezmorph.bean.BeanMorpher;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.fourspaces.couchdb.Database;
import com.fourspaces.couchdb.Document;
import com.fourspaces.couchdb.View;
import com.fourspaces.couchdb.ViewResults;

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.DbUser;
import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.PossibleAnswer;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.VisitedSession;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.services.ISessionService;

@Component("databaseDao")
public class CouchDBDao implements IDatabaseDao {

	@Autowired
	private ISessionService sessionService;

	private String databaseHost;
	private int databasePort;
	private String databaseName;
	private Database database;

	public static final Logger LOGGER = LoggerFactory.getLogger(CouchDBDao.class);

	@Value("${couchdb.host}")
	public final void setDatabaseHost(final String newDatabaseHost) {
		databaseHost = newDatabaseHost;
	}

	@Value("${couchdb.port}")
	public final void setDatabasePort(final String newDatabasePort) {
		databasePort = Integer.parseInt(newDatabasePort);
	}

	@Value("${couchdb.name}")
	public final void setDatabaseName(final String newDatabaseName) {
		databaseName = newDatabaseName;
	}

	public final void setSessionService(final ISessionService service) {
		sessionService = service;
	}

	@Override
	public final Session getSession(final String keyword) {
		final Session result = getSessionFromKeyword(keyword);
		if (result != null) {
			return result;
		}

		throw new NotFoundException();
	}

	@Override
	public final List<Session> getMySessions(final User user) {
		final NovaView view = new NovaView("session/by_creator");
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
	public final List<Question> getSkillQuestions(final User user, final Session session) {
		String viewName;
		if (session.getCreator().equals(user.getUsername())) {
			viewName = "skill_question/by_session_sorted_by_subject_and_text";
		} else {
			viewName = "skill_question/by_session_for_all_full";
		}
		return getQuestions(new NovaView(viewName), session);
	}

	@Override
	public final int getSkillQuestionCount(final Session session) {
		return getQuestionCount(new NovaView("skill_question/count_by_session"), session);
	}

	@Override
	public final Session getSessionFromKeyword(final String keyword) {
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
	public final Session getSessionFromId(final String sessionId) {
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
	public final Session saveSession(final User user, final Session session) {
		final Document sessionDocument = new Document();
		sessionDocument.put("type", "session");
		sessionDocument.put("name", session.getName());
		sessionDocument.put("shortName", session.getShortName());
		sessionDocument.put("keyword", sessionService.generateKeyword());
		sessionDocument.put("creator", user.getUsername());
		sessionDocument.put("active", true);
		sessionDocument.put("courseType", session.getCourseType());
		sessionDocument.put("courseId", session.getCourseId());
		try {
			database.saveDocument(sessionDocument);
		} catch (final IOException e) {
			return null;
		}
		return getSession(sessionDocument.getString("keyword"));
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public final boolean sessionKeyAvailable(final String keyword) {
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

	@Override
	public final Question saveQuestion(final Session session, final Question question) {
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
		final Document q = new Document();
		q.put("type", "skill_question");
		q.put("questionType", question.getQuestionType());
		q.put("questionVariant", question.getQuestionVariant());
		q.put("sessionId", session.get_id());
		q.put("subject", question.getSubject());
		q.put("text", question.getText());
		q.put("active", question.isActive());
		q.put("number", 0); // TODO: This number is now unused. A clean up is necessary.
		q.put("releasedFor", question.getReleasedFor());
		q.put("possibleAnswers", question.getPossibleAnswers());
		q.put("noCorrect", question.isNoCorrect());
		q.put("piRound", question.getPiRound());
		q.put("showStatistic", question.isShowStatistic());
		q.put("showAnswer", question.isShowAnswer());
		q.put("abstention", question.isAbstention());
		q.put("image", question.getImage());
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
		
		return q;
	}

	@Override
	public final Question updateQuestion(final Question question) {
		try {
			final Document q = database.getDocument(question.get_id());
			q.put("subject", question.getSubject());
			q.put("text", question.getText());
			q.put("active", question.isActive());
			q.put("releasedFor", question.getReleasedFor());
			q.put("possibleAnswers", question.getPossibleAnswers());
			q.put("noCorrect", question.isNoCorrect());
			q.put("piRound", question.getPiRound());
			q.put("showStatistic", question.isShowStatistic());
			q.put("showAnswer", question.isShowAnswer());
			q.put("abstention", question.isAbstention());
			q.put("image", question.getImage());
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
			database.saveDocument(q);
			question.set_rev(q.getRev());

			return question;
		} catch (final IOException e) {
			LOGGER.error("Could not update question {}", question);
		}

		return null;
	}

	@Override
	public final InterposedQuestion saveQuestion(final Session session, final InterposedQuestion question, User user) {
		final Document q = new Document();
		q.put("type", "interposed_question");
		q.put("sessionId", session.get_id());
		q.put("subject", question.getSubject());
		q.put("text", question.getText());
		q.put("timestamp", System.currentTimeMillis());
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

	@Override
	public final Question getQuestion(final String id) {
		try {
			final NovaView view = new NovaView("skill_question/by_id");
			view.setKey(id);
			final ViewResults results = getDatabase().view(view);

			if (results.getJSONArray("rows").optJSONObject(0) == null) {
				return null;
			}

			final Question q = (Question) JSONObject.toBean(
					results.getJSONArray("rows").optJSONObject(0).optJSONObject("value"),
					Question.class
					);
			final JSONArray possibleAnswers = results.getJSONArray("rows").optJSONObject(0).optJSONObject("value")
					.getJSONArray("possibleAnswers");
			final Collection<PossibleAnswer> answers = JSONArray.toCollection(
					possibleAnswers,
					PossibleAnswer.class
					);
			q.setPossibleAnswers(new ArrayList<PossibleAnswer>(answers));
			q.setSessionKeyword(getSessionKeyword(q.getSessionId()));
			return q;
		} catch (final IOException e) {
			LOGGER.error("Could not get question with id {}", id);
		}
		return null;
	}

	@Override
	public final LoggedIn registerAsOnlineUser(final User user, final Session session) {
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
				final Collection<VisitedSession> visitedSessions = JSONArray.toCollection(vs, VisitedSession.class);
				l.setVisitedSessions(new ArrayList<VisitedSession>(visitedSessions));
			}
			return l;
		} catch (final IOException e) {
			return null;
		}
	}

	@Override
	public final void updateSessionOwnerActivity(final Session session) {
		try {
			/* Do not clutter CouchDB. Only update once every 3 hours. */
			if (session.getLastOwnerActivity() > System.currentTimeMillis() - 3 * 3600000) {
				return;
			}

			session.setLastOwnerActivity(System.currentTimeMillis());
			final JSONObject json = JSONObject.fromObject(session);
			getDatabase().saveDocument(new Document(json));
		} catch (final IOException e) {
			LOGGER.error("Failed to update lastOwnerActivity for Session {}", session);
			return;
		}
	}

	@Override
	public final List<String> getQuestionIds(final Session session, final User user) {
		NovaView view = new NovaView("skill_question/by_session_only_id_for_all");
		view.setKey(session.get_id());
		return collectQuestionIds(view);
	}

	@Override
	public final void deleteQuestionWithAnswers(final Question question) {
		try {
			deleteAnswers(question);
			deleteDocument(question.get_id());
		} catch (final IOException e) {
			LOGGER.error("IOException: Could not delete question {}", question.get_id());
		}
	}

	@Override
	public final void deleteAllQuestionsWithAnswers(final Session session) {
		final NovaView view = new NovaView("skill_question/by_session");
		deleteAllQuestionDocumentsWithAnswers(session, view);
	}

	private void deleteAllQuestionDocumentsWithAnswers(final Session session, final NovaView view) {
		view.setStartKeyArray(session.get_id());
		view.setEndKey(session.get_id(), "{}");
		final ViewResults results = getDatabase().view(view);

		for (final Document d : results.getResults()) {
			final Question q = new Question();
			q.set_id(d.getId());
			deleteQuestionWithAnswers(q);
		}
	}

	private void deleteDocument(final String documentId) throws IOException {
		final Document d = getDatabase().getDocument(documentId);
		getDatabase().deleteDocument(d);
	}

	@Override
	public final void deleteAnswers(final Question question) {
		try {
			final NovaView view = new NovaView("answer/cleanup");
			view.setKey(question.get_id());
			final ViewResults results = getDatabase().view(view);

			for (final Document d : results.getResults()) {
				deleteDocument(d.getId());
			}
		} catch (final IOException e) {
			LOGGER.error("IOException: Could not delete answers for question {}", question.get_id());
		}
	}

	@Override
	public final List<String> getUnAnsweredQuestionIds(final Session session, final User user) {
		final NovaView view = new NovaView("answer/by_user");
		view.setKey(user.getUsername(), session.get_id());
		return collectUnansweredQuestionIds(getQuestionIds(session, user), view);
	}

	@Override
	public final Answer getMyAnswer(final User me, final String questionId, final int piRound) {

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

	@Override
	public final List<Answer> getAnswers(final String questionId, final int piRound) {
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
		final int abstentionCount = getAbstentionAnswerCount(questionId);
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

	private int getAbstentionAnswerCount(final String questionId) {
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
	public final int getAnswerCount(final Question question, final int piRound) {
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
	public final int countActiveUsers(final long since) {
		try {
			final View view = new View("statistic/count_active_users");
			view.setStartKey(String.valueOf(since));
			final ViewResults results = getDatabase().view(view);
			if (isEmptyResults(results)) {
				return 0;
			}
			return results.getJSONArray("rows").optJSONObject(0).getInt("value");
		} catch (final Exception e) {
			LOGGER.error("Error while retrieving active users count", e);
		}
		return 0;
	}

	private boolean isEmptyResults(final ViewResults results) {
		return results == null || results.getResults().isEmpty() || results.getJSONArray("rows").size() == 0;
	}

	@Override
	public List<Answer> getFreetextAnswers(final String questionId) {
		final List<Answer> answers = new ArrayList<Answer>();
		final NovaView view = new NovaView("skill_question/freetext_answers_full");
		view.setKey(questionId);
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
	public List<Answer> getMyAnswers(final User me, final String sessionKey) {
		final Session s = getSessionFromKeyword(sessionKey);
		if (s == null) {
			throw new NotFoundException();
		}

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
		final Session s = getSessionFromKeyword(sessionKey);
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
		final Session s = getSessionFromKeyword(sessionKey);
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
	public List<InterposedQuestion> getInterposedQuestions(final Session session) {
		final NovaView view = new NovaView("interposed_question/by_session");
		view.setKey(session.get_id());
		final ViewResults questions = getDatabase().view(view);
		if (questions == null || questions.isEmpty()) {
			return null;
		}
		return createInterposedList(session, questions);
	}

	@Override
	public List<InterposedQuestion> getInterposedQuestions(final Session session, final User user) {
		final NovaView view = new NovaView("interposed_question/by_session_and_creator");
		view.setKey(session.get_id(), user.getUsername());
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

	public InterposedQuestion getInterposedQuestion(final String sessionKey, final String documentId) {
		try {
			final Document document = getDatabase().getDocument(documentId);
			if (document == null) {
				LOGGER.error("Document is NULL");
				return null;
			}
			return (InterposedQuestion) JSONObject.toBean(document.getJSONObject(), InterposedQuestion.class);
		} catch (final IOException e) {
			LOGGER.error("Error while retrieving interposed question", e);
		}
		return null;
	}

	@Override
	public void vote(final User me, final String menu) {
		final String date = new SimpleDateFormat("dd-mm-yyyyy").format(new Date());
		try {
			final NovaView view = new NovaView("food_vote/get_user_vote");
			view.setKey(date, me.getUsername());
			final ViewResults results = getDatabase().view(view);

			if (results.getResults().isEmpty()) {
				final Document vote = new Document();
				vote.put("type", "food_vote");
				vote.put("name", menu);
				vote.put("user", me.getUsername());
				vote.put("day", date);
				database.saveDocument(vote);
			} else {
				final Document vote = results.getResults().get(0);
				vote.put("name", menu);
				database.saveDocument(vote);
			}
		} catch (final IOException e) {
			LOGGER.error("Error while saving user food vote", e);
		}
	}

	@Override
	public int countSessions() {
		return sessionsCountValue("openSessions")
				+ sessionsCountValue("closedSessions");
	}

	@Override
	public int countClosedSessions() {
		return sessionsCountValue("closedSessions");
	}

	@Override
	public int countOpenSessions() {
		return sessionsCountValue("openSessions");
	}

	@Override
	public int countAnswers() {
		return sessionsCountValue("answers");
	}

	@Override
	public int countQuestions() {
		return sessionsCountValue("questions");
	}

	private int sessionsCountValue(final String key) {
		try {
			final View view = new View("statistic/count_sessions");
			view.setGroup(true);

			final ViewResults results = getDatabase().view(view);
			if (isEmptyResults(results)) {
				return 0;
			}

			int result = 0;
			final JSONArray rows = results.getJSONArray("rows");

			for (int i = 0; i < rows.size(); i++) {
				final JSONObject row = rows.getJSONObject(i);
				if (
						row.getString("key").equals(key)
						) {
					result += row.getInt("value");
				}
			}
			return result;
		} catch (final Exception e) {
			LOGGER.error("Error while retrieving session count", e);
		}
		return 0;
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
	public List<Session> getMyVisitedSessions(final User user) {
		final NovaView view = new NovaView("logged_in/visited_sessions_by_user");
		view.setKey(user.getUsername());
		final ViewResults sessions = getDatabase().view(view);
		final List<Session> allSessions = new ArrayList<Session>();
		for (final Document d : sessions.getResults()) {
			// Not all users have visited sessions
			if (d.getJSONObject().optJSONArray("value") != null) {
				@SuppressWarnings("unchecked")
				final
				Collection<Session> visitedSessions =  JSONArray.toCollection(
						d.getJSONObject().getJSONArray("value"),
						Session.class
						);
				allSessions.addAll(visitedSessions);
			}
		}
		// Do these sessions still exist?
		final List<Session> result = new ArrayList<Session>();
		for (final Session s : allSessions) {
			try {
				final Session session = getSessionFromKeyword(s.getKeyword());
				if (session != null) {
					result.add(session);
				}
			} catch (final NotFoundException e) {
				// TODO Remove non existant session
			}
		}
		return result;
	}

	@Override
	public Answer saveAnswer(final Answer answer, final User user) {
		try {
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
			database.saveDocument(a);
			answer.set_id(a.getId());
			answer.set_rev(a.getRev());
			return answer;
		} catch (final IOException e) {
			LOGGER.error("Could not save answer {}", answer);
		}
		return null;
	}

	@Override
	public Answer updateAnswer(final Answer answer) {
		try {
			final Document a = database.getDocument(answer.get_id());
			a.put("answerSubject", answer.getAnswerSubject());
			a.put("answerText", answer.getAnswerText());
			a.put("timestamp", answer.getTimestamp());
			a.put("abstention", answer.isAbstention());
			a.put("questionValue", answer.getQuestionValue());
			database.saveDocument(a);
			answer.set_rev(a.getRev());
			return answer;
		} catch (final IOException e) {
			LOGGER.error("Could not save answer {}", answer);
		}
		return null;
	}

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

	@Override
	public final List<String> getActiveUsers(final int timeDifference) {
		final long inactiveBeforeTimestamp = new Date().getTime() - timeDifference * 1000;

		final NovaView view = new NovaView("logged_in/by_and_only_timestamp_and_username");
		view.setStartKeyArray(String.valueOf(inactiveBeforeTimestamp));
		final ViewResults results = getDatabase().view(view);

		final List<String> result = new ArrayList<String>();
		for (final Document d : results.getResults()) {
			result.add(d.getJSONObject().getJSONArray("key").getString(1));
		}
		return result;
	}

	private static class ExtendedView extends View {

		private String keys;

		public ExtendedView(final String fullname) {
			super(fullname);
		}

		public void setKeys(final String newKeys) {
			keys = newKeys;
		}

		public void setCourseIdKeys(final List<Course> courses) {
			if (courses.isEmpty()) {
				keys = "[]";
				return;
			}

			final StringBuilder sb = new StringBuilder();
			sb.append("[");
			for (int i = 0; i < courses.size() - 1; i++) {
				sb.append("\"" + courses.get(i).getId() + "\",");
			}
			sb.append("\"" + courses.get(courses.size() - 1).getId() + "\"");
			sb.append("]");
			try {
				setKeys(URLEncoder.encode(sb.toString(), "UTF-8"));
			} catch (final UnsupportedEncodingException e) {
				LOGGER.error("Error while encoding course ID keys", e);
			}
		}

		@Override
		public String getQueryString() {
			final StringBuilder query = new StringBuilder();
			if (super.getQueryString() != null) {
				query.append(super.getQueryString());
			}
			if (keys != null) {
				if (query.toString().isEmpty()) {
					query.append("&");
				}

				query.append("keys=" + keys);
			}

			if (query.toString().isEmpty()) {
				return null;
			}
			return query.toString();
		}
	}

	@Override
	public Session lockSession(final Session session, final Boolean lock) {
		try {
			final Document s = database.getDocument(session.get_id());
			s.put("active", lock);
			database.saveDocument(s);
			session.set_rev(s.getRev());
			return session;
		} catch (final IOException e) {
			LOGGER.error("Could not lock session {}", session);
		}
		return null;
	}

	@Override
	public Session updateSession(final Session session) {
		try {
			final Document s = database.getDocument(session.get_id());
			s.put("name", session.getName());
			s.put("shortName", session.getShortName());
			s.put("active", session.isActive());
			database.saveDocument(s);
			session.set_rev(s.getRev());

			return session;
		} catch (final IOException e) {
			LOGGER.error("Could not lock session {}", session);
		}

		return null;
	}

	@Override
	public void deleteSession(final Session session) {
		try {
			deleteDocument(session.get_id());
		} catch (final IOException e) {
			LOGGER.error("Could not delete session {}", session);
		}
	}

	@Override
	public List<Question> getLectureQuestions(final User user, final Session session) {
		String viewName;
		if (session.isCreator(user)) {
			viewName = "skill_question/lecture_question_by_session";
		} else {
			viewName = "skill_question/lecture_question_by_session_for_all";
		}
		return getQuestions(new NovaView(viewName), session);
	}

	@Override
	public List<Question> getFlashcards(final User user, final Session session) {
		String viewName;
		if (session.isCreator(user)) {
			viewName = "skill_question/flashcard_by_session";
		} else {
			viewName = "skill_question/flashcard_by_session_for_all";
		}
		return getQuestions(new NovaView(viewName), session);
	}

	@Override
	public List<Question> getPreparationQuestions(final User user, final Session session) {
		String viewName;
		if (session.isCreator(user)) {
			viewName = "skill_question/preparation_question_by_session";
		} else {
			viewName = "skill_question/preparation_question_by_session_for_all";
		}
		return getQuestions(new NovaView(viewName), session);

	}

	private List<Question> getQuestions(final NovaView view, final Session session) {
		view.setStartKeyArray(session.get_id());
		view.setEndKeyArray(session.get_id(), "{}");
		final ViewResults questions = getDatabase().view(view);
		if (questions == null || questions.isEmpty()) {
			return null;
		}
		final List<Question> result = new ArrayList<Question>();

		final MorpherRegistry morpherRegistry = JSONUtils.getMorpherRegistry();
		final Morpher dynaMorpher = new BeanMorpher(PossibleAnswer.class, morpherRegistry);
		morpherRegistry.registerMorpher(dynaMorpher);
		for (final Document document : questions.getResults()) {
			final Question question = (Question) JSONObject.toBean(
					document.getJSONObject().getJSONObject("value"),
					Question.class
					);
			@SuppressWarnings("unchecked")
			final
			Collection<PossibleAnswer> answers = JSONArray.toCollection(
					document.getJSONObject().getJSONObject("value").getJSONArray("possibleAnswers"),
					PossibleAnswer.class
					);
			question.setPossibleAnswers(new ArrayList<PossibleAnswer>(answers));
			question.setSessionKeyword(session.getKeyword());
			if (!"freetext".equals(question.getQuestionType()) && 0 == question.getPiRound()) {
				/* needed for legacy questions whose piRound property has not been set */
				question.setPiRound(1);
			}
			result.add(question);
		}
		return result;
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

	@Override
	public void deleteAllLectureQuestionsWithAnswers(final Session session) {
		final NovaView view = new NovaView("skill_question/lecture_question_by_session");
		deleteAllQuestionDocumentsWithAnswers(session, view);
	}

	@Override
	public void deleteAllFlashcardsWithAnswers(final Session session) {
		final NovaView view = new NovaView("skill_question/flashcard_by_session");
		deleteAllQuestionDocumentsWithAnswers(session, view);
	}

	@Override
	public void deleteAllPreparationQuestionsWithAnswers(final Session session) {
		final NovaView view = new NovaView("skill_question/preparation_question_by_session");
		deleteAllQuestionDocumentsWithAnswers(session, view);
	}

	@Override
	public List<String> getUnAnsweredLectureQuestionIds(final Session session, final User user) {
		final NovaView view = new NovaView("answer/variant_by_user");
		view.setKey(user.getUsername(), session.get_id(), "lecture");
		return collectUnansweredQuestionIds(getLectureQuestionIds(session, user), view);
	}

	private List<String> getLectureQuestionIds(final Session session, final User user) {
		NovaView view = new NovaView("skill_question/lecture_question_by_session_for_all");
		view.setStartKeyArray(session.get_id());
		view.setEndKeyArray(session.get_id(), "{}");
		return collectQuestionIds(view);
	}

	@Override
	public List<String> getUnAnsweredPreparationQuestionIds(final Session session, final User user) {
		final NovaView view = new NovaView("answer/variant_by_user");
		view.setKey(user.getUsername(), session.get_id(), "preparation");
		return collectUnansweredQuestionIds(getPreparationQuestionIds(session, user), view);
	}

	private List<String> getPreparationQuestionIds(final Session session, final User user) {
		NovaView view = new NovaView("skill_question/preparation_question_by_session_for_all");
		view.setStartKeyArray(session.get_id());
		view.setEndKeyArray(session.get_id(), "{}");
		return collectQuestionIds(view);
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
	public void publishAllQuestions(final Session session, final boolean publish) {
		final List<Question> questions = getQuestions(new NovaView("skill_question/by_session"), session);
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
	public void deleteAllQuestionsAnswers(final Session session) {
		final List<Question> questions = getQuestions(new NovaView("skill_question/by_session"), session);
		for (final Question q : questions) {
			deleteAnswers(q);
		}
	}

	@Override
	public int getLearningProgress(final Session session) {
		// Note: we have to use this many views because our CouchDB version does not support
		// advanced features like summing over lists. Thus, we have to do it all by ourselves...
		final NovaView maximumValueView = new NovaView("learning_progress_maximum_value/max");
		final NovaView answerSumView = new NovaView("learning_progress_user_values/sum");
		final NovaView answerDocumentCountView = new NovaView("learning_progress_course_answers/count");
		maximumValueView.setKey(session.get_id());
		answerSumView.setStartKeyArray(session.get_id());
		answerSumView.setEndKeyArray(session.get_id(), "{}");
		answerDocumentCountView.setStartKeyArray(session.get_id());
		answerDocumentCountView.setEndKeyArray(session.get_id(), "{}");
		answerDocumentCountView.setGroup(true);

		final List<Document> maximumValueResult = getDatabase().view(maximumValueView).getResults();
		final List<Document> answerSumResult = getDatabase().view(answerSumView).getResults();
		final List<Document> answerDocumentCountResult = getDatabase().view(answerDocumentCountView).getResults();

		if (maximumValueResult.isEmpty() || answerSumResult.isEmpty() || answerDocumentCountResult.isEmpty()) {
			return 0;
		}

		final double courseMaximumValue = maximumValueResult.get(0).getInt("value");
		final double userTotalValue = answerSumResult.get(0).getInt("value");
		final double numUsers = answerDocumentCountResult.size();
		if (courseMaximumValue == 0 || numUsers == 0) {
			return 0;
		}
		final double courseAverageValue = userTotalValue / numUsers;
		final double courseProgress = courseAverageValue / courseMaximumValue;
		return (int)Math.round(courseProgress * 100);
	}

	@Override
	public SimpleEntry<Integer,Integer> getMyLearningProgress(final Session session, final User user) {
		final int courseProgress = getLearningProgress(session);

		final NovaView maximumValueView = new NovaView("learning_progress_maximum_value/max");
		final NovaView answerSumView = new NovaView("learning_progress_user_values/sum");
		maximumValueView.setKey(session.get_id());
		answerSumView.setKey(session.get_id(), user.getUsername());

		final List<Document> maximumValueResult = getDatabase().view(maximumValueView).getResults();
		final List<Document> answerSumResult = getDatabase().view(answerSumView).getResults();

		if (maximumValueResult.isEmpty() || answerSumResult.isEmpty()) {
			return new AbstractMap.SimpleEntry<Integer, Integer>(0, courseProgress);
		}

		final double courseMaximumValue = maximumValueResult.get(0).getInt("value");
		final double userTotalValue = answerSumResult.get(0).getInt("value");

		if (courseMaximumValue == 0) {
			return new AbstractMap.SimpleEntry<Integer, Integer>(0, courseProgress);
		}
		final double myProgress = userTotalValue / courseMaximumValue;

		return new AbstractMap.SimpleEntry<Integer, Integer>((int)Math.round(myProgress*100), courseProgress);
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
}
