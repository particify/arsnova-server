package de.thm.arsnova.service.comment.service.persistence;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

import de.thm.arsnova.service.comment.model.Archive;

public interface ArchiveRepository extends CrudRepository<Archive, String> {
  List<Archive> findByName(String name);
  List<Archive> findByRoomId(String roomId);
}
