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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class LegacyController extends AbstractController {

	public static final Logger LOGGER = LoggerFactory.getLogger(LegacyController.class);
	
	/* specific routes */

	@RequestMapping(value = "/session/{sessionKey}/question")
	public final String redirectQuestionByLecturer(
			@PathVariable final String sessionKey,
			final HttpServletResponse response
	) {
		response.addHeader("X-Deprecated-API", "1");
		
		return String.format("forward:/question/bylecturer/?sessionkey=%s", sessionKey);
	}

	@RequestMapping(value = "/session/{sessionKey}/skillquestions")
	public final String redirectQuestionByLecturerList(
			@PathVariable final String sessionKey,
			final HttpServletResponse response
	) {
		response.addHeader("X-Deprecated-API", "1");
		
		return String.format("forward:/question/bylecturer/list?sessionkey=%s", sessionKey);
	}

	@RequestMapping(value = "/session/{sessionKey}/skillquestioncount")
	public final String redirectQuestionByLecturerCount(
			@PathVariable final String sessionKey,
			final HttpServletResponse response
	) {
		response.addHeader("X-Deprecated-API", "1");
		
		return String.format("forward:/question/bylecturer/count?sessionkey=%s", sessionKey);
	}

	@RequestMapping(value = "/session/{sessionKey}/answercount")
	public final String redirectQuestionByLecturerAnswerCount(
			@PathVariable final String sessionKey,
			final HttpServletResponse response
	) {
		response.addHeader("X-Deprecated-API", "1");
		
		return String.format("forward:/question/bylecturer/answercount?sessionkey=%s", sessionKey);
	}
	
	/* generalized routes */

	@RequestMapping(value = "/session/{sessionKey}/question/{arg1}")
	public final String redirectQuestionByLecturerWithOneArgument(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			final HttpServletResponse response
	) {
		response.addHeader("X-Deprecated-API", "1");
		
		return String.format("forward:/question/bylecturer/%s?sessionkey=%s", arg1, sessionKey);
	}

	@RequestMapping(value = "/session/{sessionKey}/question/{arg1}/{arg2}")
	public final String redirectQuestionByLecturerWithTwoArguments(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			@PathVariable final String arg2,
			final HttpServletResponse response
	) {
		response.addHeader("X-Deprecated-API", "1");
		
		return String.format("forward:/question/bylecturer/%s/%s?sessionkey=%s", arg1, arg2, sessionKey);
	}

}
