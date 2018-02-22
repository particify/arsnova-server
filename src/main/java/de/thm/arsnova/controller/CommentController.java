/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
package de.thm.arsnova.controller;

import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.CommentReadingCount;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.services.CommentService;
import de.thm.arsnova.web.DeprecatedApi;
import de.thm.arsnova.web.Pagination;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Handles requests related to comments.
 */
@RestController
@RequestMapping("/audiencequestion")
@Api(value = "/audiencequestion", description = "the Audience Content API")
public class CommentController extends PaginationController {

	@Autowired
	private CommentService commentService;

	@ApiOperation(value = "Count all the comments in current session",
			nickname = "getAudienceQuestionCount")
	@RequestMapping(value = "/count", method = RequestMethod.GET)
	@DeprecatedApi
	@Deprecated
	public int getInterposedCount(@ApiParam(value = "Session-Key from current session", required = true) @RequestParam final String sessionkey) {
		return commentService.count(sessionkey);
	}

	@ApiOperation(value = "count all unread comments",
			nickname = "getUnreadInterposedCount")
	@RequestMapping(value = "/readcount", method = RequestMethod.GET)
	@DeprecatedApi
	@Deprecated
	public CommentReadingCount getUnreadInterposedCount(@ApiParam(value = "Session-Key from current session", required = true) @RequestParam("sessionkey") final String sessionkey, String user) {
		return commentService.countRead(sessionkey, user);
	}

	@ApiOperation(value = "Retrieves all Comments for a Session",
			nickname = "getInterposedQuestions")
	@RequestMapping(value = "/", method = RequestMethod.GET)
	@Pagination
	public List<Comment> getInterposedQuestions(@ApiParam(value = "Session-Key from current session", required = true) @RequestParam final String sessionkey) {
		return commentService.getBySessionKey(sessionkey, offset, limit);
	}

	@ApiOperation(value = "Retrieves an Comment",
			nickname = "getInterposedQuestion")
	@RequestMapping(value = "/{questionId}", method = RequestMethod.GET)
	public Comment getInterposedQuestion(@ApiParam(value = "ID of the Comment that needs to be deleted", required = true) @PathVariable final String questionId) {
		return commentService.getAndMarkRead(questionId);
	}

	@ApiOperation(value = "Creates a new Comment for a Session and returns the Comment's data",
			nickname = "postInterposedQuestion")
	@ApiResponses(value = {
		@ApiResponse(code = 400, message = HTML_STATUS_400)
	})
	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void postInterposedQuestion(
			@ApiParam(value = "Session-Key from current session", required = true) @RequestParam final String sessionkey,
			@ApiParam(value = "the body from the new comment", required = true) @RequestBody final de.thm.arsnova.entities.Comment comment
			) {
		if (commentService.save(comment)) {
			return;
		}

		throw new BadRequestException();
	}

	@ApiOperation(value = "Deletes a Comment",
			nickname = "deleteInterposedQuestion")
	@RequestMapping(value = "/{questionId}", method = RequestMethod.DELETE)
	public void deleteInterposedQuestion(@ApiParam(value = "ID of the comment that needs to be deleted", required = true) @PathVariable final String questionId) {
		commentService.delete(questionId);
	}
}
