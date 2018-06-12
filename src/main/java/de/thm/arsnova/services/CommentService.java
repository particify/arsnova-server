package de.thm.arsnova.services;

import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.migration.v2.CommentReadingCount;

import java.io.IOException;
import java.util.List;

public interface CommentService extends EntityService<Comment> {
	int count(String roomId);

	CommentReadingCount countRead(String roomId, String username);

	List<Comment> getByRoomId(String roomId, int offset, int limit);

	Comment getAndMarkRead(String commentId) throws IOException;

	void delete(String commentId);

	void deleteByRoomId(String roomId);
}
