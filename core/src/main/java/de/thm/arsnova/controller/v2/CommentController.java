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

package de.thm.arsnova.controller.v2;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.controller.PaginationController;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.migration.FromV2Migrator;
import de.thm.arsnova.model.migration.ToV2Migrator;
import de.thm.arsnova.model.migration.v2.Comment;
import de.thm.arsnova.model.migration.v2.CommentReadingCount;
import de.thm.arsnova.service.CommentService;
import de.thm.arsnova.service.RoomService;
import de.thm.arsnova.service.UserService;
import de.thm.arsnova.web.DeprecatedApi;
import de.thm.arsnova.web.Pagination;

/**
 * Handles requests related to comments.
 */
@RestController("v2CommentController")
@RequestMapping("/v2/audiencequestion")
@Api(value = "/audiencequestion", description = "Comment (Interposed/Audience Question) API")
public class CommentController extends PaginationController {

	@Autowired
	private CommentService commentService;

	@Autowired
	private RoomService roomService;

	@Autowired
	private UserService userService;

	@Autowired
	private ToV2Migrator toV2Migrator;

	@Autowired
	private FromV2Migrator fromV2Migrator;

	@ApiOperation(value = "Count all the comments in current room",
			nickname = "getCommentCount")
	@GetMapping(value = "/count", produces = MediaType.TEXT_PLAIN_VALUE)
	@DeprecatedApi
	@Deprecated
	public String getCommentCount(
			@ApiParam(value = "Room-Key from current room", required = true)
			@RequestParam("sessionkey")
			final String roomShortId) {
		return String.valueOf(commentService.count(roomService.getIdByShortId(roomShortId)));
	}

	@ApiOperation(value = "count all unread comments",
			nickname = "getUnreadCommentCount")
	@GetMapping("/readcount")
	@DeprecatedApi
	@Deprecated
	public CommentReadingCount getUnreadCommentCount(
			@ApiParam(value = "Room-Key from current room", required = true)
			@RequestParam("sessionkey") final String roomShortId, final String user) {
		return commentService.countRead(roomService.getIdByShortId(roomShortId), user);
	}

	@ApiOperation(value = "Retrieves all Comments for a Room",
			nickname = "getComments")
	@GetMapping("/")
	@Pagination
	public List<Comment> getComments(
			@ApiParam(value = "Room-Key from current room", required = true)
			@RequestParam("sessionkey")
			final String roomShortId) {
		return commentService.getByRoomId(roomService.getIdByShortId(roomShortId), offset, limit).stream()
				.map(toV2Migrator::migrate).collect(Collectors.toList());
	}

	@ApiOperation(value = "Retrieves an Comment",
			nickname = "getComment")
	@GetMapping("/{commentId}")
	public Comment getComment(
			@ApiParam(value = "ID of the Comment that needs to be deleted", required = true)
			@PathVariable
			final String commentId)
			throws IOException {
		return toV2Migrator.migrate(commentService.getAndMarkRead(commentId));
	}

	@ApiOperation(value = "Creates a new Comment for a Room and returns the Comment's data",
			nickname = "postComment")
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = HTML_STATUS_400)
	})
	@PostMapping("/")
	@ResponseStatus(HttpStatus.CREATED)
	public void postComment(
			@ApiParam(value = "Room-Key from current room", required = true)
			@RequestParam("sessionkey")
			final String roomShortId,
			@ApiParam(value = "the body from the new comment", required = true)
			@RequestBody
			final Comment comment) {
		final de.thm.arsnova.model.Comment commentV3 = fromV2Migrator.migrate(comment);
		final Room roomV3 = roomService.getByShortId(roomShortId);
		commentV3.setRoomId(roomV3.getId());
		commentService.create(commentV3);
	}

	@ApiOperation(value = "Deletes a Comment",
			nickname = "deleteComment")
	@DeleteMapping("/{commentId}")
	public void deleteComment(
			@ApiParam(value = "ID of the comment that needs to be deleted", required = true)
			@PathVariable
			final String commentId) {
		commentService.delete(commentService.get(commentId));
	}
}
