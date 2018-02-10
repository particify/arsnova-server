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
import de.thm.arsnova.entities.UserAuthentication;

import java.util.List;
import java.util.Map;

/**
 * The functionality the question service should provide.
 */
public interface ContentService extends EntityService<Content> {
	Content save(Content content);

	Content get(String id);

	List<Content> getByRoomShortId(String roomShortId);

	int countByRoomShortId(String roomShortId);

	void delete(String questionId);

	List<String> getUnAnsweredContentIds(String roomShortId);

	Answer getMyAnswer(String contentId);

	void getFreetextAnswerAndMarkRead(String answerId, UserAuthentication user);

	AnswerStatistics getStatistics(String contentId, int piRound);

	AnswerStatistics getStatistics(String contentId);

	AnswerStatistics getAllStatistics(String contentId);

	List<TextAnswer> getTextAnswers(String contentId, int piRound, int offset, int limit);

	List<TextAnswer> getTextAnswers(String contentId, int offset, int limit);

	List<TextAnswer> getAllTextAnswers(String contentId, int offset, int limit);

	int countAnswersByContentIdAndRound(String contentId);

	int countAnswersByContentIdAndRound(String contentId, int piRound);

	List<TextAnswer> getTextAnswersByContentId(String contentId, int offset, int limit);

	List<Answer> getMyAnswersByRoomShortId(String roomShortId);

	int countTotalAnswersByRoomShortId(String roomShortId);

	int countTotalAnswersByContentId(String contentId);

	Content save(final String roomId, final Content content);

	Content update(Content content);

	void deleteAnswers(String contentId);

	Answer saveAnswer(String contentId, Answer answer);

	Answer updateAnswer(Answer answer);

	void deleteAnswer(String contentId, String answerId);

	List<Content> getLectureContents(String roomShortId);

	List<Content> getFlashcards(String roomShortId);

	List<Content> getPreparationContents(String roomShortId);

	int countLectureContents(String roomShortId);

	int countFlashcards(String roomShortId);

	int countPreparationContents(String roomShortId);

	Map<String, Object> countAnswersAndAbstentionsInternal(String contentId);

	int countLectureContentAnswers(String roomShortId);

	int countLectureQuestionAnswersInternal(String roomShortId);

	int countPreparationContentAnswers(String roomShortId);

	int countPreparationQuestionAnswersInternal(String roomShortId);

	int countFlashcardsForUserInternal(String roomShortId);

	void deleteAllContents(String roomShortId);

	void deleteLectureContents(String roomShortId);

	void deletePreparationContents(String roomShortId);

	void deleteFlashcards(String roomShortId);

	List<String> getUnAnsweredLectureContentIds(String roomShortId);

	List<String> getUnAnsweredLectureContentIds(String roomShortId, UserAuthentication user);

	List<String> getUnAnsweredPreparationContentIds(String roomShortId);

	List<String> getUnAnsweredPreparationContentIds(String roomShortId, UserAuthentication user);

	void publishAll(String roomShortId, boolean publish);

	void publishContents(String roomShortId, boolean publish, List<Content> contents);

	void deleteAllContentsAnswers(String roomShortId);

	void deleteAllPreparationAnswers(String roomShortId);

	void deleteAllLectureAnswers(String roomShortId);

	int countTotalAbstentionsByContentId(String contentId);

	void setVotingAdmission(String contentId, boolean disableVoting);

	void setVotingAdmissions(String roomShortId, boolean disableVoting, List<Content> contents);

	void setVotingAdmissionForAllContents(String roomShortId, boolean disableVoting);
}
