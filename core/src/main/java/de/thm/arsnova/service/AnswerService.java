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

public interface AnswerService extends EntityService<Answer> {
	Answer getMyAnswer(String contentId);

	AnswerStatistics getStatistics(String contentId, int piRound);

	AnswerStatistics getStatistics(String contentId);

	AnswerStatistics getAllStatistics(String contentId);

	List<String> getAnswerIdsByContentId(String contentId);

	List<String> getAnswerIdsByCreatorIdRoomId(String creatorId, String roomId);

	List<String> getAnswerIdsByCreatorIdContentIdsRound(String creatorId, List<String> contentIds, int round);

	Answer getAnswerByContentIdAndUserIdAndCurrentRound(String contentId, String userId);

	void deleteAnswers(String contentId);

	Answer create(Answer answer);
}
