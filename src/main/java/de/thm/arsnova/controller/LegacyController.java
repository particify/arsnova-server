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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.thm.arsnova.services.IQuestionService;
import de.thm.arsnova.web.DeprecatedApi;

@Controller
public class LegacyController extends AbstractController {

	public static final Logger LOGGER = LoggerFactory.getLogger(LegacyController.class);

	@Autowired
	private IQuestionService questionService;

	/* specific routes */

	@DeprecatedApi
	@RequestMapping(value = "/session/mysessions", method = RequestMethod.GET)
	public final String redirectSessionMy() {
		return "forward:/session/?ownedonly=true";
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/visitedsessions", method = RequestMethod.GET)
	public final String redirectSessionVisited() {
		return "forward:/session/?visitedonly=true";
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/question")
	public final String redirectQuestionByLecturer(@PathVariable final String sessionKey) {
		return String.format("forward:/lecturerquestion/?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/skillquestions", method = RequestMethod.GET)
	public final String redirectQuestionByLecturerList(@PathVariable final String sessionKey) {
		return String.format("forward:/lecturerquestion/?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/skillquestioncount", method = RequestMethod.GET)
	public final String redirectQuestionByLecturerCount(@PathVariable final String sessionKey) {
		return String.format("forward:/lecturerquestion/count?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/answercount", method = RequestMethod.GET)
	public final String redirectQuestionByLecturerAnswerCount(@PathVariable final String sessionKey) {
		return String.format("forward:/lecturerquestion/answercount?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/unanswered", method = RequestMethod.GET)
	public final String redirectQuestionByLecturerUnnsweredCount(@PathVariable final String sessionKey) {
		return String.format("forward:/lecturerquestion/answercount?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/myanswers", method = RequestMethod.GET)
	public final String redirectQuestionByLecturerMyAnswers(@PathVariable final String sessionKey) {
		return String.format("forward:/lecturerquestion/myanswers?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/interposed")
	public final String redirectQuestionByAudience(@PathVariable final String sessionKey) {
		return String.format("forward:/audiencequestion/?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/interposed", method = RequestMethod.DELETE)
	@ResponseBody
	public final void deleteAllInterposedQuestions(@PathVariable final String sessionKey) {
		questionService.deleteAllInterposedQuestions(sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/interposedcount", method = RequestMethod.GET)
	public final String redirectQuestionByAudienceCount(@PathVariable final String sessionKey) {
		return String.format("forward:/audiencequestion/count?sessionkey=%s", sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/interposedreadingcount", method = RequestMethod.GET)
	public final String redirectQuestionByAudienceReadCount(@PathVariable final String sessionKey) {
		return String.format("forward:/audiencequestion/readcount?sessionkey=%s", sessionKey);
	}

	/* generalized routes */

	@DeprecatedApi
	@RequestMapping(value = { "/session/{sessionKey}/question/{arg1}", "/session/{sessionKey}/questions/{arg1}" })
	public final String redirectQuestionByLecturerWithOneArgument(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1
			) {
		return String.format("forward:/lecturerquestion/%s/?sessionkey=%s", arg1, sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(
			value = { "/session/{sessionKey}/question/{arg1}/{arg2}", "/session/{sessionKey}/questions/{arg1}/{arg2}" }
			)
	public final String redirectQuestionByLecturerWithTwoArguments(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			@PathVariable final String arg2
			) {
		return String.format("forward:/lecturerquestion/%s/%s/?sessionkey=%s", arg1, arg2, sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/interposed/{arg1}")
	public final String redirectQuestionByAudienceWithOneArgument(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1
			) {
		return String.format("forward:/audiencequestion/%s/?sessionkey=%s", arg1, sessionKey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/session/{sessionKey}/interposed/{arg1}/{arg2}")
	public final String redirectQuestionByAudienceWithTwoArguments(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			@PathVariable final String arg2
			) {
		return String.format("forward:/audiencequestion/%s/%s/?sessionkey=%s", arg1, arg2, sessionKey);
	}
}
