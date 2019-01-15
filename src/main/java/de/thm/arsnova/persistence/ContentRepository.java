package de.thm.arsnova.persistence;

import de.thm.arsnova.model.Content;

import java.util.List;

public interface ContentRepository extends CrudRepository<Content, String> {
	List<Content> findByRoomIdAndVariantAndActive(Object... keys);
	List<Content> findByRoomIdForUsers(String roomId);
	List<Content> findByRoomIdForSpeaker(String roomId);
	int countByRoomId(String roomId);
	List<String> findIdsByRoomId(String roomId);
	Iterable<Content> findStubsByRoomId(final String roomId);
	Iterable<Content> findStubsByRoomIdAndVariant(String roomId, String variant);
	List<String> findUnansweredIdsByRoomIdAndUser(String roomId, String userId);
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
	List<String> findUnansweredIdsByRoomIdAndUserOnlyLectureVariant(String roomId, String userId);
	List<String> findUnansweredIdsByRoomIdAndUserOnlyPreparationVariant(String roomId, String userId);
}
