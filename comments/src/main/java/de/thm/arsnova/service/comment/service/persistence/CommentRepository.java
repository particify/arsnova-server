package de.thm.arsnova.service.comment.service.persistence;

import de.thm.arsnova.service.comment.model.Comment;

import java.util.Set;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

public interface CommentRepository extends CrudRepository<Comment, String> {
    List<Comment> findByRoomIdAndArchiveIdNull(String roomId);
    List<Comment> findByIdInAndRoomIdAndArchiveIdNull(Set<String> ids, String roomId);
    long countByArchiveId(String archiveId);
    @Transactional
    List<Comment> deleteByRoomId(String roomId);
    long countByRoomIdAndAckAndArchiveIdNull(String roomId, Boolean ack);
    List<Comment> findByArchiveId(String archiveId);
}
