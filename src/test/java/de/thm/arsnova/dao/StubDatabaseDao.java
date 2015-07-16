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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.	 If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.domain.CourseScore;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.DbUser;
import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.SessionInfo;
import de.thm.arsnova.entities.Statistics;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.SortOrder;
import de.thm.arsnova.entities.transport.ImportExportSession;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;

public class StubDatabaseDao implements IDatabaseDao {

	private static Map<String, Session> stubSessions = new ConcurrentHashMap<String, Session>();
	private static Map<String, Feedback> stubFeedbacks = new ConcurrentHashMap<String, Feedback>();
	private static Map<String, List<Question>> stubQuestions = new ConcurrentHashMap<String, List<Question>>();
	private static Map<String, User> stubUsers = new ConcurrentHashMap<String, User>();

	public InterposedQuestion interposedQuestion;

	public StubDatabaseDao() {
		fillWithDummySessions();
		fillWithDummyFeedbacks();
		fillWithDummyQuestions();
	}

	public void cleanupTestData() {
		stubSessions.clear();
		stubFeedbacks.clear();
		stubQuestions.clear();
		stubUsers.clear();

		fillWithDummySessions();
		fillWithDummyFeedbacks();
		fillWithDummyQuestions();
	}

	private void fillWithDummySessions() {
		Session session = new Session();
		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("12345678");
		session.setName("TestSession1");
		session.setShortName("TS1");

		stubSessions.put("12345678", session);

		session = new Session();
		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("87654321");
		session.setName("TestSession2");
		session.setShortName("TS2");

		stubSessions.put("87654321", session);

		session = new Session();
		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("18273645");
		session.setName("TestSession2");
		session.setShortName("TS3");

		stubSessions.put("18273645", session);
	}

	private void fillWithDummyFeedbacks() {
		stubFeedbacks.put("12345678", new Feedback(0, 0, 0, 0));
		stubFeedbacks.put("87654321", new Feedback(2, 3, 5, 7));
		stubFeedbacks.put("18273645", new Feedback(2, 3, 5, 11));
	}

	private void fillWithDummyQuestions() {
		List<Question> questions = new ArrayList<Question>();
		questions.add(new Question());
		stubQuestions.put("12345678", questions);
	}

	@Override
	public Session saveSession(User user, Session session) {
		stubSessions.put(session.getKeyword(), session);
		return session;
	}

	@Override
	public boolean sessionKeyAvailable(String keyword) {
		return (stubSessions.get(keyword) == null);
	}

	@Override
	public Session getSessionFromKeyword(String keyword) {
		return stubSessions.get(keyword);
	}

	@Override
	public Question saveQuestion(Session session, Question question) {
		List<Question> questions = stubQuestions.get(session.get_id());
		questions.add(question);
		stubQuestions.put(session.get_id(), questions);

		return question;
	}

	@Override
	public Question getQuestion(String id) {
		// Simply ... no such question ;-)
		return null;
	}

	@Override
	public List<Question> getSkillQuestions(User user, Session session) {
		if (session == null)
			throw new NotFoundException();
		List<Question> questions = stubQuestions.get(session);
		if (questions == null)
			throw new NoContentException();
		return questions;
	}

	@Override
	public int getSkillQuestionCount(Session session) {
		return stubQuestions.get(session.getKeyword()).size();
	}

