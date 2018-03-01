/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.AnswerStatistics;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AnswerRepository extends CrudRepository<Answer, String> {
	<T extends Answer> T findByContentIdUserPiRound(String contentId, Class<T> type, ClientAuthentication user, int piRound);
	AnswerStatistics findByContentIdRound(String contentId, int round, final int optionCount);
	int countByContentIdRound(String contentId, int round);
	int countByContentId(String contentId);
	<T extends Answer> List<T> findByContentId(String contentId, Class<T> type, int start, int limit);
	List<Answer> findByUserRoomId(ClientAuthentication user, String roomId);
	int countByRoomId(String roomId);
	int deleteByContentId(String contentId);
	int countByRoomIdOnlyLectureVariant(String roomId);
	int countByRoomIdOnlyPreparationVariant(String roomId);
	int deleteAllAnswersForQuestions(List<String> contentIds);
	int deleteByContentIds(List<String> contentIds);
}
