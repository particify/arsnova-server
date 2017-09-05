/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
import de.thm.arsnova.entities.migration.v2.Comment;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.migration.v2.Session;
import de.thm.arsnova.entities.migration.v2.SessionInfo;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.VisitedSession;
import de.thm.arsnova.entities.transport.ImportExportSession;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.persistance.LogEntryRepository;
import de.thm.arsnova.persistance.MotdRepository;
import de.thm.arsnova.persistance.SessionRepository;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CouchDbSessionRepository extends CouchDbCrudRepository<Session> implements SessionRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbSessionRepository.class);

	@Autowired
	private LogEntryRepository dbLogger;

	@Autowired
	private MotdRepository motdRepository;

	public CouchDbSessionRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Session.class, db, "by_keyword", createIfNotExists);
	}

	@Override
	@Cacheable("sessions")
	public Session findByKeyword(final String keyword) {
		final List<Session> session = queryView("by_keyword", keyword);

		return !session.isEmpty() ? session.get(0) : null;
	}

	/* TODO: Move to service layer. */
	private String getSessionKeyword(final String internalSessionId) throws IOException {
		final Session session = get(internalSessionId);
		if (session == null) {
			logger.error("No session found for internal id {}.", internalSessionId);

			return null;
		}

		return session.getKeyword();
	}

	@Override
	public List<Session> findVisitedByUsername(final String username, final int start, final int limit) {
		final int qSkip = start > 0 ? start : -1;
		final int qLimit = limit > 0 ? limit : -1;

		try {
			final ViewResult visitedSessionResult = db.queryView(createQuery("visited_sessions_by_user")
					.designDocId("_design/LoggedIn").key(username));
			final List<Session> visitedSessions = visitedSessionResult.getRows().stream().map(vs -> {
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
					final Session session = findByKeyword(s.getKeyword());
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
			final List<VisitedSession> newVisitedSessions = new ArrayList<>();
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
		} catch (final DocumentNotFoundException e) {
			return new ArrayList<>();
		}
	}

	@Override
	public List<SessionInfo> findInfoForVisitedByUser(final User user, final int start, final int limit) {
		final List<Session> sessions = findVisitedByUsername(user.getUsername(), start, limit);
		if (sessions.isEmpty()) {
			return new ArrayList<>();
		}
		return this.getInfosForVisitedSessions(sessions, user);
	}

	@Override
	public List<Session> findSessionsByCourses(final List<Course> courses) {
		return queryView("by_courseid",
				ComplexKey.of(courses.stream().map(Course::getId).collect(Collectors.toList())));
	}

	@Override
	public List<Session> findInactiveGuestSessionsMetadata(final long lastActivityBefore) {
		final ViewResult result = db.queryView(
				createQuery("by_lastactivity_for_guests").endKey(lastActivityBefore));
		final int[] count = new int[3];

		List<Session> sessions = new ArrayList<>();
		for (final ViewResult.Row row : result.getRows()) {
			final Session s = new Session();
			s.setId(row.getId());
			s.setRevision(row.getValueAsNode().get("_rev").asText());
			sessions.add(s);
		}

		return sessions;
	}

	/* TODO: Move to service layer. */
	@Override
	public SessionInfo importSession(final User user, final ImportExportSession importSession) {
		/* FIXME: not yet migrated - move to service layer */
		throw new UnsupportedOperationException();
//		final Session session = this.saveSession(user, importSession.generateSessionEntity(user));
//		final List<Document> questions = new ArrayList<>();
//		// We need to remember which answers belong to which question.
//		// The answers need a questionId, so we first store the questions to get the IDs.
//		// Then we update the answer objects and store them as well.
//		final Map<Document, ImportExportSession.ImportExportContent> mapping = new HashMap<>();
//		// Later, generate all answer documents
//		List<Document> answers = new ArrayList<>();
//		// We can then push answers together with comments in one large bulk request
//		List<Document> interposedQuestions = new ArrayList<>();
//		// Motds shouldn't be forgotten, too
//		List<Document> motds = new ArrayList<>();
//		try {
//			// add session id to all questions and generate documents
//			for (final ImportExportSession.ImportExportContent question : importSession.getQuestions()) {
//				final Document doc = toQuestionDocument(session, question);
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
//				for (final de.thm.arsnova.entities.transport.Answer answer : question.getAnswers()) {
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
//			for (final de.thm.arsnova.entities.transport.Comment i : importSession.getFeedbackQuestions()) {
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
//			for (final Motd m : importSession.getMotds()) {
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
//			final List<Document> documents = new ArrayList<>(answers);
//			database.bulkSaveDocuments(interposedQuestions.toArray(new Document[interposedQuestions.size()]));
//			database.bulkSaveDocuments(motds.toArray(new Document[motds.size()]));
//			database.bulkSaveDocuments(documents.toArray(new Document[documents.size()]));
//		} catch (final IOException e) {
//			logger.error("Could not import session.", e);
//			// Something went wrong, delete this session since we do not want a partial import
//			this.deleteSession(session);
//			return null;
//		}
//		return this.calculateSessionInfo(importSession, session);
	}

	/* TODO: Move to service layer. */
	@Override
	public ImportExportSession exportSession(
			final String sessionkey,
			final Boolean withAnswers,
			final Boolean withFeedbackQuestions) {
		/* FIXME: not yet migrated - move to service layer */
		throw new UnsupportedOperationException();
//		final ImportExportSession importExportSession = new ImportExportSession();
//		final Session session = getDatabaseDao().getSessionFromKeyword(sessionkey);
//		importExportSession.setSessionFromSessionObject(session);
//		final List<Content> questionList = getDatabaseDao().getAllSkillQuestions(session);
//		for (final Content question : questionList) {
//			final List<de.thm.arsnova.entities.transport.Answer> answerList = new ArrayList<>();
//			if (withAnswers) {
//				for (final Answer a : this.getDatabaseDao().getAllAnswers(question)) {
//					final de.thm.arsnova.entities.transport.Answer transportAnswer = new de.thm.arsnova.entities.transport.Answer(a);
//					answerList.add(transportAnswer);
//				}
//				// getAllAnswers does not grep for whole answer object so i need to add empty entries for abstentions
//				int i = this.getDatabaseDao().getAbstentionAnswerCount(question.getId());
//				for (int b = 0; b < i; b++) {
//					final de.thm.arsnova.entities.transport.Answer ans = new de.thm.arsnova.entities.transport.Answer();
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
//			final List<de.thm.arsnova.entities.transport.Comment> interposedQuestionList = new ArrayList<>();
//			for (final Comment i : getDatabaseDao().getInterposedQuestions(session, 0, 0)) {
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

	/* TODO: Move to service layer. */
	private SessionInfo calculateSessionInfo(final ImportExportSession importExportSession, final Session session) {
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
	public List<Session> findByUser(final User user, final int start, final int limit) {
		return findByUsername(user.getUsername(), start, limit);
	}

	@Override
	public List<Session> findByUsername(final String username, final int start, final int limit) {
		final int qSkip = start > 0 ? start : -1;
		final int qLimit = limit > 0 ? limit : -1;

		/* TODO: Only load IDs and check against cache for data. */
		return db.queryView(
				createQuery("partial_by_sessiontype_creator_name")
						.skip(qSkip)
						.limit(qLimit)
						.startKey(ComplexKey.of(null, username))
						.endKey(ComplexKey.of(null, username, ComplexKey.emptyObject()))
						.includeDocs(true),
				Session.class);
	}

	@Override
	public List<Session> findAllForPublicPool() {
		// TODO replace with new view
		return queryView("partial_by_ppsubject_name_for_publicpool");
	}

	@Override
	public List<SessionInfo> findInfosForPublicPool() {
		final List<Session> sessions = this.findAllForPublicPool();
		return getInfosForSessions(sessions);
	}

	@Override
	public List<Session> findForPublicPoolByUser(final User user) {
		/* TODO: Only load IDs and check against cache for data. */
		return db.queryView(
				createQuery("partial_by_sessiontype_creator_name")
						.startKey(ComplexKey.of("public_pool", user.getUsername()))
						.endKey(ComplexKey.of("public_pool", user.getUsername(), ComplexKey.emptyObject()))
						.includeDocs(true),
				Session.class);
	}

	/* TODO: Move to service layer. */
	@Override
	public List<SessionInfo> findInfosForPublicPoolByUser(final User user) {
		final List<Session> sessions = this.findForPublicPoolByUser(user);
		if (sessions.isEmpty()) {
			return new ArrayList<>();
		}
		return getInfosForSessions(sessions);
	}

	/* TODO: Move to service layer. */
	@Override
	public List<SessionInfo> getMySessionsInfo(final User user, final int start, final int limit) {
		final List<Session> sessions = this.findByUser(user, start, limit);
		if (sessions.isEmpty()) {
			return new ArrayList<>();
		}
		return getInfosForSessions(sessions);
	}

	/* TODO: Move to service layer. */
	private List<SessionInfo> getInfosForSessions(final List<Session> sessions) {
		final List<String> sessionIds = sessions.stream().map(Session::getId).collect(Collectors.toList());
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

	/* TODO: Move to service layer. */
	private List<SessionInfo> getInfosForVisitedSessions(final List<Session> sessions, final User user) {
		final ViewQuery answeredQuestionsView = createQuery("by_user_sessionid").designDocId("_design/Answer")
				.keys(sessions.stream().map(session -> ComplexKey.of(user.getUsername(), session.getId())).collect(Collectors.toList()));
		final ViewQuery contentIdsView = createQuery("by_sessionid").designDocId("_design/Content")
				.keys(sessions.stream().map(Session::getId).collect(Collectors.toList()));

		return getVisitedSessionInfoData(sessions, answeredQuestionsView, contentIdsView);
	}

	/* TODO: Move to service layer. */
	private List<SessionInfo> getVisitedSessionInfoData(
			final List<Session> sessions,
			final ViewQuery answeredQuestionsView,
			final ViewQuery contentIdsView) {
		final Map<String, Set<String>> answeredQuestionsMap = new HashMap<>();
		final Map<String, Set<String>> contentIdMap = new HashMap<>();

		// Maps a session ID to a set of question IDs of answered questions of that session
		for (final ViewResult.Row row : db.queryView(answeredQuestionsView).getRows()) {
			final String sessionId = row.getKey();
			final String contentId = row.getValue();
			Set<String> contentIdsInSession = answeredQuestionsMap.get(sessionId);
			if (contentIdsInSession == null) {
				contentIdsInSession = new HashSet<>();
			}
			contentIdsInSession.add(contentId);
			answeredQuestionsMap.put(sessionId, contentIdsInSession);
		}

		// Maps a session ID to a set of question IDs of that session
		for (final ViewResult.Row row : db.queryView(contentIdsView).getRows()) {
			final String sessionId = row.getKey();
			final String contentId = row.getId();
			Set<String> contentIdsInSession = contentIdMap.get(sessionId);
			if (contentIdsInSession == null) {
				contentIdsInSession = new HashSet<>();
			}
			contentIdsInSession.add(contentId);
			contentIdMap.put(sessionId, contentIdsInSession);
		}

		// For each session, count the question IDs that are not yet answered
		final Map<String, Integer> unansweredQuestionsCountMap = new HashMap<>();
		for (final Session s : sessions) {
			if (!contentIdMap.containsKey(s.getId())) {
				continue;
			}
			// Note: create a copy of the first set so that we don't modify the contents in the original set
			final Set<String> contentIdsInSession = new HashSet<>(contentIdMap.get(s.getId()));
			Set<String> answeredContentIdsInSession = answeredQuestionsMap.get(s.getId());
			if (answeredContentIdsInSession == null) {
				answeredContentIdsInSession = new HashSet<>();
			}
			contentIdsInSession.removeAll(answeredContentIdsInSession);
			unansweredQuestionsCountMap.put(s.getId(), contentIdsInSession.size());
		}

		final List<SessionInfo> sessionInfos = new ArrayList<>();
		for (final Session session : sessions) {
			int numUnanswered = 0;

			if (unansweredQuestionsCountMap.containsKey(session.getId())) {
				numUnanswered = unansweredQuestionsCountMap.get(session.getId());
			}
			final SessionInfo info = new SessionInfo(session);
			info.setNumUnanswered(numUnanswered);
			sessionInfos.add(info);
		}
		return sessionInfos;
	}

	/* TODO: Move to service layer. */
	private List<SessionInfo> getSessionInfoData(
			final List<Session> sessions,
			final ViewQuery questionCountView,
			final ViewQuery answerCountView,
			final ViewQuery commentCountView,
			final ViewQuery unreadCommentCountView) {
		final Map<String, Integer> questionCountMap = db.queryView(questionCountView).getRows()
				.stream().map(row -> new AbstractMap.SimpleImmutableEntry<>(row.getKey(), row.getValueAsInt()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		final Map<String, Integer> answerCountMap = db.queryView(answerCountView).getRows()
				.stream().map(row -> new AbstractMap.SimpleImmutableEntry<>(row.getKey(), row.getValueAsInt()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		final Map<String, Integer> commentCountMap = db.queryView(commentCountView).getRows()
				.stream().map(row -> new AbstractMap.SimpleImmutableEntry<>(row.getKey(), row.getValueAsInt()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		final Map<String, Integer> unreadCommentCountMap = db.queryView(unreadCommentCountView).getRows()
				.stream().map(row -> new AbstractMap.SimpleImmutableEntry<>(row.getKey(), row.getValueAsInt()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		final List<SessionInfo> sessionInfos = new ArrayList<>();
		for (final Session session : sessions) {
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

			final SessionInfo info = new SessionInfo(session);
			info.setNumQuestions(numQuestions);
			info.setNumAnswers(numAnswers);
			info.setNumInterposed(numComments);
			info.setNumUnredInterposed(numUnreadComments);
			sessionInfos.add(info);
		}
		return sessionInfos;
	}

	/* TODO: Move to service layer. */
	@Override
	public LoggedIn registerAsOnlineUser(final User user, final Session session) {
		LoggedIn loggedIn = new LoggedIn();
		try {
			final List<LoggedIn> loggedInList = db.queryView(createQuery("all").designDocId("_design/LoggedIn").key(user.getUsername()), LoggedIn.class);

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
