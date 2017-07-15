package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.CommentReadingCount;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

import java.util.List;

public interface CommentRepository {
	int getInterposedCount(String sessionKey);
	CommentReadingCount getInterposedReadingCount(Session session);
	CommentReadingCount getInterposedReadingCount(Session session, User user);
	List<Comment> getInterposedQuestions(Session session, final int start, final int limit);
	List<Comment> getInterposedQuestions(Session session, User user, final int start, final int limit);
	Comment getInterposedQuestion(String commentId);
	Comment saveQuestion(Session session, Comment comment, User user);
	void markInterposedQuestionAsRead(Comment comment);
	void deleteInterposedQuestion(Comment comment);
	int deleteAllInterposedQuestions(Session session);
	int deleteAllInterposedQuestions(Session session, User user);
}
