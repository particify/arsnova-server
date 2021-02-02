package de.thm.arsnova.persistence;

import java.util.List;

import de.thm.arsnova.model.Comment;

public interface CommentRepository extends CrudRepository<Comment, String> {
	int countByRoomId(String roomId);

	List<Comment> findByRoomId(String roomId, int start, int limit);

	List<Comment> findByRoomIdAndUserId(String roomId, String userId, int start, int limit);

	Iterable<Comment> findStubsByRoomId(String roomId);

	Comment findOne(String commentId);
}
