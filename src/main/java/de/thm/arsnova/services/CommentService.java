package de.thm.arsnova.services;

import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.migration.v2.CommentReadingCount;

import java.util.List;

public interface CommentService extends EntityService<Comment> {
	boolean save(Comment comment);

	int count(String roomShortId);

	CommentReadingCount countRead(String roomShortId, String username);

	List<Comment> getByRoomShortId(String roomShortId, int offset, int limit);

	Comment getAndMarkRead(String commentId);

	Comment getAndMarkReadInternal(String commentId, UserAuthentication user);

	void delete(String commentId);

	void deleteByRoomShortId(String roomShortId);
}
