/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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
package de.thm.arsnova.services;

import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.VisitedSession;

import java.util.List;
import java.util.Map;

/**
 * The functionality the question service should provide.
 */
public interface IQuestionService {
	Question saveQuestion(Question question);

	boolean saveQuestion(InterposedQuestion question);

	Question getQuestion(String id);

	List<Question> getSkillQuestions(String sessionkey);

	int getSkillQuestionCount(String sessionkey);

	void deleteQuestion(String questionId);

	void deleteAllQuestions(String sessionKeyword);

	void startNewPiRound(String questionId, User user);

	void startNewPiRoundDelayed(String questionId, int time);

	void cancelPiRoundChange(String questionId);

	void cancelDelayedPiRoundChange(String questionId);

	void resetPiRoundState(String questionId);

	List<String> getUnAnsweredQuestionIds(String sessionKey);

	Answer getMyAnswer(String questionId);

	void readFreetextAnswer(String answerId, User user);

	List<Answer> getAnswers(String questionId, int piRound, int offset, int limit);

	List<Answer> getAnswers(String questionId, int offset, int limit);

	List<Answer> getAllAnswers(String questionId, int offset, int limit);

	int getAnswerCount(String questionId);

	int getAnswerCount(String questionId, int piRound);

	List<Answer> getFreetextAnswers(String questionId, int offset, int limit);

	List<Answer> getFreetextAnswersInternal(String questionId);

	List<Answer> getMyAnswers(String sessionKey);

	int getTotalAnswerCount(String sessionKey);

	int getTotalAnswerCountByQuestion(String questionId);

	int getInterposedCount(String sessionKey);

	InterposedReadingCount getInterposedReadingCount(String sessionKey, String username);

	List<InterposedQuestion> getInterposedQuestions(String sessionKey, int offset, int limit);

	InterposedQuestion readInterposedQuestion(String questionId);

	InterposedQuestion readInterposedQuestionInternal(String questionId, User user);

	Question update(Question question);

	Question update(Question question, User user);

	void deleteAnswers(String questionId);

	Answer saveAnswer(String questionId, de.thm.arsnova.entities.transport.Answer answer);

	Answer updateAnswer(Answer answer);

	void deleteAnswer(String questionId, String answerId);

	void deleteInterposedQuestion(String questionId);

	List<Question> getLectureQuestions(String sessionkey);

	List<Question> getFlashcards(String sessionkey);

	List<Question> getPreparationQuestions(String sessionkey);

	int getLectureQuestionCount(String sessionkey);

	int getFlashcardCount(String sessionkey);

	int getPreparationQuestionCount(String sessionkey);

	Map<String, Object> getAnswerAndAbstentionCountInternal(String questionid);

	int countLectureQuestionAnswers(String sessionkey);

	int countLectureQuestionAnswersInternal(String sessionkey);

	int countPreparationQuestionAnswers(String sessionkey);

	int countPreparationQuestionAnswersInternal(String sessionkey);

	int countFlashcardsForUserInternal(String sessionkey);

	void deleteLectureQuestions(String sessionkey);

	void deleteFlashcards(String sessionkey);

	void deletePreparationQuestions(String sessionkey);

	List<String> getUnAnsweredLectureQuestionIds(String sessionkey);

	List<String> getUnAnsweredLectureQuestionIds(String sessionKey, User user);

	List<String> getUnAnsweredPreparationQuestionIds(String sessionkey);

	List<String> getUnAnsweredPreparationQuestionIds(String sessionKey, User user);

	void deleteAllInterposedQuestions(String sessionKeyword);

	void publishAll(String sessionkey, boolean publish);

	void publishQuestions(String sessionkey, boolean publish, List<Question> questions);

	void deleteAllQuestionsAnswers(String sessionkey);

	void deleteAllPreparationAnswers(String sessionkey);

	void deleteAllLectureAnswers(String sessionkey);

	int getAbstentionAnswerCount(String questionId);

	String getImage(String questionId, String answerId);

	void setVotingAdmission(String questionId, boolean disableVoting);

	void setVotingAdmissions(String sessionkey, boolean disableVoting, List<Question> questions);

	void setVotingAdmissionForAllQuestions(String sessionkey, boolean disableVoting);

	String getQuestionImage(String questionId);

	String getQuestionFcImage(String questionId);

	List<Question> replaceImageData(List<Question> questions);

	void anonymizeParticipant(VisitedSession session, String oldUsername, String anonymizedUsername);

}
