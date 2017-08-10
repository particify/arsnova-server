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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AnswerRepository extends CrudRepository<Answer, String> {
	Answer findByQuestionIdUserPiRound(String questionId, User user, int piRound);
	List<Answer> findByContentIdPiRound(String contentId, int piRound);
	List<Answer> findByContentId(String contentId);
	int countByContentIdRound(String contentId, int round);
	int countByContentId(String contentId);
	List<Answer> findByContentId(String contentId, int start, int limit);
	List<Answer> findByUserSessionId(User user, String sessionId);
	int countBySessionKey(String sessionKey);
	int deleteByContentId(String contentId);
	int countBySessionIdLectureVariant(String sessionId);
	int countBySessionIdPreparationVariant(String sessionId);
	int deleteAllAnswersForQuestions(List<String> contentIds);
	int deleteByContentIds(List<String> contentIds);
}
