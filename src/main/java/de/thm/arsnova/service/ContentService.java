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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.	 If not, see <http://www.gnu.org/licenses/>.
 */
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.	 If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.service;

import de.thm.arsnova.model.Content;

import java.io.IOException;
import java.util.List;

/**
 * The functionality the question service should provide.
 */
public interface ContentService extends EntityService<Content> {
	Content get(String id);

	List<Content> getByRoomId(String roomId);

	Iterable<Content> getByRoomIdAndGroup(String roomId, String group);

	int countByRoomId(String roomId);

	int countByRoomIdAndGroup(String roomId, String group);

	void delete(String questionId);

	List<String> getUnAnsweredContentIds(String roomId);

	int countFlashcardsForUserInternal(String roomId);

	void deleteAllContents(String roomId);

	void deleteLectureContents(String roomId);

	void deletePreparationContents(String roomId);

	void deleteFlashcards(String roomId);

	List<String> getUnAnsweredLectureContentIds(String roomId);

	List<String> getUnAnsweredLectureContentIds(String roomId, String userId);

	List<String> getUnAnsweredPreparationContentIds(String roomId);

	List<String> getUnAnsweredPreparationContentIds(String roomId, String userId);

	void publishAll(String roomId, boolean publish) throws IOException;

	void publishContents(String roomId, boolean publish, Iterable<Content> contents) throws IOException;

	void deleteAllContentsAnswers(String roomId);

	void deleteAllPreparationAnswers(String roomId);

	void deleteAllLectureAnswers(String roomId);

	void setVotingAdmission(String contentId, boolean disableVoting);

	void setVotingAdmissions(String roomId, boolean disableVoting, Iterable<Content> contents);
}
