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
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.RoomStatistics;
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.migration.v2.LoggedIn;
import de.thm.arsnova.entities.transport.ImportExportSession;
import de.thm.arsnova.persistance.LogEntryRepository;
import de.thm.arsnova.persistance.MotdRepository;
import de.thm.arsnova.persistance.RoomRepository;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
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

public class CouchDbRoomRepository extends CouchDbCrudRepository<Room> implements RoomRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbRoomRepository.class);

	@Autowired
	private LogEntryRepository dbLogger;

	@Autowired
	private MotdRepository motdRepository;

	public CouchDbRoomRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Room.class, db, "by_keyword", createIfNotExists);
	}

	@Override
	@Cacheable("rooms")
	public Room findByKeyword(final String keyword) {
		final List<Room> roomList = queryView("by_keyword", keyword);

		return !roomList.isEmpty() ? roomList.get(0) : null;
	}

	/* TODO: Move to service layer. */
	private String getSessionKeyword(final String internalSessionId) throws IOException {
		final Room room = get(internalSessionId);
		if (room == null) {
			logger.error("No session found for internal id {}.", internalSessionId);

			return null;
		}

		return room.getShortId();
	}

	@Override
	public List<Room> findSessionsByCourses(final List<Course> courses) {
		return queryView("by_courseid",
				ComplexKey.of(courses.stream().map(Course::getId).collect(Collectors.toList())));
	}

	@Override
	public List<Room> findInactiveGuestSessionsMetadata(final long lastActivityBefore) {
		final ViewResult result = db.queryView(
				createQuery("by_lastactivity_for_guests").endKey(lastActivityBefore));
		final int[] count = new int[3];

		List<Room> sessions = new ArrayList<>();
		for (final ViewResult.Row row : result.getRows()) {
			final Room s = new Room();
			s.setId(row.getId());
			s.setRevision(row.getValueAsNode().get("_rev").asText());
			sessions.add(s);
		}

		return sessions;
	}

	/* TODO: Move to service layer. */
	@Override
	public Room importSession(final UserAuthentication user, final ImportExportSession importSession) {
		/* FIXME: not yet migrated - move to service layer */
		throw new UnsupportedOperationException();
//		final Room session = this.saveSession(user, importSession.generateSessionEntity(user));
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
//				question.setRoomId(session.getId());
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
//					answerDoc.put("sessionId", a.getRoomId());
//					answerDoc.put("questionId", a.getContentId());
//					answerDoc.put("answerSubject", a.getAnswerSubject());
//					answerDoc.put("questionVariant", a.getGroup());
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
//		final Room session = getDatabaseDao().getSessionFromKeyword(sessionkey);
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
	private Room calculateSessionInfo(final ImportExportSession importExportSession, final Room room) {
		/* FIXME: not yet migrated - move to service layer */
		throw new UnsupportedOperationException();
//		int unreadComments = 0;
//		int numUnanswered = 0;
//		int numAnswers = 0;
//		for (Comment i : importExportSession.getFeedbackQuestions()) {
//			if (!i.isRead()) {
//				unreadComments++;
//			}
//		}
//		for (ImportExportSession.ImportExportContent question : importExportSession.getQuestions()) {
//			numAnswers += question.getAnswers().size();
//			if (question.getAnswers().isEmpty()) {
//				numUnanswered++;
//			}
//		}
//		RoomStatistics stats = new RoomStatistics();
//		stats.setContentCount(importExportSession.getQuestions().size());
//		stats.setAnswerCount(numAnswers);
//		stats.setUnreadAnswerCount(numUnanswered);
//		stats.setCommentCount(importExportSession.getFeedbackQuestions().size());
//		stats.setUnreadCommentCount(unreadComments);
//
//		return room;
	}

	@Override
	public List<Room> findByOwner(final UserAuthentication owner, final int start, final int limit) {
		return findByOwnerId(owner.getId(), start, limit);
	}

	@Override
	public List<Room> findByOwnerId(final String ownerId, final int start, final int limit) {
		final int qSkip = start > 0 ? start : -1;
		final int qLimit = limit > 0 ? limit : -1;

		/* TODO: Only load IDs and check against cache for data. */
		return db.queryView(
				createQuery("partial_by_pool_ownerid_name")
						.skip(qSkip)
						.limit(qLimit)
						.startKey(ComplexKey.of(false, ownerId))
						.endKey(ComplexKey.of(false, ownerId, ComplexKey.emptyObject()))
						.includeDocs(true),
				Room.class);
	}

	@Override
	public List<Room> findAllForPublicPool() {
		// TODO replace with new view
		return queryView("partial_by_ppsubject_name_for_publicpool");
	}

	@Override
	public List<Room> findInfosForPublicPool() {
		final List<Room> rooms = this.findAllForPublicPool();
		return attachStatsForRooms(rooms);
	}

	@Override
	public List<Room> findForPublicPoolByOwner(final UserAuthentication owner) {
		/* TODO: Only load IDs and check against cache for data. */
		return db.queryView(
				createQuery("partial_by_pool_ownerid_name")
						.startKey(ComplexKey.of(true, owner.getId()))
						.endKey(ComplexKey.of(true, owner.getId(), ComplexKey.emptyObject()))
						.includeDocs(true),
				Room.class);
	}

	/* TODO: Move to service layer. */
	@Override
	public List<Room> findInfosForPublicPoolByOwner(final UserAuthentication owner) {
		final List<Room> rooms = this.findForPublicPoolByOwner(owner);
		if (rooms.isEmpty()) {
			return new ArrayList<>();
		}
		return attachStatsForRooms(rooms);
	}

	/* TODO: Move to service layer. */
	@Override
	public List<Room> getRoomsWithStatsForOwner(final UserAuthentication owner, final int start, final int limit) {
		final List<Room> sessions = this.findByOwner(owner, start, limit);
		if (sessions.isEmpty()) {
			return new ArrayList<>();
		}
		return attachStatsForRooms(sessions);
	}

	/* TODO: Move to service layer. */
	private List<Room> attachStatsForRooms(final List<Room> rooms) {
		final List<String> sessionIds = rooms.stream().map(Room::getId).collect(Collectors.toList());
		final ViewQuery questionCountView = createQuery("by_sessionid").designDocId("_design/Content")
				.group(true).keys(sessionIds);
		final ViewQuery answerCountView = createQuery("by_sessionid").designDocId("_design/Answer")
				.group(true).keys(sessionIds);
		final ViewQuery commentCountView = createQuery("by_sessionid").designDocId("_design/Comment")
				.group(true).keys(sessionIds);
		final ViewQuery unreadCommentCountView = createQuery("by_sessionid_read").designDocId("_design/Comment")
				.group(true).keys(rooms.stream().map(session -> ComplexKey.of(session.getId(), false)).collect(Collectors.toList()));

		return attachStats(rooms, questionCountView, answerCountView, commentCountView, unreadCommentCountView);
	}

	/* TODO: Move to service layer. */
	public List<Room> getVisitedRoomsWithStatsForOwner(final List<Room> rooms, final UserAuthentication owner) {
		final ViewQuery answeredQuestionsView = createQuery("by_user_sessionid").designDocId("_design/Answer")
				.keys(rooms.stream().map(session -> ComplexKey.of(owner.getUsername(), session.getId())).collect(Collectors.toList()));
		final ViewQuery contentIdsView = createQuery("by_sessionid").designDocId("_design/Content")
				.keys(rooms.stream().map(Room::getId).collect(Collectors.toList()));

		return attachVisitedRoomStats(rooms, answeredQuestionsView, contentIdsView);
	}

	/* TODO: Move to service layer. */
	private List<Room> attachVisitedRoomStats(
			final List<Room> rooms,
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
		for (final Room s : rooms) {
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

		for (final Room room : rooms) {
			int numUnanswered = 0;

			if (unansweredQuestionsCountMap.containsKey(room.getId())) {
				numUnanswered = unansweredQuestionsCountMap.get(room.getId());
			}
			RoomStatistics stats = new RoomStatistics();
			room.setStatistics(stats);
			stats.setUnansweredContentCount(numUnanswered);
		}
		return rooms;
	}

	/* TODO: Move to service layer. */
	private List<Room> attachStats(
			final List<Room> rooms,
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

		for (final Room room : rooms) {
			int numQuestions = 0;
			int numAnswers = 0;
			int numComments = 0;
			int numUnreadComments = 0;
			if (questionCountMap.containsKey(room.getId())) {
				numQuestions = questionCountMap.get(room.getId());
			}
			if (answerCountMap.containsKey(room.getId())) {
				numAnswers = answerCountMap.get(room.getId());
			}
			if (commentCountMap.containsKey(room.getId())) {
				numComments = commentCountMap.get(room.getId());
			}
			if (unreadCommentCountMap.containsKey(room.getId())) {
				numUnreadComments = unreadCommentCountMap.get(room.getId());
			}

			final RoomStatistics stats = new RoomStatistics();
			room.setStatistics(stats);
			stats.setContentCount(numQuestions);
			stats.setAnswerCount(numAnswers);
			stats.setCommentCount(numComments);
			stats.setUnreadCommentCount(numUnreadComments);
		}
		return rooms;
	}

	/* TODO: Move to service layer. */
	@Override
	public LoggedIn registerAsOnlineUser(final UserAuthentication user, final Room room) {
		LoggedIn loggedIn = new LoggedIn();
		try {
			final List<LoggedIn> loggedInList = db.queryView(createQuery("all").designDocId("_design/LoggedIn").key(user.getUsername()), LoggedIn.class);

			if (!loggedInList.isEmpty()) {
				loggedIn = loggedInList.get(0);

				/* Do not clutter CouchDB. Only update once every 3 hours per session. */
				if (loggedIn.getSessionId().equals(room.getId()) && loggedIn.getTimestamp() > System.currentTimeMillis() - 3 * 3600000) {
					return loggedIn;
				}
			}

			loggedIn.setUser(user.getUsername());
			loggedIn.setSessionId(room.getId());
			/* FIXME: migrate */
			//loggedIn.addVisitedSession(room);
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
