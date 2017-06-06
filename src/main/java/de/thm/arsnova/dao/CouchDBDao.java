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
import com.fourspaces.couchdb.View;
import com.fourspaces.couchdb.ViewResults;
import com.google.common.collect.Lists;
import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.domain.CourseScore;
import de.thm.arsnova.entities.*;
import de.thm.arsnova.persistance.ContentRepository;
import de.thm.arsnova.persistance.LogEntryRepository;
import de.thm.arsnova.persistance.MotdRepository;
import de.thm.arsnova.persistance.SessionRepository;
import de.thm.arsnova.services.ISessionService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Database implementation based on CouchDB.
 *
 * Note to developers:
 *
 * This class makes use of Spring Framework's caching annotations. When you are about to add new functionality,
 * you should also think about the possibility of caching. Ideally, your methods should be dependent on domain
 * objects like Session or Content, which can be used as cache keys. Relying on plain String objects as a key, e.g.
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
public class CouchDBDao implements IDatabaseDao {

	private static final int BULK_PARTITION_SIZE = 500;

	@Autowired
	private ISessionService sessionService;

	@Autowired
	private LogEntryRepository dbLogger;

	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private MotdRepository motdRepository;

	@Autowired
	private ContentRepository contentRepository;

	private String databaseHost;
	private int databasePort;
	private String databaseName;
	private Database database;

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

	private void deleteDocument(final String documentId) throws IOException {
		final Document d = getDatabase().getDocument(documentId);
		getDatabase().deleteDocument(d);
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

	private boolean isEmptyResults(final ViewResults results) {
		return results == null || results.getResults().isEmpty() || results.getJSONArray("rows").isEmpty();
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