	@Override
	public List<Session> getMySessions(User user, final int start, final int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Session> getPublicPoolSessions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SessionInfo> getPublicPoolSessionsInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Session> getMyPublicPoolSessions(User user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SessionInfo> getMyPublicPoolSessionsInfo(final User user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LoggedIn registerAsOnlineUser(User u, Session s) {
		stubUsers.put(s.getKeyword(), u);
		return new LoggedIn();
	}

	@Override
	public Session updateSessionOwnerActivity(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Answer getMyAnswer(User user, String questionId, int piRound) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getAnswerCount(Question question, int piRound) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Answer> getFreetextAnswers(String questionId, final int start, final int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Answer> getMyAnswers(User user, Session session) {
		return new ArrayList<Answer>();
	}

	@Override
	public int getTotalAnswerCount(String sessionKey) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getInterposedCount(String sessionKey) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<InterposedQuestion> getInterposedQuestions(Session session, final int start, final int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InterposedQuestion saveQuestion(Session session, InterposedQuestion question, User user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InterposedQuestion getInterposedQuestion(String questionId) {
		return this.interposedQuestion;
	}

	@Override
	public void markInterposedQuestionAsRead(InterposedQuestion question) {
		this.interposedQuestion.setRead(true);
	}

	@Override
	public List<Session> getMyVisitedSessions(User user, final int start, final int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InterposedReadingCount getInterposedReadingCount(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getQuestionIds(Session session, User user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getUnAnsweredQuestionIds(Session session, User user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Question updateQuestion(Question question) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteQuestionWithAnswers(Question question) {
		// TODO Auto-generated method stub
	}

	@Override
	public void deleteAnswers(Question question) {
		// TODO Auto-generated method stub
	}

	@Override
	public Answer updateAnswer(Answer answer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Session getSessionFromId(String sessionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteAnswer(String answerId) {
		// TODO Auto-generated method stub
	}

	@Override
	public void deleteInterposedQuestion(InterposedQuestion question) {
		// TODO Auto-generated method stub
	}

	@Override
	public List<Session> getCourseSessions(List<Course> courses) {
		return null;
	}

	@Override
	public Session updateSession(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteSession(Session session) {
		// TODO Auto-generated method stub
	}

	@Override
	public void deleteAllQuestionsWithAnswers(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getLectureQuestionCount(Session session) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getFlashcardCount(Session session) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPreparationQuestionCount(Session session) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int countLectureQuestionAnswers(Session session) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int countPreparationQuestionAnswers(Session session) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void deleteAllLectureQuestionsWithAnswers(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAllFlashcardsWithAnswers(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAllPreparationQuestionsWithAnswers(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<String> getUnAnsweredLectureQuestionIds(Session session, User user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getUnAnsweredPreparationQuestionIds(Session session, User user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteAllInterposedQuestions(Session session) {
		// TODO Auto-generated method stub
	}

	@Override
	public void publishQuestions(Session session, boolean publish, List<Question> questions) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Question> publishAllQuestions(Session session, boolean publish) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteAllQuestionsAnswers(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public DbUser createOrUpdateUser(DbUser user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DbUser getUser(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CourseScore getLearningProgress(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteUser(DbUser dbUser) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<InterposedQuestion> getInterposedQuestions(Session session, User user, final int start, final int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteAllInterposedQuestions(Session session, User user) {
		// TODO Auto-generated method stub

	}

	@Override
	public InterposedReadingCount getInterposedReadingCount(Session session, User user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SessionInfo> getMySessionsInfo(User user, final int start, final int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SessionInfo> getMyVisitedSessionsInfo(User currentUser, final int start, final int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteAllPreparationAnswers(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAllLectureAnswers(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getAbstentionAnswerCount(String questionId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public SessionInfo importSession(User user, ImportExportSession importSession) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Question> getSkillQuestionsForUsers(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Question> getSkillQuestionsForTeachers(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Question> getLectureQuestionsForUsers(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Question> getLectureQuestionsForTeachers(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Question> getFlashcardsForUsers(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Question> getFlashcardsForTeachers(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Question> getPreparationQuestionsForUsers(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Question> getPreparationQuestionsForTeachers(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Answer> getAnswers(Question question, int piRound) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Answer> getAnswers(Question question) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Answer saveAnswer(Answer answer, User user, Question question, Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statistics getStatistics() {
		final Statistics stats = new Statistics();
		stats.setOpenSessions(3);
		stats.setClosedSessions(0);
		stats.setLectureQuestions(0);
		stats.setAnswers(0);
		stats.setInterposedQuestions(0);
		return stats;
	}

	@Override
	public List<String> getSubjects(Session session, String questionVariant) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getQuestionIdsBySubject(Session session, String questionVariant, String subject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortOrder createOrUpdateSortOrder(SortOrder sortOrder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortOrder getSortOrder(String sessionkey, String questionVariant, String subject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteSortOrder(SortOrder sortOrder) {
		// TODO Auto-generated method stub
	}

	@Override
	public List<Question> getQuestionsByIds(List<String> ids, Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Answer> getAllAnswers(Question question) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTotalAnswerCountByQuestion(Question question) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void resetQuestionsRoundState(Session session,
			List<Question> questions) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setVotingAdmissions(Session session, boolean disableVoting, List<Question> questions) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Question> setVotingAdmissionForAllQuestions(Session session, boolean disableVoting) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getObjectFromId(String documentId, Class<T> klass) {
		// TODO Auto-generated method stub
		return null;
	}
}
