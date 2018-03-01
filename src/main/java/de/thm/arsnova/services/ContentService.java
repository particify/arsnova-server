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
package de.thm.arsnova.services;

import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.AnswerStatistics;
import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.TextAnswer;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;

import java.util.List;
import java.util.Map;

/**
 * The functionality the question service should provide.
 */
public interface ContentService extends EntityService<Content> {
	Content save(Content content);

	Content get(String id);

	List<Content> getByRoomId(String roomId);

	int countByRoomId(String roomId);

	void delete(String questionId);

	List<String> getUnAnsweredContentIds(String roomId);

	Answer getMyAnswer(String contentId);

	void getFreetextAnswerAndMarkRead(String answerId, ClientAuthentication user);

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

	Content save(final String roomId, final Content content);

	Content update(Content content);

	void deleteAnswers(String contentId);

	Answer saveAnswer(String contentId, Answer answer);

	Answer updateAnswer(Answer answer);

	void deleteAnswer(String contentId, String answerId);

	List<Content> getLectureContents(String roomId);

	List<Content> getFlashcards(String roomId);

	List<Content> getPreparationContents(String roomId);

	int countLectureContents(String roomId);

	int countFlashcards(String roomId);

	int countPreparationContents(String roomId);

	Map<String, Object> countAnswersAndAbstentionsInternal(String contentId);

	int countLectureContentAnswers(String roomId);

	int countLectureQuestionAnswersInternal(String roomId);

	int countPreparationContentAnswers(String roomId);

	int countPreparationQuestionAnswersInternal(String roomId);

	int countFlashcardsForUserInternal(String roomId);

	void deleteAllContents(String roomId);

	void deleteLectureContents(String roomId);

	void deletePreparationContents(String roomId);

	void deleteFlashcards(String roomId);

	List<String> getUnAnsweredLectureContentIds(String roomId);

	List<String> getUnAnsweredLectureContentIds(String roomId, ClientAuthentication user);

	List<String> getUnAnsweredPreparationContentIds(String roomId);

	List<String> getUnAnsweredPreparationContentIds(String roomId, ClientAuthentication user);

	void publishAll(String roomId, boolean publish);

	void publishContents(String roomId, boolean publish, List<Content> contents);

	void deleteAllContentsAnswers(String roomId);

	void deleteAllPreparationAnswers(String roomId);

	void deleteAllLectureAnswers(String roomId);

	int countTotalAbstentionsByContentId(String contentId);

	void setVotingAdmission(String contentId, boolean disableVoting);

	void setVotingAdmissions(String roomId, boolean disableVoting, List<Content> contents);
}
