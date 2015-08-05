/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.transport.InterposedQuestion;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.services.IQuestionService;
import de.thm.arsnova.web.DeprecatedApi;
import de.thm.arsnova.web.Pagination;

/**
 * Handles requests related to audience questions, which are also called interposed or feedback questions.
 */
@RestController
@RequestMapping("/audiencequestion")
@Api(value = "/audiencequestion", description = "the Audience Question API")
public class AudienceQuestionController extends PaginationController {

	public static final Logger LOGGER = LoggerFactory.getLogger(AudienceQuestionController.class);

	@Autowired
	private IQuestionService questionService;

	@ApiOperation(value = "Count all the questions in current session",
			nickname = "getAudienceQuestionCount",
			notes = "getInterposedCount(String sessionkey, String user)")
	@RequestMapping(value = "/count", method = RequestMethod.GET)
	@DeprecatedApi
	@Deprecated
	public int getInterposedCount(@ApiParam(value="Session-Key from current session", required=true) @RequestParam final String sessionkey) {
		return questionService.getInterposedCount(sessionkey);
	}

	@ApiOperation(value = "count all unread interposed questions",
			nickname = "getUnreadInterposedCount",
			notes = "getUnreadInterposedCount(String sessionkey, String user)")
	@RequestMapping(value = "/readcount", method = RequestMethod.GET)
	@DeprecatedApi
	@Deprecated
	public InterposedReadingCount getUnreadInterposedCount(@ApiParam(value = "Session-Key from current session", required = true) @RequestParam("sessionkey") final String sessionkey, String user) {
		return questionService.getInterposedReadingCount(sessionkey, user);
	}

	@ApiOperation(value = "Retrieves all Interposed Questions for a Session",
			nickname = "getInterposedQuestions",
			notes = "Repsonse structure: InterposedQuestion[], encoding-type: application/json")
	@RequestMapping(value = "/", method = RequestMethod.GET)
	@Pagination
	public List<InterposedQuestion> getInterposedQuestions(@ApiParam(value = "Session-Key from current session", required = true) @RequestParam final String sessionkey) {
		return InterposedQuestion.fromList(questionService.getInterposedQuestions(sessionkey, offset, limit));
	}

	@ApiOperation(value = "Retrieves an InterposedQuestion",
			nickname = "getInterposedQuestion",
			notes = "Repsonse structure: InterposedQuestion, encoding-type: application/json")
	@RequestMapping(value = "/{questionId}", method = RequestMethod.GET)
	public InterposedQuestion getInterposedQuestion(@ApiParam(value = "ID of the question that needs to be deleted", required = true) @PathVariable final String questionId) {
		return new InterposedQuestion(questionService.readInterposedQuestion(questionId));
	}

	@ApiOperation(value = "Creates a new Interposed Question for a Session and returns the InterposedQuestion's data",
			nickname = "postInterposedQuestion",
			notes = "Repsonse structure: InterposedQuestion, encoding-type: application/json")
	@ApiResponses(value = {
		@ApiResponse(code = 400, message = "Bad Request - The Api cannot or will not process the request due to something that is perceived to be a client error")
	})
	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void postInterposedQuestion(
			@ApiParam(value="Session-Key from current session", required=true) @RequestParam final String sessionkey,
			@ApiParam(value="the body from the new question", required=true) @RequestBody final de.thm.arsnova.entities.InterposedQuestion question
			) {
		if (questionService.saveQuestion(question)) {
			return;
		}

		throw new BadRequestException();
	}

	@ApiOperation(value = "Deletes an InterposedQuestion",
			nickname = "deleteInterposedQuestion",
			notes = "Repsonse structure: none, encoding-type: application/json")
	@RequestMapping(value = "/{questionId}", method = RequestMethod.DELETE)
	public void deleteInterposedQuestion(@ApiParam(value = "ID of the question that needs to be deleted", required=true) @PathVariable final String questionId) {
		questionService.deleteInterposedQuestion(questionId);
	}
}
