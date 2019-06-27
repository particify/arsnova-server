/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.service;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.event.BeforeDeletionEvent;
import de.thm.arsnova.model.Comment;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.migration.v2.CommentReadingCount;
import de.thm.arsnova.persistence.CommentRepository;
import de.thm.arsnova.security.User;
import de.thm.arsnova.web.exceptions.ForbiddenException;
import de.thm.arsnova.web.exceptions.NotFoundException;
import de.thm.arsnova.web.exceptions.UnauthorizedException;

/**
 * Performs all comment related operations.
 */
@Service
public class CommentServiceImpl extends DefaultEntityServiceImpl<Comment> implements CommentService {
	private UserService userService;

	private RoomService roomService;

	private CommentRepository commentRepository;

	public CommentServiceImpl(
			final CommentRepository repository,
			final RoomService roomService,
			final UserService userService,
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
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
	public CommentReadingCount countRead(final String roomId, final String username) {
		if (username == null) {
			return commentRepository.countReadingByRoomId(roomId);
		} else {
			final User user = userService.getCurrentUser();
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
		final Map<String, Object> changes = new HashMap<>();
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
