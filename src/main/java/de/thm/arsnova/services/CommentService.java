package de.thm.arsnova.services;

import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.CommentReadingCount;
import de.thm.arsnova.entities.User;

import java.util.List;

public interface CommentService {
	boolean saveQuestion(Comment comment);

	int getInterposedCount(String sessionKey);

	CommentReadingCount getInterposedReadingCount(String sessionKey, String username);

	List<Comment> getInterposedQuestions(String sessionKey, int offset, int limit);

	Comment readInterposedQuestion(String commentId);

	Comment readInterposedQuestionInternal(String commentId, User user);

	void deleteInterposedQuestion(String commentId);

	void deleteAllInterposedQuestions(String sessionKeyword);
}
