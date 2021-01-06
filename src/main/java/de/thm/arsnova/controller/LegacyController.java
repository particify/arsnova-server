/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2021 The ARSnova Team and Contributors
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

import de.thm.arsnova.services.IQuestionService;
import de.thm.arsnova.web.DeprecatedApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * This controller forwards requests from deprecated URLs to their new controller, where the requests are handled.
 */
@Controller
public class LegacyController extends AbstractController {

	@Autowired
	private IQuestionService questionService;

	/* specific routes */

	@DeprecatedApi
	@RequestMapping(value = "/session/mysessions", method = RequestMethod.GET)
	public String redirectSessionMy() {
		return "forward:/session/?ownedonly=true";
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/visitedsessions", method = RequestMethod.GET)
	public String redirectSessionVisited() {
		return "forward:/session/?visitedonly=true";
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/question")
	public String redirectQuestionByLecturer(@PathVariable final String sessionKey) {
		return String.format("forward:/lecturerquestion/?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/skillquestions", method = RequestMethod.GET)
	public String redirectQuestionByLecturerList(@PathVariable final String sessionKey) {
		return String.format("forward:/lecturerquestion/?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/skillquestioncount", method = RequestMethod.GET)
	public String redirectQuestionByLecturerCount(@PathVariable final String sessionKey) {
		return String.format("forward:/lecturerquestion/count?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/answercount", method = RequestMethod.GET)
	public String redirectQuestionByLecturerAnswerCount(@PathVariable final String sessionKey) {
		return String.format("forward:/lecturerquestion/answercount?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/unanswered", method = RequestMethod.GET)
	public String redirectQuestionByLecturerUnnsweredCount(@PathVariable final String sessionKey) {
		return String.format("forward:/lecturerquestion/answercount?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/myanswers", method = RequestMethod.GET)
	public String redirectQuestionByLecturerMyAnswers(@PathVariable final String sessionKey) {
		return String.format("forward:/lecturerquestion/myanswers?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/interposed")
	public String redirectQuestionByAudience(@PathVariable final String sessionKey) {
		return String.format("forward:/audiencequestion/?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/interposed", method = RequestMethod.DELETE)
	@ResponseBody
	public void deleteAllInterposedQuestions(@PathVariable final String sessionKey) {
		questionService.deleteAllInterposedQuestions(sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/interposedcount", method = RequestMethod.GET)
	public String redirectQuestionByAudienceCount(@PathVariable final String sessionKey) {
		return String.format("forward:/audiencequestion/count?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/interposedreadingcount", method = RequestMethod.GET)
	public String redirectQuestionByAudienceReadCount(@PathVariable final String sessionKey) {
		return String.format("forward:/audiencequestion/readcount?sessionkey=%s", sessionKey);
	}

	/* generalized routes */

	@DeprecatedApi
	@RequestMapping(value = { "/session/{sessionKey}/question/{arg1}", "/session/{sessionKey}/questions/{arg1}" })
	public String redirectQuestionByLecturerWithOneArgument(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1
			) {
		return String.format("forward:/lecturerquestion/%s/?sessionkey=%s", arg1, sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(
			value = { "/session/{sessionKey}/question/{arg1}/{arg2}", "/session/{sessionKey}/questions/{arg1}/{arg2}" }
			)
	public String redirectQuestionByLecturerWithTwoArguments(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			@PathVariable final String arg2
			) {
		return String.format("forward:/lecturerquestion/%s/%s/?sessionkey=%s", arg1, arg2, sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/interposed/{arg1}")
	public String redirectQuestionByAudienceWithOneArgument(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1
			) {
		return String.format("forward:/audiencequestion/%s/?sessionkey=%s", arg1, sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/interposed/{arg1}/{arg2}")
	public String redirectQuestionByAudienceWithTwoArguments(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			@PathVariable final String arg2
			) {
		return String.format("forward:/audiencequestion/%s/%s/?sessionkey=%s", arg1, arg2, sessionKey);
	}
}
