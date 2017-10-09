package de.thm.arsnova.services;

import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.migration.v2.CommentReadingCount;
import de.thm.arsnova.events.DeleteCommentEvent;
import de.thm.arsnova.events.NewCommentEvent;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.persistance.CommentRepository;
import de.thm.arsnova.persistance.RoomRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Performs all comment related operations.
 */
@Service
public class CommentServiceImpl extends DefaultEntityServiceImpl<Comment> implements CommentService, ApplicationEventPublisherAware {
	private UserService userService;

	private CommentRepository commentRepository;

	private RoomRepository roomRepository;

	private ApplicationEventPublisher publisher;

	public CommentServiceImpl(
			CommentRepository repository,
			RoomRepository roomRepository,
			UserService userService,
			@Qualifier("defaultJsonMessageConverter") MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
		super(Comment.class, repository, jackson2HttpMessageConverter.getObjectMapper());
		this.commentRepository = repository;
		this.roomRepository = roomRepository;
		this.userService = userService;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public boolean save(final Comment comment) {
		final Room room = roomRepository.findByKeyword(comment.getSessionId());
		final UserAuthentication user = userService.getCurrentUser();
		comment.setSessionId(room.getId());
		comment.setCreatorId(user.getId());
		comment.setRead(false);
		if (comment.getTimestamp() == null) {
			comment.setTimestamp(new Date());
		}
		final Comment result = super.create(comment);

		if (null != result) {
			final NewCommentEvent event = new NewCommentEvent(this, room, result);
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

		final Room room = roomRepository.findByKeyword(comment.getSessionId());
		final DeleteCommentEvent event = new DeleteCommentEvent(this, room, comment);
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteBySessionKey(final String sessionKeyword) {
		final Room room = roomRepository.findByKeyword(sessionKeyword);
		if (room == null) {
			throw new UnauthorizedException();
		}
		final UserAuthentication user = getCurrentUser();
		if (room.getOwnerId().equals(user.getId())) {
			commentRepository.deleteBySessionId(room.getId());
		} else {
			commentRepository.deleteBySessionIdAndUser(room.getId(), user);
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
		final Room room = roomRepository.findByKeyword(sessionKey);
		if (room == null) {
			throw new NotFoundException();
		}
		if (username == null) {
			return commentRepository.countReadingBySessionId(room.getId());
		} else {
			UserAuthentication currentUser = userService.getCurrentUser();
			if (!currentUser.getUsername().equals(username)) {
				throw new ForbiddenException();
			}

			return commentRepository.countReadingBySessionIdAndUser(room.getId(), currentUser);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Comment> getBySessionKey(final String sessionKey, final int offset, final int limit) {
		final Room room = this.getSession(sessionKey);
		final UserAuthentication user = getCurrentUser();
		if (room.getOwnerId().equals(user.getId())) {
			return commentRepository.findBySessionId(room.getId(), offset, limit);
		} else {
			return commentRepository.findBySessionIdAndUser(room.getId(), user, offset, limit);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Comment getAndMarkRead(final String commentId) {
		final UserAuthentication user = userService.getCurrentUser();
		return this.getAndMarkReadInternal(commentId, user);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public Comment getAndMarkReadInternal(final String commentId, UserAuthentication user) {
		final Comment comment = commentRepository.findOne(commentId);
		if (comment == null) {
			throw new NotFoundException();
		}
		final Room room = roomRepository.findOne(comment.getSessionId());
		if (!comment.getCreatorId().equals(user.getId()) && !room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		if (room.getOwnerId().equals(user.getId())) {
			comment.setRead(true);
			save(comment);
		}
		return comment;
	}

	private UserAuthentication getCurrentUser() {
		final UserAuthentication user = userService.getCurrentUser();
		if (user == null) {
			throw new UnauthorizedException();
		}
		return user;
	}

	private Room getSession(final String sessionkey) {
		final Room room = roomRepository.findByKeyword(sessionkey);
		if (room == null) {
			throw new NotFoundException();
		}
		return room;
	}
}
