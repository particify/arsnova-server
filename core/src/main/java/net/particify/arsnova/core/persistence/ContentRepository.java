package net.particify.arsnova.core.persistence;

import java.util.List;

import net.particify.arsnova.core.model.Content;

public interface ContentRepository extends CrudRepository<Content, String> {
  int countByRoomId(String roomId);

  List<String> findIdsByRoomId(String roomId);

  Iterable<Content> findStubsByIds(List<String> ids);

  Iterable<Content> findStubsByRoomId(String roomId);

  List<Content> findByRoomId(String roomId);
}
