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

import java.util.List;

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

import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.services.IQuestionService;

@Controller
public class QuestionByAudienceController extends AbstractController {

	public static final Logger LOGGER = LoggerFactory.getLogger(QuestionByAudienceController.class);

	@Autowired
	private IQuestionService questionService;

	@RequestMapping(value = "/session/{sessionKey}/interposedcount", method = RequestMethod.GET)
	@ResponseBody
	public final int getInterposedCount(
			@PathVariable final String sessionKey,
			final HttpServletResponse response
	) {
		return questionService.getInterposedCount(sessionKey);
	}

	@RequestMapping(value = "/session/{sessionKey}/interposedreadingcount", method = RequestMethod.GET)
	@ResponseBody
	public final InterposedReadingCount getUnreadInterposedCount(
			@PathVariable final String sessionKey,
			final HttpServletResponse response
	) {
		return questionService.getInterposedReadingCount(sessionKey);
	}

	@RequestMapping(value = "/session/{sessionKey}/interposed", method = RequestMethod.GET)
	@ResponseBody
	public final List<InterposedQuestion> getInterposedQuestions(
			@PathVariable final String sessionKey,
			final HttpServletResponse response
	) {
		return questionService.getInterposedQuestions(sessionKey);
	}

	@RequestMapping(value = "/session/{sessionKey}/interposed/{questionId}", method = RequestMethod.GET)
	@ResponseBody
	public final InterposedQuestion getInterposedQuestions(
			@PathVariable final String sessionKey,
			@PathVariable final String questionId,
			final HttpServletResponse response
	) {
		return questionService.readInterposedQuestion(sessionKey, questionId);
	}

	@RequestMapping(value = "/session/{sessionkey}/interposed", method = RequestMethod.POST)
	@ResponseBody
	public final void postInterposedQuestion(
			@PathVariable final String sessionkey,
			@RequestBody final InterposedQuestion question,
			final HttpServletResponse response
	) {
		if (!sessionkey.equals(question.getSessionId())) {
			response.setStatus(HttpStatus.PRECONDITION_FAILED.value());
			return;
		}

		if (questionService.saveQuestion(question)) {
			response.setStatus(HttpStatus.CREATED.value());
			return;
		}

		response.setStatus(HttpStatus.BAD_REQUEST.value());
		return;
	}
	
	@RequestMapping(value = "/session/{sessionkey}/interposed/{questionId}", method = RequestMethod.DELETE)
	@ResponseBody
	public final void deleteInterposedQuestion(
			@PathVariable final String sessionkey,
			@PathVariable final String questionId,
			final HttpServletResponse response
	) {
		questionService.deleteQuestion(sessionkey, questionId);
	}

}
