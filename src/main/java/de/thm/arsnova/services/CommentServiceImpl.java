package de.thm.arsnova.services;

import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.CommentReadingCount;
import de.thm.arsnova.entities.migration.v2.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.events.DeleteCommentEvent;
import de.thm.arsnova.events.NewCommentEvent;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.persistance.CommentRepository;
import de.thm.arsnova.persistance.SessionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Performs all comment related operations.
 */
@Service
public class CommentServiceImpl extends DefaultEntityServiceImpl<Comment> implements CommentService, ApplicationEventPublisherAware {
	private UserService userService;

	private CommentRepository commentRepository;

	private SessionRepository sessionRepository;

	private ApplicationEventPublisher publisher;

	public CommentServiceImpl(
			CommentRepository repository,
			SessionRepository sessionRepository,
			UserService userService,
			@Qualifier("defaultJsonMessageConverter") MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
		super(Comment.class, repository, jackson2HttpMessageConverter.getObjectMapper());
		this.commentRepository = repository;
		this.sessionRepository = sessionRepository;
		this.userService = userService;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public boolean save(final Comment comment) {
		final Session session = sessionRepository.findByKeyword(comment.getSessionId());
		final User user = userService.getCurrentUser();
		comment.setSessionId(session.getId());
		comment.setCreator(user.getUsername());
		comment.setRead(false);
		if (comment.getTimestamp() == 0) {
			comment.setTimestamp(System.currentTimeMillis());
		}
		final Comment result = super.create(comment);

		if (null != result) {
			final NewCommentEvent event = new NewCommentEvent(this, session, result);
			this.publisher.publishEvent(event);
			return true;
		}
		return false;
	}

	@Override
	@PreAuthorize("hasPermission(#commentId, 'comment', 'owner')")
	public void delete(final String commentId) {
		final Comment comment = commentRepository.findOne(commentId);
		if (comment == null) {
			throw new NotFoundException();
		}
		commentRepository.delete(comment);

		final Session session = sessionRepository.findByKeyword(comment.getSessionId());
		final DeleteCommentEvent event = new DeleteCommentEvent(this, session, comment);
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteBySessionKey(final String sessionKeyword) {
		final Session session = sessionRepository.findByKeyword(sessionKeyword);
		if (session == null) {
			throw new UnauthorizedException();
		}
		final User user = getCurrentUser();
		if (session.isCreator(user)) {
			commentRepository.deleteBySessionId(session.getId());
		} else {
			commentRepository.deleteBySessionIdAndUser(session.getId(), user);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int count(final String sessionKey) {
		return commentRepository.countBySessionId(getSession(sessionKey).getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public CommentReadingCount countRead(final String sessionKey, String username) {
		final Session session = sessionRepository.findByKeyword(sessionKey);
		if (session == null) {
			throw new NotFoundException();
		}
		if (username == null) {
			return commentRepository.countReadingBySessionId(session.getId());
		} else {
			User currentUser = userService.getCurrentUser();
			if (!currentUser.getUsername().equals(username)) {
				throw new ForbiddenException();
			}

			return commentRepository.countReadingBySessionIdAndUser(session.getId(), currentUser);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Comment> getBySessionKey(final String sessionKey, final int offset, final int limit) {
		final Session session = this.getSession(sessionKey);
		final User user = getCurrentUser();
		if (session.isCreator(user)) {
			return commentRepository.findBySessionId(session.getId(), offset, limit);
		} else {
			return commentRepository.findBySessionIdAndUser(session.getId(), user, offset, limit);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Comment getAndMarkRead(final String commentId) {
		final User user = userService.getCurrentUser();
		return this.getAndMarkReadInternal(commentId, user);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public Comment getAndMarkReadInternal(final String commentId, User user) {
		final Comment comment = commentRepository.findOne(commentId);
		if (comment == null) {
			throw new NotFoundException();
		}
		final Session session = sessionRepository.findOne(comment.getSessionId());
		if (!comment.isCreator(user) && !session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		if (session.isCreator(user)) {
			comment.setRead(true);
			save(comment);
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
		final Session session = sessionRepository.findByKeyword(sessionkey);
		if (session == null) {
			throw new NotFoundException();
		}
		return session;
	}
}
