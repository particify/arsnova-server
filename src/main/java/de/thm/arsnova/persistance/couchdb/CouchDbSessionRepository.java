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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.persistance.couchdb;

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.SessionInfo;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.VisitedSession;
import de.thm.arsnova.entities.transport.Comment;
import de.thm.arsnova.entities.transport.ImportExportSession;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.persistance.LogEntryRepository;
import de.thm.arsnova.persistance.MotdRepository;
import de.thm.arsnova.persistance.SessionRepository;
import de.thm.arsnova.services.SessionService;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
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

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CouchDbSessionRepository extends CouchDbRepositorySupport<Session> implements SessionRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbSessionRepository.class);

	@Autowired
	private SessionService sessionService;

	@Autowired
	private LogEntryRepository dbLogger;

	@Autowired
	private MotdRepository motdRepository;

	public CouchDbSessionRepository(CouchDbConnector db, boolean createIfNotExists) {
		super(Session.class, db, createIfNotExists);
	}

	@Override
	@Cacheable("sessions")
	public Session getSessionFromKeyword(final String keyword) {
		final List<Session> session = queryView("by_keyword", keyword);

		return !session.isEmpty() ? session.get(0) : null;
	}

	@Override
	@Cacheable("sessions")
	public Session getSessionFromId(final String sessionId) {
		return get(sessionId);
	}

	@Override
	@Caching(evict = @CacheEvict(cacheNames = "sessions", key = "#result.keyword"))
	public Session saveSession(final User user, final Session session) {
		session.setKeyword(sessionService.generateKeyword());
		session.setCreator(user.getUsername());
		session.setActive(true);
		session.setFeedbackLock(false);

		try {
			db.create(session);
		} catch (final IllegalArgumentException e) {
			logger.error("Could not save session to database.", e);
		}

		return session.getId() != null ? session : null;
	}

	@Override
	public boolean sessionKeyAvailable(final String keyword) {
		return getSessionFromKeyword(keyword) == null;
	}

	private String getSessionKeyword(final String internalSessionId) throws IOException {
		final Session session = get(internalSessionId);
		if (session == null) {
			logger.error("No session found for internal id {}.", internalSessionId);

			return null;
		}

		return session.getKeyword();
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
			update(session);

			return session;
		} catch (final UpdateConflictException e) {
			logger.error("Failed to update lastOwnerActivity for session {}.", session, e);
			return session;
		}
	}

	@Override
	public List<Session> getVisitedSessionsForUsername(String username, final int start, final int limit) {
		final int qSkip = start > 0 ? start : -1;
		final int qLimit = limit > 0 ? limit : -1;

		try {
			ViewResult visitedSessionResult = db.queryView(createQuery("visited_sessions_by_user")
					.designDocId("_design/LoggedIn").key(username));
			List<Session> visitedSessions = visitedSessionResult.getRows().stream().map(vs -> {
				final Session s = new Session();
				s.setId(vs.getValueAsNode().get("_id").asText());
				s.setKeyword(vs.getValueAsNode().get("keyword").asText());
				s.setName(vs.getValueAsNode().get("name").asText());

				return s;
			}).collect(Collectors.toList());

			if (visitedSessions.isEmpty()) {
				return new ArrayList<>();
			}

			// Filter sessions that don't exist anymore, also filter my own sessions
			final List<Session> result = new ArrayList<>();
			final List<Session> filteredSessions = new ArrayList<>();
			for (final Session s : visitedSessions) {
				try {
					/* FIXME: caching (getSessionFromKeyword) */
					final Session session = getSessionFromKeyword(s.getKeyword());
					if (session != null && !(session.getCreator().equals(username))) {
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
				List<VisitedSession> newVisitedSessions = new ArrayList<>();
				for (final Session s : result) {
					newVisitedSessions.add(new VisitedSession(s));
				}

			try {
				final LoggedIn loggedIn = db.get(LoggedIn.class, visitedSessionResult.getRows().get(0).getId());
				loggedIn.setVisitedSessions(newVisitedSessions);
				db.update(loggedIn);
			} catch (UpdateConflictException e) {
				logger.error("Could not clean up LoggedIn document of {}.", username, e);
			}

			return result;
		} catch (DocumentNotFoundException e) {
			return new ArrayList<>();
		}
	}

	@Override
	public List<SessionInfo> getMyVisitedSessionsInfo(final User user, final int start, final int limit) {
		List<Session> sessions = getVisitedSessionsForUsername(user.getUsername(), start, limit);
		if (sessions.isEmpty()) {
			return new ArrayList<>();
		}
		return this.getInfosForVisitedSessions(sessions, user);
	}

	@Override
	public List<Session> getCourseSessions(final List<Course> courses) {
		return queryView("by_courseid",
				ComplexKey.of(courses.stream().map(Course::getId).collect(Collectors.toList())));
	}

	@Override
	@CachePut(value = "sessions")
	public Session updateSession(final Session session) {
		try {
			update(session);

			return session;
		} catch (final UpdateConflictException e) {
			logger.error("Could not update session {}.", session, e);
		}

		return null;
	}

	@Override
	@Caching(evict = { @CacheEvict("sessions"), @CacheEvict(cacheNames = "sessions", key = "#p0.keyword") })
	public Session changeSessionCreator(final Session session, final String newCreator) {
		Session s = get(session.getId());
		s.setCreator(newCreator);
		try {
			update(s);
		} catch (final UpdateConflictException e) {
			logger.error("Could not update creator for session {}.", session, e);
		}

		return s;
	}

	@Override
	@Caching(evict = { @CacheEvict("sessions"), @CacheEvict(cacheNames = "sessions", key = "#p0.keyword") })
	public int[] deleteSession(final Session session) {
		/* FIXME: not yet migrated - move to service layer */
		throw new UnsupportedOperationException();
//		int[] count = new int[] {0, 0};
//		try {
//			count = deleteAllQuestionsWithAnswers(session);
//			remove(session);
//			logger.debug("Deleted session document {} and related data.", session.getId());
//			dbLogger.log("delete", "type", "session", "id", session.getId());
//		} catch (final Exception e) {
//			/* TODO: improve error handling */
//			logger.error("Could not delete session {}.", session, e);
//		}
//
//		return count;
	}

	@Override
	public int[] deleteInactiveGuestSessions(long lastActivityBefore) {
		ViewResult result = db.queryView(
				createQuery("by_lastactivity_for_guests").endKey(lastActivityBefore));
		int[] count = new int[3];

		for (ViewResult.Row row : result.getRows()) {
			Session s = new Session();
			s.setId(row.getId());
			s.setRevision(row.getValueAsNode().get("_rev").asText());
			int[] qaCount = deleteSession(s);
			count[1] += qaCount[0];
			count[2] += qaCount[1];
		}

		if (!result.isEmpty()) {
			logger.info("Deleted {} inactive guest sessions.", result.getSize());
			dbLogger.log("cleanup", "type", "session", "sessionCount", result.getSize(), "questionCount", count[1], "answerCount", count[2]);
		}
		count[0] = result.getSize();

		return count;
	}

	@Override
	public SessionInfo importSession(User user, ImportExportSession importSession) {
		/* FIXME: not yet migrated - move to service layer */
		throw new UnsupportedOperationException();
//		final Session session = this.saveSession(user, importSession.generateSessionEntity(user));
//		List<Document> questions = new ArrayList<>();
//		// We need to remember which answers belong to which question.
//		// The answers need a questionId, so we first store the questions to get the IDs.
//		// Then we update the answer objects and store them as well.
//		Map<Document, ImportExportSession.ImportExportContent> mapping = new HashMap<>();
//		// Later, generate all answer documents
//		List<Document> answers = new ArrayList<>();
//		// We can then push answers together with comments in one large bulk request
//		List<Document> interposedQuestions = new ArrayList<>();
//		// Motds shouldn't be forgotten, too
//		List<Document> motds = new ArrayList<>();
//		try {
//			// add session id to all questions and generate documents
//			for (ImportExportSession.ImportExportContent question : importSession.getQuestions()) {
//				Document doc = toQuestionDocument(session, question);
//				question.setSessionId(session.getId());
//				questions.add(doc);
//				mapping.put(doc, question);
//			}
//			database.bulkSaveDocuments(questions.toArray(new Document[questions.size()]));
//
//			// bulk import answers together with interposed questions
//			for (Map.Entry<Document, ImportExportSession.ImportExportContent> entry : mapping.entrySet()) {
//				final Document doc = entry.getKey();
//				final ImportExportSession.ImportExportContent question = entry.getValue();
//				question.setId(doc.getId());
//				question.setRevision(doc.getRev());
//				for (de.thm.arsnova.entities.transport.Answer answer : question.getAnswers()) {
//					final Answer a = answer.generateAnswerEntity(user, question);
//					final Document answerDoc = new Document();
//					answerDoc.put("type", "skill_question_answer");
//					answerDoc.put("sessionId", a.getSessionId());
//					answerDoc.put("questionId", a.getQuestionId());
//					answerDoc.put("answerSubject", a.getAnswerSubject());
//					answerDoc.put("questionVariant", a.getQuestionVariant());
//					answerDoc.put("questionValue", a.getQuestionValue());
//					answerDoc.put("answerText", a.getAnswerText());
//					answerDoc.put("answerTextRaw", a.getAnswerTextRaw());
//					answerDoc.put("timestamp", a.getTimestamp());
//					answerDoc.put("piRound", a.getPiRound());
//					answerDoc.put("abstention", a.isAbstention());
//					answerDoc.put("successfulFreeTextAnswer", a.isSuccessfulFreeTextAnswer());
//					// we do not store the user's name
//					answerDoc.put("user", "");
//					answers.add(answerDoc);
//				}
//			}
//			for (de.thm.arsnova.entities.transport.Comment i : importSession.getFeedbackQuestions()) {
//				final Document q = new Document();
//				q.put("type", "interposed_question");
//				q.put("sessionId", session.getId());
//				q.put("subject", i.getSubject());
//				q.put("text", i.getText());
//				q.put("timestamp", i.getTimestamp());
//				q.put("read", i.isRead());
//				// we do not store the creator's name
//				q.put("creator", "");
//				interposedQuestions.add(q);
//			}
//			for (Motd m : importSession.getMotds()) {
//				final Document d = new Document();
//				d.put("type", "motd");
//				d.put("motdkey", m.getMotdkey());
//				d.put("title", m.getTitle());
//				d.put("text", m.getText());
//				d.put("audience", m.getAudience());
//				d.put("sessionkey", session.getKeyword());
//				d.put("startdate", String.valueOf(m.getStartdate().getTime()));
//				d.put("enddate", String.valueOf(m.getEnddate().getTime()));
//				motds.add(d);
//			}
//			List<Document> documents = new ArrayList<>(answers);
//			database.bulkSaveDocuments(interposedQuestions.toArray(new Document[interposedQuestions.size()]));
//			database.bulkSaveDocuments(motds.toArray(new Document[motds.size()]));
//			database.bulkSaveDocuments(documents.toArray(new Document[documents.size()]));
//		} catch (IOException e) {
//			logger.error("Could not import session.", e);
//			// Something went wrong, delete this session since we do not want a partial import
//			this.deleteSession(session);
//			return null;
//		}
//		return this.calculateSessionInfo(importSession, session);
	}

	@Override
	public ImportExportSession exportSession(String sessionkey, Boolean withAnswers, Boolean withFeedbackQuestions) {
		/* FIXME: not yet migrated - move to service layer */
		throw new UnsupportedOperationException();
//		ImportExportSession importExportSession = new ImportExportSession();
//		Session session = getDatabaseDao().getSessionFromKeyword(sessionkey);
//		importExportSession.setSessionFromSessionObject(session);
//		List<Content> questionList = getDatabaseDao().getAllSkillQuestions(session);
//		for (Content question : questionList) {
//			List<de.thm.arsnova.entities.transport.Answer> answerList = new ArrayList<>();
//			if (withAnswers) {
//				for (Answer a : this.getDatabaseDao().getAllAnswers(question)) {
//					de.thm.arsnova.entities.transport.Answer transportAnswer = new de.thm.arsnova.entities.transport.Answer(a);
//					answerList.add(transportAnswer);
//				}
//				// getAllAnswers does not grep for whole answer object so i need to add empty entries for abstentions
//				int i = this.getDatabaseDao().getAbstentionAnswerCount(question.getId());
//				for (int b = 0; b < i; b++) {
//					de.thm.arsnova.entities.transport.Answer ans = new de.thm.arsnova.entities.transport.Answer();
//					ans.setAnswerSubject("");
//					ans.setAnswerImage("");
//					ans.setAnswerText("");
//					ans.setAbstention(true);
//					answerList.add(ans);
//				}
//			}
//			importExportSession.addQuestionWithAnswers(question, answerList);
//		}
//		if (withFeedbackQuestions) {
//			List<de.thm.arsnova.entities.transport.Comment> interposedQuestionList = new ArrayList<>();
//			for (Comment i : getDatabaseDao().getInterposedQuestions(session, 0, 0)) {
//				de.thm.arsnova.entities.transport.Comment transportInterposedQuestion = new de.thm.arsnova.entities.transport.Comment(i);
//				interposedQuestionList.add(transportInterposedQuestion);
//			}
//			importExportSession.setFeedbackQuestions(interposedQuestionList);
//		}
//		if (withAnswers) {
//			importExportSession.setSessionInfo(this.calculateSessionInfo(importExportSession, session));
//		}
//		importExportSession.setMotds(motdRepository.getMotdsForSession(session.getKeyword()));
//		return importExportSession;
	}

	private SessionInfo calculateSessionInfo(ImportExportSession importExportSession, Session session) {
		int unreadComments = 0;
		int numUnanswered = 0;
		int numAnswers = 0;
		for (Comment i : importExportSession.getFeedbackQuestions()) {
			if (!i.isRead()) {
				unreadComments++;
			}
		}
		for (ImportExportSession.ImportExportContent question : importExportSession.getQuestions()) {
			numAnswers += question.getAnswers().size();
			if (question.getAnswers().isEmpty()) {
				numUnanswered++;
			}
		}
		final SessionInfo info = new SessionInfo(session);
		info.setNumQuestions(importExportSession.getQuestions().size());
		info.setNumUnanswered(numUnanswered);
		info.setNumAnswers(numAnswers);
		info.setNumInterposed(importExportSession.getFeedbackQuestions().size());
		info.setNumUnredInterposed(unreadComments);
		return info;
	}

	@Override
	public List<Session> getMySessions(final User user, final int start, final int limit) {
		return getSessionsForUsername(user.getUsername(), start, limit);
	}

	@Override
	public List<Session> getSessionsForUsername(String username, final int start, final int limit) {
		final int qSkip = start > 0 ? start : -1;
		final int qLimit = limit > 0 ? limit : -1;

		/* TODO: Only load IDs and check against cache for data. */
		List<Session> sessions = db.queryView(
				createQuery("partial_by_sessiontype_creator_name")
						.skip(qSkip)
						.limit(qLimit)
						.startKey(ComplexKey.of(null, username))
						.endKey(ComplexKey.of(null, username, ComplexKey.emptyObject()))
						.includeDocs(true),
				Session.class);

		return sessions;
	}

	@Override
	public List<Session> getPublicPoolSessions() {
		// TODO replace with new view
		return queryView("partial_by_ppsubject_name_for_publicpool");
	}

	@Override
	public List<SessionInfo> getPublicPoolSessionsInfo() {
		final List<Session> sessions = this.getPublicPoolSessions();
		return getInfosForSessions(sessions);
	}

	@Override
	public List<Session> getMyPublicPoolSessions(final User user) {
		/* TODO: Only load IDs and check against cache for data. */
		return db.queryView(
				createQuery("partial_by_sessiontype_creator_name")
						.startKey(ComplexKey.of("public_pool", user.getUsername()))
						.endKey(ComplexKey.of("public_pool", user.getUsername(), ComplexKey.emptyObject()))
						.includeDocs(true),
				Session.class);
	}

	@Override
	public List<SessionInfo> getMyPublicPoolSessionsInfo(final User user) {
		final List<Session> sessions = this.getMyPublicPoolSessions(user);
		if (sessions.isEmpty()) {
			return new ArrayList<>();
		}
		return getInfosForSessions(sessions);
	}

	@Override
	public List<SessionInfo> getMySessionsInfo(final User user, final int start, final int limit) {
		final List<Session> sessions = this.getMySessions(user, start, limit);
		if (sessions.isEmpty()) {
			return new ArrayList<>();
		}
		return getInfosForSessions(sessions);
	}

	private List<SessionInfo> getInfosForSessions(final List<Session> sessions) {
		List<String> sessionIds = sessions.stream().map(Session::getId).collect(Collectors.toList());
		final ViewQuery questionCountView = createQuery("by_sessionid").designDocId("_design/Content")
				.group(true).keys(sessionIds);
		final ViewQuery answerCountView = createQuery("by_sessionid").designDocId("_design/Answer")
				.group(true).keys(sessionIds);
		final ViewQuery commentCountView = createQuery("by_sessionid").designDocId("_design/Comment")
				.group(true).keys(sessionIds);
		final ViewQuery unreadCommentCountView = createQuery("by_sessionid_read").designDocId("_design/Comment")
				.group(true).keys(sessions.stream().map(session -> ComplexKey.of(session.getId(), false)).collect(Collectors.toList()));

		return getSessionInfoData(sessions, questionCountView, answerCountView, commentCountView, unreadCommentCountView);
	}

	private List<SessionInfo> getInfosForVisitedSessions(final List<Session> sessions, final User user) {
		final ViewQuery answeredQuestionsView = createQuery("by_user_sessionid").designDocId("_design/Answer")
				.keys(sessions.stream().map(session -> ComplexKey.of(user.getUsername(), session.getId())).collect(Collectors.toList()));
		final ViewQuery questionIdsView = createQuery("by_sessionid").designDocId("_design/Content")
				.keys(sessions.stream().map(Session::getId).collect(Collectors.toList()));

		return getVisitedSessionInfoData(sessions, answeredQuestionsView, questionIdsView);
	}

	private List<SessionInfo> getVisitedSessionInfoData(List<Session> sessions,
														ViewQuery answeredQuestionsView, ViewQuery questionIdsView) {
		final Map<String, Set<String>> answeredQuestionsMap = new HashMap<>();
		final Map<String, Set<String>> questionIdMap = new HashMap<>();

		// Maps a session ID to a set of question IDs of answered questions of that session
		for (final ViewResult.Row row : db.queryView(answeredQuestionsView).getRows()) {
			final String sessionId = row.getKey();
			final String questionId = row.getValue();
			Set<String> questionIdsInSession = answeredQuestionsMap.get(sessionId);
			if (questionIdsInSession == null) {
				questionIdsInSession = new HashSet<>();
			}
			questionIdsInSession.add(questionId);
			answeredQuestionsMap.put(sessionId, questionIdsInSession);
		}

		// Maps a session ID to a set of question IDs of that session
		for (final ViewResult.Row row : db.queryView(questionIdsView).getRows()) {
			final String sessionId = row.getKey();
			final String questionId = row.getId();
			Set<String> questionIdsInSession = questionIdMap.get(sessionId);
			if (questionIdsInSession == null) {
				questionIdsInSession = new HashSet<>();
			}
			questionIdsInSession.add(questionId);
			questionIdMap.put(sessionId, questionIdsInSession);
		}

		// For each session, count the question IDs that are not yet answered
		Map<String, Integer> unansweredQuestionsCountMap = new HashMap<>();
		for (final Session s : sessions) {
			if (!questionIdMap.containsKey(s.getId())) {
				continue;
			}
			// Note: create a copy of the first set so that we don't modify the contents in the original set
			Set<String> questionIdsInSession = new HashSet<>(questionIdMap.get(s.getId()));
			Set<String> answeredQuestionIdsInSession = answeredQuestionsMap.get(s.getId());
			if (answeredQuestionIdsInSession == null) {
				answeredQuestionIdsInSession = new HashSet<>();
			}
			questionIdsInSession.removeAll(answeredQuestionIdsInSession);
			unansweredQuestionsCountMap.put(s.getId(), questionIdsInSession.size());
		}

		List<SessionInfo> sessionInfos = new ArrayList<>();
		for (Session session : sessions) {
			int numUnanswered = 0;

			if (unansweredQuestionsCountMap.containsKey(session.getId())) {
				numUnanswered = unansweredQuestionsCountMap.get(session.getId());
			}
			SessionInfo info = new SessionInfo(session);
			info.setNumUnanswered(numUnanswered);
			sessionInfos.add(info);
		}
		return sessionInfos;
	}

	private List<SessionInfo> getSessionInfoData(final List<Session> sessions,
												 final ViewQuery questionCountView,
												 final ViewQuery answerCountView,
												 final ViewQuery commentCountView,
												 final ViewQuery unreadCommentCountView) {
		Map<String, Integer> questionCountMap = db.queryView(questionCountView).getRows()
				.stream().map(row -> new AbstractMap.SimpleImmutableEntry<>(row.getKey(), row.getValueAsInt()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		Map<String, Integer> answerCountMap = db.queryView(answerCountView).getRows()
				.stream().map(row -> new AbstractMap.SimpleImmutableEntry<>(row.getKey(), row.getValueAsInt()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		Map<String, Integer> commentCountMap = db.queryView(commentCountView).getRows()
				.stream().map(row -> new AbstractMap.SimpleImmutableEntry<>(row.getKey(), row.getValueAsInt()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		Map<String, Integer> unreadCommentCountMap = db.queryView(unreadCommentCountView).getRows()
				.stream().map(row -> new AbstractMap.SimpleImmutableEntry<>(row.getKey(), row.getValueAsInt()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		List<SessionInfo> sessionInfos = new ArrayList<>();
		for (Session session : sessions) {
			int numQuestions = 0;
			int numAnswers = 0;
			int numComments = 0;
			int numUnreadComments = 0;
			if (questionCountMap.containsKey(session.getId())) {
				numQuestions = questionCountMap.get(session.getId());
			}
			if (answerCountMap.containsKey(session.getId())) {
				numAnswers = answerCountMap.get(session.getId());
			}
			if (commentCountMap.containsKey(session.getId())) {
				numComments = commentCountMap.get(session.getId());
			}
			if (unreadCommentCountMap.containsKey(session.getId())) {
				numUnreadComments = unreadCommentCountMap.get(session.getId());
			}

			SessionInfo info = new SessionInfo(session);
			info.setNumQuestions(numQuestions);
			info.setNumAnswers(numAnswers);
			info.setNumInterposed(numComments);
			info.setNumUnredInterposed(numUnreadComments);
			sessionInfos.add(info);
		}
		return sessionInfos;
	}

	@Override
	public LoggedIn registerAsOnlineUser(final User user, final Session session) {
		LoggedIn loggedIn = new LoggedIn();
		try {
			List<LoggedIn> loggedInList = db.queryView(createQuery("all").designDocId("_design/LoggedIn").key(user.getUsername()), LoggedIn.class);

			if (!loggedInList.isEmpty()) {
				loggedIn = loggedInList.get(0);

				/* Do not clutter CouchDB. Only update once every 3 hours per session. */
				if (loggedIn.getSessionId().equals(session.getId()) && loggedIn.getTimestamp() > System.currentTimeMillis() - 3 * 3600000) {
					return loggedIn;
				}
			}

			loggedIn.setUser(user.getUsername());
			loggedIn.setSessionId(session.getId());
			loggedIn.addVisitedSession(session);
			loggedIn.updateTimestamp();

			if (loggedIn.getId() == null) {
				db.create(loggedIn);
			} else {
				db.update(loggedIn);
			}
		} catch (final UpdateConflictException e) {
			logger.error("Could not save LoggedIn document of {}.", user.getUsername(), e);
		}

		return loggedIn;
	}
}
