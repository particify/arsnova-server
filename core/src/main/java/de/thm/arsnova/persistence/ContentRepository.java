package de.thm.arsnova.persistence;

import java.util.List;

import de.thm.arsnova.model.Content;

public interface ContentRepository extends CrudRepository<Content, String> {
	List<Content> findByRoomIdAndVariantAndActive(Object... keys);

	List<Content> findByRoomIdForUsers(String roomId);

	List<Content> findByRoomIdForSpeaker(String roomId);

	int countByRoomId(String roomId);

	Iterable<Content> findStubsByIds(List<String> ids);

	Iterable<Content> findStubsByRoomId(String roomId);

	List<Content> findByRoomId(String roomId);
}
