package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ContentRepository extends CrudRepository<Content, String> {
	List<Content> findBySessionIdAndVariantAndActive(Object... keys);
	Content findOne(String id);
	Content save(String sessionId, Content content);
	List<Content> findBySessionIdForUsers(String sessionId);
	List<Content> findBySessionIdForSpeaker(String sessionId);
	int countBySessionId(String sessionId);
	List<String> findIdsBySessionId(String sessionId);
	int deleteQuestionWithAnswers(String contentId);
	int[] deleteAllQuestionsWithAnswers(String sessionId);
	List<String> findUnansweredIdsBySessionIdAndUser(String sessionId, User user);
	void update(Content content);
	List<Content> findBySessionIdOnlyLectureVariantAndActive(String sessionId);
	List<Content> findBySessionIdOnlyLectureVariant(String sessionId);
	List<Content> findBySessionIdOnlyFlashcardVariantAndActive(String sessionId);
	List<Content> findBySessionIdOnlyFlashcardVariant(String sessionId);
	List<Content> findBySessionIdOnlyPreparationVariantAndActive(String sessionId);
	List<Content> findBySessionIdOnlyPreparationVariant(String sessionId);
	List<Content> findBySessionId(String sessionId);
	int countLectureVariantBySessionId(String sessionId);
	int countFlashcardVariantBySessionId(String sessionId);
	int countPreparationVariantBySessionId(String sessionId);
	void publishQuestions(String sessionId, boolean publish, List<Content> contents);
	List<Content> publishAllQuestions(String sessionId, boolean publish);
	List<String> findIdsBySessionIdAndVariantAndSubject(String sessionId, String questionVariant, String subject);
	void resetQuestionsRoundState(String sessionId, List<Content> contents);
	void setVotingAdmissions(String sessionId, boolean disableVoting, List<Content> contents);
	List<Content> setVotingAdmissionForAllQuestions(String sessionId, boolean disableVoting);
	int[] deleteAllLectureQuestionsWithAnswers(String sessionId);
	int[] deleteAllFlashcardsWithAnswers(String sessionId);
	int[] deleteAllPreparationQuestionsWithAnswers(String sessionId);
	List<String> findSubjectsBySessionIdAndVariant(String sessionId, String questionVariant);
	List<String> findUnansweredIdsBySessionIdAndUserOnlyLectureVariant(String sessionId, User user);
	List<String> findUnansweredIdsBySessionIdAndUserOnlyPreparationVariant(String sessionId, User user);
}
