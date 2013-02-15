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

package de.thm.arsnova.services;

import java.util.List;

import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.Question;

public interface IQuestionService {
	Question saveQuestion(Question question);

	boolean saveQuestion(InterposedQuestion question);

	Question getQuestion(String id);

	List<Question> getSkillQuestions(String sessionkey);

	int getSkillQuestionCount(String sessionkey);

	List<String> getQuestionIds(String sessionKey);

	void deleteQuestion(String questionId);

	List<String> getUnAnsweredQuestions(String sessionKey);

	Answer getMyAnswer(String questionId);

	List<Answer> getAnswers(String questionId);

	int getAnswerCount(String questionId);

	List<Answer> getFreetextAnswers(String questionId);

	List<Answer> getMytAnswers(String sessionKey);

	int getTotalAnswerCount(String sessionKey);

	int getInterposedCount(String sessionKey);

	InterposedReadingCount getInterposedReadingCount(String sessionKey);

	List<InterposedQuestion> getInterposedQuestions(String sessionKey);

	InterposedQuestion readInterposedQuestion(String questionId);

	void update(Question question);

	void deleteAnswers(String questionId);

	Answer saveAnswer(Answer answer);

	Answer updateAnswer(Answer answer);

	void deleteAnswer(String questionId, String answerId);

	void deleteInterposedQuestion(String questionId);

}
