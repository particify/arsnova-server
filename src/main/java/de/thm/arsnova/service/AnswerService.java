package de.thm.arsnova.service;

import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.AnswerStatistics;
import de.thm.arsnova.model.TextAnswer;

import java.util.List;
import java.util.Map;

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

	Answer saveAnswer(String contentId, Answer answer);

	Answer updateAnswer(Answer answer);

	void deleteAnswer(String contentId, String answerId);

	Map<String, Object> countAnswersAndAbstentionsInternal(String contentId);

	int countLectureContentAnswers(String roomId);

	int countLectureQuestionAnswersInternal(String roomId);

	int countPreparationContentAnswers(String roomId);

	int countPreparationQuestionAnswersInternal(String roomId);

	int countTotalAbstentionsByContentId(String contentId);
}
