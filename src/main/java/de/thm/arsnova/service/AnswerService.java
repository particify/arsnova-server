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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.service;

import java.util.List;
import java.util.Map;

import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.AnswerStatistics;
import de.thm.arsnova.model.TextAnswer;

public interface AnswerService extends EntityService<Answer> {
	Answer getMyAnswer(String contentId);

	void getFreetextAnswerAndMarkRead(String answerId, String userId);

	AnswerStatistics getStatistics(String contentId, int piRound);

	AnswerStatistics getStatistics(String contentId);

	AnswerStatistics getAllStatistics(String contentId);

	List<TextAnswer> getTextAnswers(String contentId, int piRound, int offset, int limit);

	List<TextAnswer> getTextAnswers(String contentId, int offset, int limit);

	List<TextAnswer> getAllTextAnswers(String contentId, int offset, int limit);

	int countAnswersByContentIdAndRound(String contentId);

	int countAnswersByContentIdAndRound(String contentId, int piRound);

	List<TextAnswer> getTextAnswersByContentId(String contentId, int offset, int limit);

	List<Answer> getMyAnswersByRoomId(String roomId);

	int countTotalAnswersByRoomId(String roomId);

	int countTotalAnswersByContentId(String contentId);

	void deleteAnswers(String contentId);

	Answer create(Answer answer);

	Answer update(Answer answer);

	Map<String, Object> countAnswersAndAbstentionsInternal(String contentId);

	int countLectureContentAnswers(String roomId);

	int countLectureQuestionAnswersInternal(String roomId);

	int countPreparationContentAnswers(String roomId);

	int countPreparationQuestionAnswersInternal(String roomId);

	int countTotalAbstentionsByContentId(String contentId);
}
