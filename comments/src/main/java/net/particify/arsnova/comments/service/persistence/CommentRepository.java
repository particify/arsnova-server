package net.particify.arsnova.comments.service.persistence;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

import net.particify.arsnova.comments.model.Comment;

public interface CommentRepository extends CrudRepository<Comment, UUID> {
  List<Comment> findByRoomIdAndArchiveIdNull(UUID roomId);
  List<Comment> findByIdInAndRoomIdAndArchiveIdNull(Set<String> ids, UUID roomId);
  long countByArchiveId(UUID archiveId);
  @Transactional
  List<Comment> deleteByRoomId(UUID roomId);
  long countByRoomIdAndAckAndArchiveIdNull(UUID roomId, boolean ack);
  List<Comment> findByArchiveId(UUID archiveId);
}
