package de.thm.arsnova.persistence;

import de.thm.arsnova.model.Comment;
import de.thm.arsnova.model.migration.v2.CommentReadingCount;

import java.util.List;

public interface CommentRepository extends CrudRepository<Comment, String> {
	int countByRoomId(String roomId);
	CommentReadingCount countReadingByRoomId(String roomId);
	CommentReadingCount countReadingByRoomIdAndUserId(String roomId, String userId);
	List<Comment> findByRoomId(String roomId, int start, int limit);
	List<Comment> findByRoomIdAndUserId(String roomId, String userId, int start, int limit);
	Comment findOne(String commentId);
	int deleteByRoomId(String roomId);
	int deleteByRoomIdAndUserId(String roomId, String userId);
}
