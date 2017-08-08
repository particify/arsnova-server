package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.CommentReadingCount;
import de.thm.arsnova.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CommentRepository extends CrudRepository<Comment, String> {
	int countBySessionId(String sessionKey);
	CommentReadingCount countReadingBySessionId(String sessionId);
	CommentReadingCount countReadingBySessionIdAndUser(String sessionId, User user);
	List<Comment> findBySessionId(String sessionId, int start, int limit);
	List<Comment> findBySessionIdAndUser(String sessionId, User user, int start, int limit);
	Comment findOne(String commentId);
	Comment save(String sessionId, Comment comment, User user);
	void markInterposedQuestionAsRead(Comment comment);
	void delete(Comment comment);
	int deleteBySessionId(String sessionId);
	int deleteBySessionIdAndUser(String sessionId, User user);
}
