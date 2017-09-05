package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.migration.v2.Content;
import de.thm.arsnova.entities.UserAuthentication;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ContentRepository extends CrudRepository<Content, String> {
	List<Content> findBySessionIdAndVariantAndActive(Object... keys);
	List<Content> findBySessionIdForUsers(String sessionId);
	List<Content> findBySessionIdForSpeaker(String sessionId);
	int countBySessionId(String sessionId);
	List<String> findIdsBySessionId(String sessionId);
	List<String> findIdsBySessionIdAndVariant(String sessionId, String variant);
	int deleteBySessionId(String sessionId);
	List<String> findUnansweredIdsBySessionIdAndUser(String sessionId, UserAuthentication user);
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
	List<String> findIdsBySessionIdAndVariantAndSubject(String sessionId, String questionVariant, String subject);
	List<String> findSubjectsBySessionIdAndVariant(String sessionId, String questionVariant);
	List<String> findUnansweredIdsBySessionIdAndUserOnlyLectureVariant(String sessionId, UserAuthentication user);
	List<String> findUnansweredIdsBySessionIdAndUserOnlyPreparationVariant(String sessionId, UserAuthentication user);
}
