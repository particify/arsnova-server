package net.particify.arsnova.comments.service.persistence;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import org.springframework.data.repository.CrudRepository;

import net.particify.arsnova.comments.model.Comment;

public interface CommentRepository extends CrudRepository<Comment, String> {
  List<Comment> findByRoomIdAndArchiveIdNull(String roomId);
  List<Comment> findByIdInAndRoomIdAndArchiveIdNull(Set<String> ids, String roomId);
  long countByArchiveId(String archiveId);
  @Transactional
  List<Comment> deleteByRoomId(String roomId);
  long countByRoomIdAndAckAndArchiveIdNull(String roomId, Boolean ack);
  List<Comment> findByArchiveId(String archiveId);
}
