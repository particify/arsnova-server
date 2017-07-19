package de.thm.arsnova.services;

import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.CommentReadingCount;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.events.DeleteCommentEvent;
import de.thm.arsnova.events.NewCommentEvent;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.persistance.CommentRepository;
import de.thm.arsnova.persistance.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Performs all comment related operations.
 */
@Service
public class CommentServiceImpl implements CommentService {
	@Autowired
	private UserService userService;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private SessionRepository sessionRepository;

	private ApplicationEventPublisher publisher;

	@Override
	@PreAuthorize("isAuthenticated()")
	public boolean saveQuestion(final Comment comment) {
		final Session session = sessionRepository.getSessionFromKeyword(comment.getSessionId());
		final Comment result = commentRepository.saveQuestion(session.getId(), comment, userService.getCurrentUser());

		if (null != result) {
			final NewCommentEvent event = new NewCommentEvent(this, session, result);
			this.publisher.publishEvent(event);
			return true;
		}
		return false;
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#commentId, 'comment', 'owner')")
	public void deleteInterposedQuestion(final String commentId) {
		final Comment comment = commentRepository.getInterposedQuestion(commentId);
		if (comment == null) {
			throw new NotFoundException();
		}
		commentRepository.deleteInterposedQuestion(comment);

		final Session session = sessionRepository.getSessionFromKeyword(comment.getSessionId());
		final DeleteCommentEvent event = new DeleteCommentEvent(this, session, comment);
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteAllInterposedQuestions(final String sessionKeyword) {
		final Session session = sessionRepository.getSessionFromKeyword(sessionKeyword);
		if (session == null) {
			throw new UnauthorizedException();
		}
		final User user = getCurrentUser();
		if (session.isCreator(user)) {
			commentRepository.deleteAllInterposedQuestions(session.getId());
		} else {
			commentRepository.deleteAllInterposedQuestions(session.getId(), user);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getInterposedCount(final String sessionKey) {
		return commentRepository.getInterposedCount(sessionKey);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public CommentReadingCount getInterposedReadingCount(final String sessionKey, String username) {
		final Session session = sessionRepository.getSessionFromKeyword(sessionKey);
		if (session == null) {
			throw new NotFoundException();
		}
		if (username == null) {
			return commentRepository.getInterposedReadingCount(session.getId());
		} else {
			User currentUser = userService.getCurrentUser();
			if (!currentUser.getUsername().equals(username)) {
				throw new ForbiddenException();
			}

			return commentRepository.getInterposedReadingCount(session.getId(), currentUser);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Comment> getInterposedQuestions(final String sessionKey, final int offset, final int limit) {
		final Session session = this.getSession(sessionKey);
		final User user = getCurrentUser();
		if (session.isCreator(user)) {
			return commentRepository.getInterposedQuestions(session.getId(), offset, limit);
		} else {
			return commentRepository.getInterposedQuestions(session.getId(), user, offset, limit);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Comment readInterposedQuestion(final String commentId) {
		final User user = userService.getCurrentUser();
		return this.readInterposedQuestionInternal(commentId, user);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public Comment readInterposedQuestionInternal(final String commentId, User user) {
		final Comment comment = commentRepository.getInterposedQuestion(commentId);
		if (comment == null) {
			throw new NotFoundException();
		}
		final Session session = sessionRepository.getSessionFromId(comment.getSessionId());
		if (!comment.isCreator(user) && !session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		if (session.isCreator(user)) {
			commentRepository.markInterposedQuestionAsRead(comment);
		}
		return comment;
	}

	private User getCurrentUser() {
		final User user = userService.getCurrentUser();
		if (user == null) {
			throw new UnauthorizedException();
		}
		return user;
	}

	private Session getSession(final String sessionkey) {
		final Session session = sessionRepository.getSessionFromKeyword(sessionkey);
		if (session == null) {
			throw new NotFoundException();
		}
		return session;
	}
}
