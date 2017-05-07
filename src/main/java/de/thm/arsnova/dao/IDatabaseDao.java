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

import de.thm.arsnova.domain.CourseScore;
import de.thm.arsnova.entities.*;

import java.util.List;

/**
 * All methods the database must support.
 */
public interface IDatabaseDao {
	Question saveQuestion(Session session, Question question);

	InterposedQuestion saveQuestion(Session session, InterposedQuestion question, User user);

	Question getQuestion(String id);

	List<Question> getSkillQuestionsForUsers(Session session);

	List<Question> getSkillQuestionsForTeachers(Session session);

	int getSkillQuestionCount(Session session);

	List<String> getQuestionIds(Session session, User user);

	int deleteQuestionWithAnswers(Question question);

	int[] deleteAllQuestionsWithAnswers(Session session);

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

	Question updateQuestion(Question question);

	int deleteAnswers(Question question);

	Answer saveAnswer(Answer answer, User user, Question question, Session session);

	Answer updateAnswer(Answer answer);

	void deleteAnswer(String answerId);

	void deleteInterposedQuestion(InterposedQuestion question);

	int deleteInactiveGuestVisitedSessionLists(long lastActivityBefore);

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

	int[] deleteAllLectureQuestionsWithAnswers(Session session);

	int[] deleteAllFlashcardsWithAnswers(Session session);

	int[] deleteAllPreparationQuestionsWithAnswers(Session session);

	List<String> getUnAnsweredLectureQuestionIds(Session session, User user);

	List<String> getUnAnsweredPreparationQuestionIds(Session session, User user);

	int deleteAllInterposedQuestions(Session session);

	int deleteAllInterposedQuestions(Session session, User user);

	void publishQuestions(Session session, boolean publish, List<Question> questions);

	List<Question> publishAllQuestions(Session session, boolean publish);

	int deleteAllQuestionsAnswers(Session session);

	CourseScore getLearningProgress(Session session);

	int deleteAllPreparationAnswers(Session session);

	int deleteAllLectureAnswers(Session session);

	Statistics getStatistics();

	List<String> getSubjects(Session session, String questionVariant);

	List<String> getQuestionIdsBySubject(Session session, String questionVariant, String subject);

	List<Question> getQuestionsByIds(List<String> ids, Session session);

	void resetQuestionsRoundState(Session session, List<Question> questions);

	void setVotingAdmissions(Session session, boolean disableVoting, List<Question> questions);

	List<Question> setVotingAdmissionForAllQuestions(Session session, boolean disableVoting);

	<T> T getObjectFromId(String documentId, Class<T> klass);

	MotdList getMotdListForUser(final String username);

	MotdList createOrUpdateMotdList(MotdList motdlist);
}
