package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import de.thm.arsnova.entities.migration.v2.CommentReadingCount;
import org.springframework.data.repository.CrudRepository;

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
