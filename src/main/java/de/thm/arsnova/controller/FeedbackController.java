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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.services.IFeedbackService;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.web.DeprecatedApi;

/**
 * Handles requests concerning the user's feedback, i.e., "too fast" or "faster, please". This HTTP API is
 * deprecated in favor of the socket implementation.
 *
 * @see de.thm.arsnova.socket.ARSnovaSocketIOServer
 */
@RestController
@Api(value = "/feedback", description = "the deprecated Feedback API")
public class FeedbackController extends AbstractController {

	public static final Logger LOGGER = LoggerFactory.getLogger(FeedbackController.class);

	@Autowired
	private IFeedbackService feedbackService;

	@Autowired
	private IUserService userService;

	@ApiOperation(value = "Get feedback from a session",
				  nickname = "getFeedback",
				  notes = "getFeedback(@PathVariable final String sessionkey)")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/session/{sessionkey}/feedback", method = RequestMethod.GET)
	public Feedback getFeedback(@PathVariable final String sessionkey) {
		return feedbackService.getFeedback(sessionkey);
	}

	@ApiOperation(value = "Get own feedback from a session",
				  nickname = "getMyFeedback",
				  notes = "getMyFeedback(@PathVariable final String sessionkey)")
	@ApiResponses(value = {
    		@ApiResponse(code = 404, message = "Not Found - The requested resource could not be found but may be available again in the future.")
	})
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/session/{sessionkey}/myfeedback", method = RequestMethod.GET)
	public Integer getMyFeedback(@PathVariable final String sessionkey) {
		Integer value = feedbackService.getMyFeedback(sessionkey, userService.getCurrentUser());
		if (value != null && value >= Feedback.MIN_FEEDBACK_TYPE && value <= Feedback.MAX_FEEDBACK_TYPE) {
			return value;
		}
		throw new NotFoundException();
	}

	@ApiOperation(value = "Get amout of feedback from a session",
				  nickname = "getFeedbackCount",
				  notes = "getFeedbackCount(@PathVariable final String sessionkey)")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/session/{sessionkey}/feedbackcount", method = RequestMethod.GET)
	public int getFeedbackCount(@PathVariable final String sessionkey) {
		return feedbackService.getFeedbackCount(sessionkey);
	}

	@ApiOperation(value = "Get the average feedback (rounded) from a session",
				  nickname = "getAverageFeedbackRounded",
				  notes = "getAverageFeedbackRounded(@PathVariable final String sessionkey)")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/session/{sessionkey}/roundedaveragefeedback", method = RequestMethod.GET)
	public long getAverageFeedbackRounded(@PathVariable final String sessionkey) {
		return feedbackService.getAverageFeedbackRounded(sessionkey);
	}

	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/session/{sessionkey}/averagefeedback", method = RequestMethod.GET)
	public double getAverageFeedback(@PathVariable final String sessionkey) {
		return feedbackService.getAverageFeedback(sessionkey);
	}

	@ApiOperation(value = "Post feedback for a session",
				  nickname = "postFeedback",
				  notes = "postFeedback(" +
					"@PathVariable final String sessionkey," +
					"@RequestBody final int value)")
	@ApiResponses(value = {
    		@ApiResponse(code = 404, message = "Not Found - The requested resource could not be found but may be available again in the future.")
	})
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/session/{sessionkey}/feedback", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Feedback postFeedback(
			@PathVariable final String sessionkey,
			@RequestBody final int value
			) {
		User user = userService.getCurrentUser();
		if (feedbackService.saveFeedback(sessionkey, value, user)) {
			Feedback feedback = feedbackService.getFeedback(sessionkey);
			if (feedback != null) {
				return feedback;
			}
			throw new RuntimeException();
		}

		throw new NotFoundException();
	}
}
