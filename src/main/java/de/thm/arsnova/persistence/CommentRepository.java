package de.thm.arsnova.persistence;

import java.util.List;

import de.thm.arsnova.model.Comment;
import de.thm.arsnova.model.migration.v2.CommentReadingCount;

public interface CommentRepository extends CrudRepository<Comment, String> {
	int countByRoomId(String roomId);

	CommentReadingCount countReadingByRoomId(String roomId);

	CommentReadingCount countReadingByRoomIdAndUserId(String roomId, String userId);

	List<Comment> findByRoomId(String roomId, int start, int limit);

	List<Comment> findByRoomIdAndUserId(String roomId, String userId, int start, int limit);

	Iterable<Comment> findStubsByRoomId(String roomId);

	Iterable<Comment> findStubsByRoomIdAndUserId(String roomId, String userId);

	Comment findOne(String commentId);
}
