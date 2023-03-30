package net.particify.arsnova.comments.service.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

import net.particify.arsnova.comments.model.Archive;

public interface ArchiveRepository extends CrudRepository<Archive, UUID> {
  List<Archive> findByName(String name);
  List<Archive> findByRoomId(UUID roomId);
}
