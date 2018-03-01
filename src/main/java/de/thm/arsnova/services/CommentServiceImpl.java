package de.thm.arsnova.services;

import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
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

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		final Room room = roomRepository.findOne(comment.getRoomId());
		final ClientAuthentication user = userService.getCurrentUser();
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

		final Room room = roomRepository.findOne(comment.getRoomId());
		final DeleteCommentEvent event = new DeleteCommentEvent(this, room, comment);
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteByRoomId(final String roomId) {
		final Room room = roomRepository.findOne(roomId);
		if (room == null) {
			throw new UnauthorizedException();
		}
		final ClientAuthentication user = getCurrentUser();
		if (room.getOwnerId().equals(user.getId())) {
			commentRepository.deleteByRoomId(room.getId());
		} else {
			commentRepository.deleteByRoomIdAndUser(room.getId(), user);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int count(final String roomId) {
		return commentRepository.countByRoomId(roomId);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public CommentReadingCount countRead(final String roomId, String username) {
		if (username == null) {
			return commentRepository.countReadingByRoomId(roomId);
		} else {
			ClientAuthentication currentUser = userService.getCurrentUser();
			if (!currentUser.getUsername().equals(username)) {
				throw new ForbiddenException();
			}

			return commentRepository.countReadingByRoomIdAndUser(roomId, currentUser);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Comment> getByRoomId(final String roomId, final int offset, final int limit) {
		final Room room = roomRepository.findOne(roomId);
		final ClientAuthentication user = getCurrentUser();
		if (room.getOwnerId().equals(user.getId())) {
			return commentRepository.findByRoomId(room.getId(), offset, limit);
		} else {
			return commentRepository.findByRoomIdAndUser(room.getId(), user, offset, limit);
		}
	}

	@Override
	@PreAuthorize("hasPermission(#commentId, 'comment', 'update')")
	public Comment getAndMarkRead(final String commentId) throws IOException {
		final Comment comment = commentRepository.findOne(commentId);
		if (comment == null) {
			throw new NotFoundException();
		}
		Map<String, Object> changes = new HashMap<>();
		changes.put("read", true);
		patch(comment, changes);

		return comment;
	}

	private ClientAuthentication getCurrentUser() {
		final ClientAuthentication user = userService.getCurrentUser();
		if (user == null) {
			throw new UnauthorizedException();
		}
		return user;
	}
}
