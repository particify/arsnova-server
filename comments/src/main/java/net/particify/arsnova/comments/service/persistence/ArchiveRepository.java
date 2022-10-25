package net.particify.arsnova.comments.service.persistence;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

import net.particify.arsnova.comments.model.Archive;

public interface ArchiveRepository extends CrudRepository<Archive, String> {
  List<Archive> findByName(String name);
  List<Archive> findByRoomId(String roomId);
}
