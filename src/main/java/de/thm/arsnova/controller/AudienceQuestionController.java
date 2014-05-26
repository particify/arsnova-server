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

import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.exceptions.PreconditionFailedException;
import de.thm.arsnova.services.IQuestionService;
import de.thm.arsnova.web.DeprecatedApi;

@RestController
@RequestMapping("/audiencequestion")
public class AudienceQuestionController extends AbstractController {

	public static final Logger LOGGER = LoggerFactory.getLogger(AudienceQuestionController.class);

	@Autowired
	private IQuestionService questionService;

	@RequestMapping(value = "/count", method = RequestMethod.GET)
	@DeprecatedApi
	public final int getInterposedCount(@RequestParam final String sessionkey) {
		return questionService.getInterposedCount(sessionkey);
	}

	@RequestMapping(value = "/readcount", method = RequestMethod.GET)
	@DeprecatedApi
	public final InterposedReadingCount getUnreadInterposedCount(@RequestParam("sessionkey") final String sessionkey) {
		return questionService.getInterposedReadingCount(sessionkey);
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public final List<InterposedQuestion> getInterposedQuestions(@RequestParam final String sessionkey) {
		return questionService.getInterposedQuestions(sessionkey);
	}

	@RequestMapping(value = "/{questionId}", method = RequestMethod.GET)
	public final InterposedQuestion getInterposedQuestion(@PathVariable final String questionId) {
		return questionService.readInterposedQuestion(questionId);
	}

	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public final void postInterposedQuestion(
			@RequestParam final String sessionkey,
			@RequestBody final InterposedQuestion question
			) {
		if (!sessionkey.equals(question.getSessionId())) {
			throw new PreconditionFailedException();
		}

		if (questionService.saveQuestion(question)) {
			return;
		}

		throw new BadRequestException();
	}

	@RequestMapping(value = "/{questionId}", method = RequestMethod.DELETE)
	public final void deleteInterposedQuestion(@PathVariable final String questionId) {
		questionService.deleteInterposedQuestion(questionId);
	}
}
