/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.services.FeedbackService;
import de.thm.arsnova.services.UserService;
import de.thm.arsnova.websocket.ArsnovaSocketioServerImpl;
import de.thm.arsnova.web.DeprecatedApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles requests concerning the user's feedback, i.e., "too fast" or "faster, please". This HTTP API is
 * deprecated in favor of the socket implementation.
 *
 * @see ArsnovaSocketioServerImpl
 */
@RestController
public class FeedbackController extends AbstractController {
	@Autowired
	private FeedbackService feedbackService;

	@Autowired
	private UserService userService;

	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/session/{sessionkey}/feedback", method = RequestMethod.GET)
	public Feedback getFeedback(@PathVariable final String sessionkey) {
		return feedbackService.getBySessionKey(sessionkey);
	}

	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/session/{sessionkey}/myfeedback", method = RequestMethod.GET)
	public Integer getMyFeedback(@PathVariable final String sessionkey) {
		Integer value = feedbackService.getBySessionKeyAndUser(sessionkey, userService.getCurrentUser());
		if (value != null && value >= Feedback.MIN_FEEDBACK_TYPE && value <= Feedback.MAX_FEEDBACK_TYPE) {
			return value;
		}
		throw new NotFoundException();
	}

	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/session/{sessionkey}/feedbackcount", method = RequestMethod.GET)
	public int getFeedbackCount(@PathVariable final String sessionkey) {
		return feedbackService.countFeedbackBySessionKey(sessionkey);
	}

	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/session/{sessionkey}/roundedaveragefeedback", method = RequestMethod.GET)
	public long getAverageFeedbackRounded(@PathVariable final String sessionkey) {
		return feedbackService.calculateRoundedAverageFeedback(sessionkey);
	}

	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/session/{sessionkey}/averagefeedback", method = RequestMethod.GET)
	public double getAverageFeedback(@PathVariable final String sessionkey) {
		return feedbackService.calculateAverageFeedback(sessionkey);
	}

	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/session/{sessionkey}/feedback", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Feedback postFeedback(
			@PathVariable final String sessionkey,
			@RequestBody final int value
			) {
		User user = userService.getCurrentUser();
		feedbackService.save(sessionkey, value, user);
		Feedback feedback = feedbackService.getBySessionKey(sessionkey);

		return feedback;
	}
}
