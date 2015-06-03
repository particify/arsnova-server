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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.transport.InterposedQuestion;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.services.IQuestionService;
import de.thm.arsnova.web.DeprecatedApi;
import de.thm.arsnova.web.Pagination;

/**
 * Handles requests related to audience questions, which are also called interposed or feedback questions.
 */
@RestController
@RequestMapping("/audiencequestion")
public class AudienceQuestionController extends PaginationController {

	public static final Logger LOGGER = LoggerFactory.getLogger(AudienceQuestionController.class);

	@Autowired
	private IQuestionService questionService;

	@RequestMapping(value = "/count", method = RequestMethod.GET)
	@DeprecatedApi
	public int getInterposedCount(@RequestParam final String sessionkey) {
		return questionService.getInterposedCount(sessionkey);
	}

	@RequestMapping(value = "/readcount", method = RequestMethod.GET)
	@DeprecatedApi
	public InterposedReadingCount getUnreadInterposedCount(@RequestParam("sessionkey") final String sessionkey, String user) {
		return questionService.getInterposedReadingCount(sessionkey, user);
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	@Pagination
	public List<InterposedQuestion> getInterposedQuestions(@RequestParam final String sessionkey) {
		return InterposedQuestion.fromList(questionService.getInterposedQuestions(sessionkey, offset, limit));
	}

	@RequestMapping(value = "/{questionId}", method = RequestMethod.GET)
	public InterposedQuestion getInterposedQuestion(@PathVariable final String questionId) {
		return new InterposedQuestion(questionService.readInterposedQuestion(questionId));
	}

	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void postInterposedQuestion(
			@RequestParam final String sessionkey,
			@RequestBody final de.thm.arsnova.entities.InterposedQuestion question
			) {
		if (questionService.saveQuestion(question)) {
			return;
		}

		throw new BadRequestException();
	}

	@RequestMapping(value = "/{questionId}", method = RequestMethod.DELETE)
	public void deleteInterposedQuestion(@PathVariable final String questionId) {
		questionService.deleteInterposedQuestion(questionId);
	}
}
