package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ContentRepository extends CrudRepository<Content, String> {
	List<Content> findByRoomIdAndVariantAndActive(Object... keys);
	List<Content> findByRoomIdForUsers(String roomId);
	List<Content> findByRoomIdForSpeaker(String roomId);
	int countByRoomId(String roomId);
	List<String> findIdsByRoomId(String roomId);
	List<String> findIdsByRoomIdAndVariant(String roomId, String variant);
	int deleteByRoomId(String roomId);
	List<String> findUnansweredIdsByRoomIdAndUser(String roomId, ClientAuthentication user);
	List<Content> findByRoomIdOnlyLectureVariantAndActive(String roomId);
	List<Content> findByRoomIdOnlyLectureVariant(String roomId);
	List<Content> findByRoomIdOnlyFlashcardVariantAndActive(String roomId);
	List<Content> findByRoomIdOnlyFlashcardVariant(String roomId);
	List<Content> findByRoomIdOnlyPreparationVariantAndActive(String roomId);
	List<Content> findByRoomIdOnlyPreparationVariant(String roomId);
	List<Content> findByRoomId(String roomId);
	int countLectureVariantByRoomId(String roomId);
	int countFlashcardVariantRoomId(String roomId);
	int countPreparationVariantByRoomId(String roomId);
	List<String> findIdsByRoomIdAndVariantAndSubject(String roomId, String questionVariant, String subject);
	List<String> findSubjectsByRoomIdAndVariant(String roomId, String questionVariant);
	List<String> findUnansweredIdsByRoomIdAndUserOnlyLectureVariant(String roomId, ClientAuthentication user);
	List<String> findUnansweredIdsByRoomIdAndUserOnlyPreparationVariant(String roomId, ClientAuthentication user);
}
