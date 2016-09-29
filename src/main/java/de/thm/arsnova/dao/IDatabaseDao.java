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

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.domain.CourseScore;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.DbUser;
import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Motd;
import de.thm.arsnova.entities.MotdList;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.SessionInfo;
import de.thm.arsnova.entities.SortOrder;
import de.thm.arsnova.entities.Statistics;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.ImportExportSession;

import java.util.List;

/**
 * All methods the database must support.
 */
public interface IDatabaseDao {
	Session getSessionFromKeyword(String keyword);

	List<Session> getMySessions(User user, final int start, final int limit);

	List<Session> getSessionsForUsername(String username, final int start, final int limit);

	List<Session> getPublicPoolSessions();

	List<Session> getMyPublicPoolSessions(User user);

	Session saveSession(User user, Session session);

	boolean sessionKeyAvailable(String keyword);

	Question saveQuestion(Session session, Question question);

	InterposedQuestion saveQuestion(Session session, InterposedQuestion question, User user);

	Question getQuestion(String id);

	/**
	 * @deprecated Use getSkillQuestionsForUsers or getSkillQuestionsForTeachers
	 * @param user
	 * @param session
	 * @return
	 */
	@Deprecated
	List<Question> getSkillQuestions(User user, Session session);

	List<Question> getSkillQuestionsForUsers(Session session);

	List<Question> getSkillQuestionsForTeachers(Session session);

	int getSkillQuestionCount(Session session);

	LoggedIn registerAsOnlineUser(User u, Session s);

	Session updateSessionOwnerActivity(Session session);

	List<String> getQuestionIds(Session session, User user);

	void deleteQuestionWithAnswers(Question question);

	void deleteAllQuestionsWithAnswers(Session session);

	List<String> getUnAnsweredQuestionIds(Session session, User user);

	Answer getMyAnswer(User me, String questionId, int piRound);

	List<Answer> getAnswers(Question question, int piRound);

	List<Answer> getAnswers(Question question);

	List<Answer> getAllAnswers(Question question);

	int getAnswerCount(Question question, int piRound);

	int getTotalAnswerCountByQuestion(Question question);

	int getAbstentionAnswerCount(String questionId);

	List<Answer> getFreetextAnswers(String questionId, final int start, final int limit);

	List<Answer> getMyAnswers(User me, Session session);

	int getTotalAnswerCount(String sessionKey);

	int getInterposedCount(String sessionKey);

	InterposedReadingCount getInterposedReadingCount(Session session);

	InterposedReadingCount getInterposedReadingCount(Session session, User user);

	List<InterposedQuestion> getInterposedQuestions(Session session, final int start, final int limit);

	List<InterposedQuestion> getInterposedQuestions(Session session, User user, final int start, final int limit);

	InterposedQuestion getInterposedQuestion(String questionId);

	void markInterposedQuestionAsRead(InterposedQuestion question);

	List<Session> getMyVisitedSessions(User user, final int start, final int limit);

	List<Session> getVisitedSessionsForUsername(String username, final int start, final int limit);

	Question updateQuestion(Question question);

	void deleteAnswers(Question question);

	Answer saveAnswer(Answer answer, User user, Question question, Session session);

	Answer updateAnswer(Answer answer);

	Session getSessionFromId(String sessionId);

	void deleteAnswer(String answerId);

	void deleteInterposedQuestion(InterposedQuestion question);

	List<Session> getCourseSessions(List<Course> courses);

	Session updateSession(Session session);

	Session changeSessionCreator(Session session, String newCreator);

	void deleteSession(Session session);

	List<Question> getLectureQuestionsForUsers(Session session);

	List<Question> getLectureQuestionsForTeachers(Session session);

	List<Question> getFlashcardsForUsers(Session session);

	List<Question> getFlashcardsForTeachers(Session session);

	List<Question> getPreparationQuestionsForUsers(Session session);

	List<Question> getPreparationQuestionsForTeachers(Session session);

	List<Question> getAllSkillQuestions(Session session);

	int getLectureQuestionCount(Session session);

	int getFlashcardCount(Session session);

	int getPreparationQuestionCount(Session session);

	int countLectureQuestionAnswers(Session session);

	int countPreparationQuestionAnswers(Session session);

	void deleteAllLectureQuestionsWithAnswers(Session session);

	void deleteAllFlashcardsWithAnswers(Session session);

	void deleteAllPreparationQuestionsWithAnswers(Session session);

	List<String> getUnAnsweredLectureQuestionIds(Session session, User user);

	List<String> getUnAnsweredPreparationQuestionIds(Session session, User user);

	void deleteAllInterposedQuestions(Session session);

	void deleteAllInterposedQuestions(Session session, User user);

	void publishQuestions(Session session, boolean publish, List<Question> questions);

	List<Question> publishAllQuestions(Session session, boolean publish);

	void deleteAllQuestionsAnswers(Session session);

	DbUser createOrUpdateUser(DbUser user);

	DbUser getUser(String username);

	boolean deleteUser(DbUser dbUser);

	CourseScore getLearningProgress(Session session);

	List<SessionInfo> getMySessionsInfo(User user, final int start, final int limit);

	List<SessionInfo> getPublicPoolSessionsInfo();

	List<SessionInfo> getMyPublicPoolSessionsInfo(final User user);

	List<SessionInfo> getMyVisitedSessionsInfo(User currentUser, final int start, final int limit);

	void deleteAllPreparationAnswers(Session session);

	void deleteAllLectureAnswers(Session session);

	SessionInfo importSession(User user, ImportExportSession importSession);

	ImportExportSession exportSession(String sessionkey, Boolean withAnswer, Boolean withFeedbackQuestions);

	Statistics getStatistics();

	List<String> getSubjects(Session session, String questionVariant);

	List<String> getQuestionIdsBySubject(Session session, String questionVariant, String subject);

	SortOrder createOrUpdateSortOrder(SortOrder sortOrder);

	SortOrder getSortOrder(String sessionId, String questionVariant, String subject);

	void deleteSortOrder(SortOrder sortOrder);

	List<Question> getQuestionsByIds(List<String> ids, Session session);

	void resetQuestionsRoundState(Session session, List<Question> questions);

	void setVotingAdmissions(Session session, boolean disableVoting, List<Question> questions);

	List<Question> setVotingAdmissionForAllQuestions(Session session, boolean disableVoting);

	<T> T getObjectFromId(String documentId, Class<T> klass);

	List<Motd> getAdminMotds();

	List<Motd> getMotdsForAll();

	List<Motd> getMotdsForLoggedIn();

	List<Motd> getMotdsForTutors();

	List<Motd> getMotdsForStudents();

	List<Motd> getMotdsForSession(final String sessionkey);

	List<Motd> getMotds(NovaView view);

	Motd getMotdByKey(String key);

	Motd createOrUpdateMotd(Motd motd);

	void deleteMotd(Motd motd);

	MotdList getMotdListForUser(final String username);

	MotdList createOrUpdateMotdList(MotdList motdlist);
}
