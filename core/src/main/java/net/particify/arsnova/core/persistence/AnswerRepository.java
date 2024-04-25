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

package net.particify.arsnova.core.persistence;

import java.util.List;
import java.util.Map;

import net.particify.arsnova.core.model.Answer;
import net.particify.arsnova.core.model.ChoiceAnswerStatistics;
import net.particify.arsnova.core.model.PrioritizationAnswerStatistics;

public interface AnswerRepository extends CrudRepository<Answer, String> {
  <T extends Answer> T findByContentIdUserIdPiRound(String contentId, Class<T> type, String userId, int piRound);

  ChoiceAnswerStatistics findStatisticsByContentIdRound(String contentId, int round, int optionCount);

  Iterable<Answer> findStubsByContentIdAndHidden(String contentId, boolean excludeHidden);

  List<String> findIdsByContentIdAndHidden(String contentId, boolean excludeHidden);

  List<String> findIdsByCreatorIdRoomId(String creatorId, String roomId);

  List<String> findIdsByCreatorIdContentIdsRound(String creatorId, List<String> contentIds, int round);

  List<String> findIdsByAnswerStubs(List<Answer> answerStubs);

  PrioritizationAnswerStatistics findByContentIdRoundForPrioritization(String contentId, int optionCount);

  <T extends Answer> List<T> findByContentIdRound(Class<T> clazz, String contentId, int round);

  Map<String, Integer> findUserScoreByContentIdRound(String contentId, int round);
}
