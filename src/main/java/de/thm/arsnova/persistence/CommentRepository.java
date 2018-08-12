package de.thm.arsnova.persistence;

import de.thm.arsnova.model.Comment;
import de.thm.arsnova.model.migration.v2.ClientAuthentication;
import de.thm.arsnova.model.migration.v2.CommentReadingCount;

import java.util.List;

public interface CommentRepository extends CrudRepository<Comment, String> {
	int countByRoomId(String roomId);
	CommentReadingCount countReadingByRoomId(String roomId);
	CommentReadingCount countReadingByRoomIdAndUser(String roomId, ClientAuthentication user);
	List<Comment> findByRoomId(String roomId, int start, int limit);
	List<Comment> findByRoomIdAndUser(String roomId, ClientAuthentication user, int start, int limit);
	Comment findOne(String commentId);
	int deleteByRoomId(String roomId);
	int deleteByRoomIdAndUser(String roomId, ClientAuthentication user);
}
