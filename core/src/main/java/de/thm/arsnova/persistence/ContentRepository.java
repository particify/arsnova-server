package de.thm.arsnova.persistence;

import java.util.List;
import java.util.Set;

import de.thm.arsnova.model.Content;

public interface ContentRepository extends CrudRepository<Content, String> {
	List<Content> findByRoomIdAndVariantAndActive(Object... keys);

	List<Content> findByRoomIdForUsers(String roomId);

	List<Content> findByRoomIdForSpeaker(String roomId);

	int countByRoomId(String roomId);

	List<String> findIdsByRoomId(String roomId);

	Iterable<Content> findStubsByIds(Set<String> ids);

	Iterable<Content> findStubsByRoomId(String roomId);

	List<String> findUnansweredIdsByRoomIdAndUser(String roomId, String userId);

	List<Content> findByRoomIdOnlyFlashcardVariantAndActive(String roomId);

	List<Content> findByRoomId(String roomId);
}
