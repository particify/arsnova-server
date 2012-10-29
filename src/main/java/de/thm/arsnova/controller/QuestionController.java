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

import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.services.IQuestionService;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Controller
public class QuestionController extends AbstractController {

	public static final Logger logger = LoggerFactory
			.getLogger(QuestionController.class);

	@Autowired
	IQuestionService questionService;

	@Autowired
	IUserService userService;

	@Autowired
	ARSnovaSocketIOServer server;

	@RequestMapping(value = "/session/{sessionkey}/question/{questionId}", method = RequestMethod.GET)
	@ResponseBody
	public Question getQuestion(@PathVariable String sessionkey,
			@PathVariable String questionId, HttpServletResponse response) {
		Question question = questionService.getQuestion(questionId, sessionkey);
		if (question != null) {
			return question;
		}

		response.setStatus(HttpStatus.NOT_FOUND.value());
		return null;
	}

	@RequestMapping(value = "/session/{sessionkey}/question", method = RequestMethod.POST)
	@ResponseBody
	public void postQuestion(@PathVariable String sessionkey,
			@RequestBody Question question, HttpServletResponse response) {
		if (!sessionkey.equals(question.getSession())) {
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

	@RequestMapping(value = { "/getSkillQuestions/{sessionkey}",
			"/session/{sessionkey}/skillquestions" }, method = RequestMethod.GET)
	@ResponseBody
	public List<Question> getSkillQuestions(@PathVariable String sessionkey,
			HttpServletResponse response) {
		List<Question> questions = questionService
				.getSkillQuestions(sessionkey);
		if (questions == null || questions.isEmpty()) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		logger.info(questions.toString());
		return questions;
	}
	
	@RequestMapping(value="/session/{sessionKey}/questionids", method=RequestMethod.GET)
	@ResponseBody
	public List<String> getQuestionIds(@PathVariable String sessionKey, HttpServletResponse response) {
		List<String> questions = questionService.getQuestionIds(sessionKey);
		if(questions == null || questions.isEmpty()) {
			throw new NotFoundException();
		}
		logger.info(questions.toString());
		return questions;		
	}
	
	@RequestMapping(value="/session/{sessionKey}/questions/{questionId}", method=RequestMethod.DELETE)
	@ResponseBody
	public void deleteAnswersAndQuestion(@PathVariable String sessionKey, @PathVariable String questionId, HttpServletResponse response) {
		questionService.deleteQuestion(sessionKey, questionId);
	}
	
	@RequestMapping(value="/session/{sessionKey}/questions/unanswered", method=RequestMethod.GET)
	@ResponseBody
	public List<String> getUnAnsweredSkillQuestions(@PathVariable String sessionKey, HttpServletResponse response) {
		List<String> answers = questionService.getUnAnsweredQuestions(sessionKey);
		if(answers == null || answers.isEmpty()) {
			throw new NotFoundException();
		}
		logger.info(answers.toString());
		return answers;
	}
	
	/**
	 * returns a JSON document which represents the given answer of a question.
	 * @param sessionKey
	 *            Session Keyword to which the question belongs to
	 * @param questionId
	 *            CouchDB Question ID for which the given answer should be
	 *            retrieved
	 * @return JSON Document of {@link Answer} or {@link NotFoundException} 
	 * @throws NotFoundException if
	 *         wrong session, wrong question or no answer was given by the
	 *         current user
	 * @throws ForbiddenException if not logged in
	 */
	@RequestMapping(value="/session/{sessionKey}/question/{questionId}/myanswer", method=RequestMethod.GET)
	@ResponseBody
	public Answer getMyAnswer(@PathVariable String sessionKey, @PathVariable String questionId, HttpServletResponse response) {
		Answer answer = questionService.getMyAnswer(sessionKey, questionId);
		if(answer == null) {
			throw new NotFoundException();
		}
		return answer;
	}
}
