package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.CommentReadingCount;
import de.thm.arsnova.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CommentRepository extends CrudRepository<Comment, String> {
	int getInterposedCount(String sessionKey);
	CommentReadingCount getInterposedReadingCount(String sessionId);
	CommentReadingCount getInterposedReadingCount(String sessionId, User user);
	List<Comment> getInterposedQuestions(String sessionId, int start, int limit);
	List<Comment> getInterposedQuestions(String sessionId, User user, int start, int limit);
	Comment getInterposedQuestion(String commentId);
	Comment saveQuestion(String sessionId, Comment comment, User user);
	void markInterposedQuestionAsRead(Comment comment);
	void deleteInterposedQuestion(Comment comment);
	int deleteAllInterposedQuestions(String sessionId);
	int deleteAllInterposedQuestions(String sessionId, User user);
}
