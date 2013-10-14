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

import java.util.List;

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.FoodVote;
import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

public interface IDatabaseDao {
	Session getSessionFromKeyword(String keyword);

	Session getSession(String keyword);

	List<Session> getMySessions(User user);

	Session saveSession(Session session);

	boolean sessionKeyAvailable(String keyword);

	Question saveQuestion(Session session, Question question);

	InterposedQuestion saveQuestion(Session session, InterposedQuestion question);

	Question getQuestion(String id);

	List<Question> getSkillQuestions(String session);

	int getSkillQuestionCount(Session session);

	LoggedIn registerAsOnlineUser(User u, Session s);

	void updateSessionOwnerActivity(Session session);

	List<String> getQuestionIds(Session session, User user);

	void deleteQuestionWithAnswers(Question question);

	void deleteAllQuestionsWithAnswers(Session session);

	List<String> getUnAnsweredQuestionIds(Session session, User user);

	Answer getMyAnswer(String questionId, int piRound);

	List<Answer> getAnswers(String questionId, int piRound);

	int getAnswerCount(String questionId);

	List<Answer> getFreetextAnswers(String questionId);

	int countActiveUsers(long since);

	int countActiveUsers(Session session, long since);

	List<Answer> getMyAnswers(String sessionKey);

	int getTotalAnswerCount(String sessionKey);

	int getInterposedCount(String sessionKey);

	InterposedReadingCount getInterposedReadingCount(Session session);

	List<InterposedQuestion> getInterposedQuestions(String sessionKey);

	void vote(String menu);

	int getFoodVoteCount();

	List<FoodVote> getFoodVote();

	int countSessions();

	int countOpenSessions();

	int countClosedSessions();

	int countAnswers();

	int countQuestions();

	InterposedQuestion getInterposedQuestion(String questionId);

	void markInterposedQuestionAsRead(InterposedQuestion question);

	List<Session> getMyVisitedSessions(User user);

	Question updateQuestion(Question question);

	void deleteAnswers(Question question);

	Answer saveAnswer(Answer answer, User user);

	Answer updateAnswer(Answer answer);

	Session getSessionFromId(String sessionId);

	void deleteAnswer(String answerId);

	void deleteInterposedQuestion(InterposedQuestion question);

	List<Session> getCourseSessions(List<Course> courses);

	Session lockSession(Session session, Boolean lock);

	List<String> getActiveUsers(int timeDifference);

	void deleteSession(Session session);
}
