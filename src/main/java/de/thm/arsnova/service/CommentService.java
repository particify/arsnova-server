package de.thm.arsnova.service;

import de.thm.arsnova.model.Comment;
import de.thm.arsnova.model.migration.v2.CommentReadingCount;

import java.io.IOException;
import java.util.List;

public interface CommentService extends EntityService<Comment> {
	int count(String roomId);

	CommentReadingCount countRead(String roomId, String username);

	List<Comment> getByRoomId(String roomId, int offset, int limit);

	Comment getAndMarkRead(String commentId) throws IOException;

	void deleteByRoomId(String roomId);
}
