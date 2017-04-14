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
package de.thm.arsnova.dao;

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.domain.CourseScore;
import de.thm.arsnova.entities.*;
import de.thm.arsnova.entities.transport.ImportExportSession;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StubDatabaseDao implements IDatabaseDao {

	private static Map<String, Session> stubSessions = new ConcurrentHashMap<>();
	private static Map<String, Feedback> stubFeedbacks = new ConcurrentHashMap<>();
	private static Map<String, List<Question>> stubQuestions = new ConcurrentHashMap<>();
	private static Map<String, User> stubUsers = new ConcurrentHashMap<>();

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
		List<Question> questions = new ArrayList<>();
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
	public void log(String event, Map<String, Object> payload, LogEntry.LogLevel level) {
		// TODO Auto-generated method stub
	}

	@Override
	public void log(String event, Map<String, Object> payload) {
		// TODO Auto-generated method stub
	}

	@Override
	public void log(String event, LogEntry.LogLevel level, Object... rawPayload) {
		// TODO Auto-generated method stub
	}

	@Override
	public void log(String event, Object... rawPayload) {
		// TODO Auto-generated method stub
	}

	@Override
	public Session getSessionFromKeyword(String keyword) {
		return stubSessions.get(keyword);
	}

	@Override
	public Question saveQuestion(Session session, Question question) {
		List<Question> questions = stubQuestions.get(session.getKeyword());
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
	public int getSkillQuestionCount(Session session) {
		return stubQuestions.get(session.getKeyword()).size();
	}

	@Override
	public List<Session> getMySessions(User user, final int start, final int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Session> getSessionsForUsername(String username, final int start, final int limit) {
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
		return new ArrayList<>();
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
	public int deleteQuestionWithAnswers(Question question) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int deleteAnswers(Question question) {
		// TODO Auto-generated method stub
		return 0;
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
	public Session changeSessionCreator(Session session, final String newCreator) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] deleteSession(Session session) {
		return new int[] { 0,0 };
	}

	@Override
	public int[] deleteInactiveGuestSessions(long lastActivityBefore) {
		return new int[] { 0,0 };
	}

	@Override
	public int deleteInactiveGuestVisitedSessionLists(long lastActivityBefore) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int[] deleteAllQuestionsWithAnswers(Session session) {
		return new int[] { 0, 0 };
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
	public int[] deleteAllLectureQuestionsWithAnswers(Session session) {
		return new int[] { 0, 0 };
	}

	@Override
	public int[] deleteAllFlashcardsWithAnswers(Session session) {
		return new int[] { 0, 0 };
	}

	@Override
	public int[] deleteAllPreparationQuestionsWithAnswers(Session session) {
		return new int[] { 0, 0 };
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
	public int deleteAllInterposedQuestions(Session session) {
		return 0;
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
	public int deleteAllQuestionsAnswers(Session session) {
		// TODO Auto-generated method stub
		return 0;
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
	public int deleteInactiveUsers(long lastActivityBefore) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<InterposedQuestion> getInterposedQuestions(Session session, User user, final int start, final int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int deleteAllInterposedQuestions(Session session, User user) {
		// TODO Auto-generated method stub
		return 0;
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
	public List<Session> getVisitedSessionsForUsername(String username, final int start, final int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int deleteAllPreparationAnswers(Session session) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int deleteAllLectureAnswers(Session session) {
		// TODO Auto-generated method stub
		return 0;
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
	public ImportExportSession exportSession(String sessionkey, Boolean withAnswer, Boolean withFeedbackQuestions) {
		// TODO Auto.generated method stub
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
	public List<Question> getAllSkillQuestions(Session session) {
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

	@Override
	public List<Motd> getAdminMotds() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Motd> getMotdsForAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Motd> getMotdsForLoggedIn() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Motd> getMotdsForTutors() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Motd> getMotdsForStudents() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Motd> getMotdsForSession(final String sessionkey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Motd> getMotds(NovaView view) {
		return null;
	}

	@Override
	public Motd getMotdByKey(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Motd createOrUpdateMotd(Motd motd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteMotd(Motd motd) {
		// TODO Auto-generated method stub
	}

	@Override
	public MotdList getMotdListForUser(final String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MotdList createOrUpdateMotdList(MotdList motdlist) {
		// TODO Auto-generated method stub
		return null;
	}
}
