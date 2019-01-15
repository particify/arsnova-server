package de.thm.arsnova.service;

import de.thm.arsnova.event.BeforeDeletionEvent;
import de.thm.arsnova.model.Comment;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.migration.v2.CommentReadingCount;
import de.thm.arsnova.persistence.CommentRepository;
import de.thm.arsnova.security.User;
import de.thm.arsnova.web.exceptions.ForbiddenException;
import de.thm.arsnova.web.exceptions.NotFoundException;
import de.thm.arsnova.web.exceptions.UnauthorizedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.annotation.Secured;
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
public class CommentServiceImpl extends DefaultEntityServiceImpl<Comment> implements CommentService {
	private UserService userService;

	private RoomService roomService;

	private CommentRepository commentRepository;

	public CommentServiceImpl(
			CommentRepository repository,
			RoomService roomService,
			UserService userService,
			@Qualifier("defaultJsonMessageConverter") MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
		super(Comment.class, repository, jackson2HttpMessageConverter.getObjectMapper());
		this.commentRepository = repository;
		this.roomService = roomService;
		this.userService = userService;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void prepareCreate(final Comment comment) {
		final Room room = roomService.get(comment.getRoomId());
		final User user = userService.getCurrentUser();
		comment.setCreatorId(user.getId());
		comment.setRead(false);
		if (comment.getTimestamp() == null) {
			comment.setTimestamp(new Date());
		}
		/* TODO: fire event */
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteByRoomId(final String roomId) {
		final Room room = roomService.get(roomId);
		if (room == null) {
			throw new UnauthorizedException();
		}
		final User user = getCurrentUser();
		if (room.getOwnerId().equals(user.getId())) {
			delete(commentRepository.findStubsByRoomId(room.getId()));
		} else {
			delete(commentRepository.findStubsByRoomIdAndUserId(room.getId(), user.getId()));
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
			User user = userService.getCurrentUser();
			if (!user.getUsername().equals(username)) {
				throw new ForbiddenException();
			}

			return commentRepository.countReadingByRoomIdAndUserId(roomId, user.getId());
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Comment> getByRoomId(final String roomId, final int offset, final int limit) {
		final Room room = roomService.get(roomId);
		final User user = getCurrentUser();
		if (room.getOwnerId().equals(user.getId())) {
			return commentRepository.findByRoomId(room.getId(), offset, limit);
		} else {
			return commentRepository.findByRoomIdAndUserId(room.getId(), user.getId(), offset, limit);
		}
	}

	@Override
	@PreAuthorize("hasPermission(#commentId, 'comment', 'update')")
	public Comment getAndMarkRead(final String commentId) throws IOException {
		final Comment comment = get(commentId);
		if (comment == null) {
			throw new NotFoundException();
		}
		Map<String, Object> changes = new HashMap<>();
		changes.put("read", true);
		patch(comment, changes);

		return comment;
	}

	private User getCurrentUser() {
		final User user = userService.getCurrentUser();
		if (user == null) {
			throw new UnauthorizedException();
		}
		return user;
	}

	@EventListener
	@Secured({"ROLE_USER", "RUN_AS_SYSTEM"})
	public void handleRoomDeletion(final BeforeDeletionEvent<Room> event) {
		final Iterable<Comment> comments = commentRepository.findStubsByRoomId(event.getEntity().getId());
		delete(comments);
	}
}
