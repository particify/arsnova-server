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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.controller.AbstractController;
import de.thm.arsnova.model.Feedback;
import de.thm.arsnova.security.User;
import de.thm.arsnova.service.FeedbackService;
import de.thm.arsnova.service.RoomService;
import de.thm.arsnova.service.UserService;
import de.thm.arsnova.web.DeprecatedApi;
import de.thm.arsnova.web.exceptions.NotFoundException;
import de.thm.arsnova.websocket.ArsnovaSocketioServerImpl;

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
	@GetMapping("/feedback")
	public Feedback getFeedback(@PathVariable final String shortId) {
		return feedbackService.getByRoomId(roomService.getIdByShortId(shortId));
	}

	@DeprecatedApi
	@Deprecated
	@GetMapping(value = "/myfeedback", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getMyFeedback(@PathVariable final String shortId) {
		final String roomId = roomService.getIdByShortId(shortId);
		final Integer value = feedbackService.getByRoomIdAndUserId(roomId, userService.getCurrentUser().getId());
		if (value != null && value >= Feedback.MIN_FEEDBACK_TYPE && value <= Feedback.MAX_FEEDBACK_TYPE) {
			return value.toString();
		}
		throw new NotFoundException();
	}

	@DeprecatedApi
	@Deprecated
	@GetMapping(value = "/feedbackcount", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getFeedbackCount(@PathVariable final String shortId) {
		return String.valueOf(feedbackService.countFeedbackByRoomId(roomService.getIdByShortId(shortId)));
	}

	@DeprecatedApi
	@Deprecated
	@GetMapping(value = "/roundedaveragefeedback", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getAverageFeedbackRounded(@PathVariable final String shortId) {
		return String.valueOf(feedbackService.calculateRoundedAverageFeedback(roomService.getIdByShortId(shortId)));
	}

	@DeprecatedApi
	@Deprecated
	@GetMapping(value = "/averagefeedback", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getAverageFeedback(@PathVariable final String shortId) {
		return String.valueOf(feedbackService.calculateAverageFeedback(roomService.getIdByShortId(shortId)));
	}

	@DeprecatedApi
	@Deprecated
	@PostMapping("/feedback")
	@ResponseStatus(HttpStatus.CREATED)
	public Feedback postFeedback(
			@PathVariable final String shortId,
			@RequestBody final int value) {
		final String roomId = roomService.getIdByShortId(shortId);
		final User user = userService.getCurrentUser();
		feedbackService.save(roomId, value, user.getId());
		final Feedback feedback = feedbackService.getByRoomId(roomId);

		return feedback;
	}
}
