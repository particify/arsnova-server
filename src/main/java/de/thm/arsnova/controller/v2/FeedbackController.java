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
package de.thm.arsnova.controller.v2;

import de.thm.arsnova.controller.AbstractController;
import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.services.FeedbackService;
import de.thm.arsnova.services.RoomService;
import de.thm.arsnova.services.UserService;
import de.thm.arsnova.web.DeprecatedApi;
import de.thm.arsnova.websocket.ArsnovaSocketioServerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RestController("v2FeedbackController")
@RequestMapping("/v2/session/{shortId}")
public class FeedbackController extends AbstractController {
	@Autowired
	private FeedbackService feedbackService;

	@Autowired
	private RoomService roomService;

	@Autowired
	private UserService userService;

	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/feedback", method = RequestMethod.GET)
	public Feedback getFeedback(@PathVariable final String shortId) {
		return feedbackService.getByRoomId(roomService.getIdByShortId(shortId));
	}

	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/myfeedback", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public String getMyFeedback(@PathVariable final String shortId) {
		String roomId = roomService.getIdByShortId(shortId);
		Integer value = feedbackService.getByRoomIdAndUser(roomId, userService.getCurrentUser());
		if (value != null && value >= Feedback.MIN_FEEDBACK_TYPE && value <= Feedback.MAX_FEEDBACK_TYPE) {
			return value.toString();
		}
		throw new NotFoundException();
	}

	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/feedbackcount", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public String getFeedbackCount(@PathVariable final String shortId) {
		return String.valueOf(feedbackService.countFeedbackByRoomId(roomService.getIdByShortId(shortId)));
	}

	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/roundedaveragefeedback", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public String getAverageFeedbackRounded(@PathVariable final String shortId) {
		return String.valueOf(feedbackService.calculateRoundedAverageFeedback(roomService.getIdByShortId(shortId)));
	}

	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/averagefeedback", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public String getAverageFeedback(@PathVariable final String shortId) {
		return String.valueOf(feedbackService.calculateAverageFeedback(roomService.getIdByShortId(shortId)));
	}

	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/feedback", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Feedback postFeedback(
			@PathVariable final String shortId,
			@RequestBody final int value
			) {
		String roomId = roomService.getIdByShortId(shortId);
		ClientAuthentication user = userService.getCurrentUser();
		feedbackService.save(roomId, value, user);
		Feedback feedback = feedbackService.getByRoomId(roomId);

		return feedback;
	}
}
