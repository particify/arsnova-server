package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.migration.v2.Comment;
import de.thm.arsnova.entities.migration.v2.CommentReadingCount;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CommentRepository extends CrudRepository<Comment, String> {
	int countBySessionId(String sessionKey);
	CommentReadingCount countReadingBySessionId(String sessionId);
	CommentReadingCount countReadingBySessionIdAndUser(String sessionId, UserAuthentication user);
	List<Comment> findBySessionId(String sessionId, int start, int limit);
	List<Comment> findBySessionIdAndUser(String sessionId, UserAuthentication user, int start, int limit);
	Comment findOne(String commentId);
	int deleteBySessionId(String sessionId);
	int deleteBySessionIdAndUser(String sessionId, UserAuthentication user);
}
