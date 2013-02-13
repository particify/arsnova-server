/*
 * Copyright (C) 2012 THM webMedia
 *
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.controller;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.services.IFeedbackService;
import de.thm.arsnova.services.IUserService;

@Controller
public class FeedbackController extends AbstractController {

	public static final Logger LOGGER = LoggerFactory.getLogger(FeedbackController.class);

	@Autowired
	private IFeedbackService feedbackService;

	@Autowired
	private IUserService userService;

	@RequestMapping(value = "/session/{sessionkey}/feedback", method = RequestMethod.GET)
	@ResponseBody
	public final Feedback getFeedback(@PathVariable final String sessionkey) {
		return feedbackService.getFeedback(sessionkey);
	}

	@RequestMapping(value = "/session/{sessionkey}/myfeedback", method = RequestMethod.GET)
	@ResponseBody
	public final Integer getMyFeedback(@PathVariable final String sessionkey, final HttpServletResponse response) {
		Integer value = feedbackService.getMyFeedback(sessionkey, userService.getCurrentUser());

		if (value != null && value >= Feedback.MIN_FEEDBACK_TYPE && value <= Feedback.MAX_FEEDBACK_TYPE) {
			return value;
		}
		throw new NotFoundException();
	}

	@RequestMapping(value = "/session/{sessionkey}/feedbackcount", method = RequestMethod.GET)
	@ResponseBody
	public final int getFeedbackCount(@PathVariable final String sessionkey) {
		return feedbackService.getFeedbackCount(sessionkey);
	}

	@RequestMapping(value = "/session/{sessionkey}/roundedaveragefeedback", method = RequestMethod.GET)
	@ResponseBody
	public final long getAverageFeedbackRounded(@PathVariable final String sessionkey) {
		return feedbackService.getAverageFeedbackRounded(sessionkey);
	}

	@RequestMapping(value = "/session/{sessionkey}/averagefeedback", method = RequestMethod.GET)
	@ResponseBody
	public final double getAverageFeedback(@PathVariable final String sessionkey) {
		return feedbackService.getAverageFeedback(sessionkey);
	}

	@RequestMapping(value = "/session/{sessionkey}/feedback", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	public final Feedback postFeedback(
			@PathVariable final String sessionkey,
			@RequestBody final int value,
			final HttpServletResponse response
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
