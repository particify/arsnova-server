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

import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.AnswerStatistics;
import de.thm.arsnova.model.AnswerStatisticsUserSummary;
import de.thm.arsnova.model.ChoiceAnswerStatistics;
import de.thm.arsnova.model.TextAnswer;
import de.thm.arsnova.model.TextAnswerStatistics;

public interface AnswerService extends EntityService<Answer> {
	Answer getMyAnswer(String contentId);

	ChoiceAnswerStatistics getChoiceStatistics(String contentId, int piRound);

	ChoiceAnswerStatistics getChoiceStatistics(String contentId);

	AnswerStatistics getAllStatistics(String contentId);

	TextAnswerStatistics getTextStatistics(String contentId, int round);

	TextAnswerStatistics getTextStatistics(String contentId);

	AnswerStatisticsUserSummary getStatisticsByUserIdAndContentIds(String userId, List<String> contentIds);

	List<String> getAnswerIdsByContentIdNotHidden(String contentId);

	List<String> getAnswerIdsByCreatorIdRoomId(String creatorId, String roomId);

	List<String> getAnswerIdsByCreatorIdContentIdsRound(String creatorId, List<String> contentIds, int round);

	Answer getAnswerByContentIdAndUserIdAndCurrentRound(String contentId, String userId);

	void deleteAnswers(String contentId);

	Answer create(Answer answer);

	void hideTextAnswer(TextAnswer answer, boolean hidden);
}
