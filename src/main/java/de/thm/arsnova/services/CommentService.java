package de.thm.arsnova.services;

import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.CommentReadingCount;
import de.thm.arsnova.entities.User;

import java.util.List;

public interface CommentService extends EntityService<Comment> {
	boolean save(Comment comment);

	int count(String sessionKey);

	CommentReadingCount countRead(String sessionKey, String username);

	List<Comment> getBySessionKey(String sessionKey, int offset, int limit);

	Comment getAndMarkRead(String commentId);

	Comment getAndMarkReadInternal(String commentId, User user);

	void delete(String commentId);

	void deleteBySessionKey(String sessionKeyword);
}
