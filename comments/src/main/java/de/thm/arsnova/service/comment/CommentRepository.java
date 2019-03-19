package de.thm.arsnova.service.comment;

import de.thm.arsnova.service.comment.model.Comment;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CommentRepository extends CrudRepository<Comment, String> {
    List<Comment> findByRoomId(String roomId);
}
