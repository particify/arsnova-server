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

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.services.IFeedbackService;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Controller
public class FeedbackController extends AbstractController {

	public static final Logger logger = LoggerFactory.getLogger(FeedbackController.class);

	@Autowired
	IFeedbackService feedbackService;

	@Autowired
	IUserService userService;

	@Autowired
	ARSnovaSocketIOServer server;

	@RequestMapping(value = "/session/{sessionkey}/feedback", method = RequestMethod.GET)
	@ResponseBody
	public Feedback getFeedback(@PathVariable String sessionkey) {
		return feedbackService.getFeedback(sessionkey);
	}

	@RequestMapping(value = "/session/{sessionkey}/myfeedback", method = RequestMethod.GET)
	@ResponseBody
	public Integer getMyFeedback(@PathVariable String sessionkey, HttpServletResponse response) {
		Integer value = feedbackService.getMyFeedback(sessionkey, userService.getCurrentUser());
		if (value != null && value >= 0 && value <= 3) {
			return value;
		}
		response.setStatus(HttpStatus.NOT_FOUND.value());
		return null;
	}

	@RequestMapping(value = "/session/{sessionkey}/feedbackcount", method = RequestMethod.GET)
	@ResponseBody
	public int getFeedbackCount(@PathVariable String sessionkey) {
		return feedbackService.getFeedbackCount(sessionkey);
	}

	@RequestMapping(value = "/session/{sessionkey}/roundedaveragefeedback", method = RequestMethod.GET)
	@ResponseBody
	public long getAverageFeedbackRounded(
		@PathVariable String sessionkey
	) {
		return feedbackService.getAverageFeedbackRounded(sessionkey);
	}
	
	@RequestMapping(value = "/session/{sessionkey}/averagefeedback", method = RequestMethod.GET)
	@ResponseBody
	public double getAverageFeedback(
		@PathVariable String sessionkey
	) {
		return feedbackService.getAverageFeedback(sessionkey);
	}

	@RequestMapping(value = "/session/{sessionkey}/feedback", method = RequestMethod.POST)
	@ResponseBody
	public Feedback postFeedback(@PathVariable String sessionkey, @RequestBody int value, HttpServletResponse response) {
		User user = userService.getCurrentUser();
		if (feedbackService.saveFeedback(sessionkey, value, user)) {
			Feedback feedback = feedbackService.getFeedback(sessionkey);
			if (feedback != null) {
				// TODO: Broadcast feedback changes via websocket
				response.setStatus(HttpStatus.CREATED.value());
				return feedback;
			}

			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return null;
		}

		response.setStatus(HttpStatus.BAD_REQUEST.value());
		return null;
	}
}
