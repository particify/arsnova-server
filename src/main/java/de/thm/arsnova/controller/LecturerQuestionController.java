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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

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

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import de.thm.arsnova.PaginationListDecorator;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.services.IQuestionService;
import de.thm.arsnova.web.DeprecatedApi;
import de.thm.arsnova.web.Pagination;

/**
 * Handles requests related to questions teachers are asking their students.
 */
@RestController
@RequestMapping("/lecturerquestion")
@Api(value = "/lecturerquestion", description = "Operations for Lecture Questions")
public class LecturerQuestionController extends PaginationController {

	public static final Logger LOGGER = LoggerFactory.getLogger(LecturerQuestionController.class);

	@Autowired
	private IQuestionService questionService;
	
	@ApiOperation(value = "Get question with provided question Id",
			nickname = "getQuestion",
			notes = "getQuestion(@PathVariable final String questionId)"
	)
	@ApiResponses(value = {
		@ApiResponse(code = 404, message = "Not Found - The requested resource could not be found but may be available again in the future.")
	})
	@RequestMapping(value = "/{questionId}", method = RequestMethod.GET)
	public Question getQuestion(@PathVariable final String questionId) {
		final Question question = questionService.getQuestion(questionId);
		if (question != null) {
			return question;
		}

		throw new NotFoundException();
	}

	@ApiOperation(value = "Post provided question",
			nickname = "postQuestion",
			notes = "postQuestion(@RequestBody final Question question)"
	)
	@ApiResponses(value = {
		@ApiResponse(code = 400, message = "Bad Request - The Api cannot or will not process the request due to something that is perceived to be a client error")
	})
	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Question postQuestion(@RequestBody final Question question) {
		if (questionService.saveQuestion(question) != null) {
			return question;
		}
		throw new BadRequestException();
	}

	@ApiOperation(value = "Update the question, identified by provided id, with the provided question in the Request Body",
			nickname = "updateQuestion",
			notes = "updateQuestion(@PathVariable final String questionId,@RequestBody final Question question)"
	)
	@ApiResponses(value = {
		@ApiResponse(code = 400, message = "Bad Request - The Api cannot or will not process the request due to something that is perceived to be a client error")
	})
	@RequestMapping(value = "/{questionId}", method = RequestMethod.PUT)
	public Question updateQuestion(
			@PathVariable final String questionId,
			@RequestBody final Question question
			) {
		try {
			return questionService.update(question);
		} catch (final Exception e) {
			throw new BadRequestException();
		}
	}

	@ApiOperation(value = "Start new Pi Round on question, identified by provided id, with an optional time",
			nickname = "startPiRound",
			notes = "startPiRound(@PathVariable final String questionId," +
 				"@RequestParam(value = \"time\", defaultValue = \"0\", required = false) final int time)"
	)
	@RequestMapping(value = "/{questionId}/questionimage", method = RequestMethod.GET)
	public String getQuestionImage(
			@PathVariable final String questionId,
			@RequestParam(value = "fcImage", defaultValue = "false", required = false) final boolean fcImage
			) {

		if (fcImage) {
			return questionService.getQuestionFcImage(questionId);
		} else {
			return questionService.getQuestionImage(questionId);
		}
	}

	@RequestMapping(value = "/{questionId}/startnewpiround", method = RequestMethod.POST)
	public void startPiRound(
			@PathVariable final String questionId,
			@RequestParam(value = "time", defaultValue = "0", required = false) final int time
			) {

		if(time == 0) {
			questionService.startNewPiRound(questionId, null);
		} else {
			questionService.startNewPiRoundDelayed(questionId, time);
		}
	}

	@RequestMapping(value = "/{questionId}/canceldelayedpiround", method = RequestMethod.POST)
	@ApiOperation(value = "Cancel Pi Round on question, identified by provided id",
			nickname = "cancelPiRound",
			notes = "cancelPiRound(@PathVariable final String questionId)"
	)
	public void cancelPiRound(
			@PathVariable final String questionId
			) {
		questionService.cancelPiRoundChange(questionId);
	}

	@RequestMapping(value = "/{questionId}/resetpiroundstate", method = RequestMethod.POST)
	@ApiOperation(value = "Reset Pi Round on question, identified by provided id",
			nickname = "resetPiQuestion",
			notes = "resetPiQuestion(@PathVariable final String questionId)"
	)
	public void resetPiQuestion(
			@PathVariable final String questionId
			) {
		questionService.resetPiRoundState(questionId);
	}

	@ApiOperation(value = "Set voting admission on question, identified by provided id",
			nickname = "setVotingAdmission",
			notes = "setVotingAdmission(@PathVariable final String questionId," +
				"@RequestParam(value = \"disable\", defaultValue = \"false\", required = false) final Boolean disableVote)"
	)
	@RequestMapping(value = "/{questionId}/disablevote", method = RequestMethod.POST)
	public void setVotingAdmission(
			@PathVariable final String questionId,
			@RequestParam(value = "disable", defaultValue = "false", required = false) final Boolean disableVote
			) {
		boolean disable = false;

		if (disableVote != null) {
			disable = disableVote;
		}

		questionService.setVotingAdmission(questionId, disable);
	}

	@ApiOperation(value = "Set voting admission for all questions",
			nickname = "setVotingAdmissionForAllQuestions",
			notes = "setVotingAdmissionForAllQuestions(" +
				"@RequestParam final String sessionkey," +
				"@RequestParam(value = \"disable\", defaultValue = \"false\", required = false) final Boolean disableVote," +
				"@RequestParam(value = \"lecturequestionsonly\", defaultValue = \"false\", required = false) final boolean lectureQuestionsOnly," +
				"@RequestParam(value = \"preparationquestionsonly\", defaultValue = \"false\", required = false) final boolean preparationQuestionsOnly)"
	)
	@RequestMapping(value = "/disablevote", method = RequestMethod.POST)
	public void setVotingAdmissionForAllQuestions(
			@RequestParam final String sessionkey,
			@RequestParam(value = "disable", defaultValue = "false", required = false) final Boolean disableVote,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false", required = false) final boolean lectureQuestionsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false", required = false) final boolean preparationQuestionsOnly
			) {
		boolean disable = false;
		List<Question> questions;

		if (disableVote != null) {
			disable = disableVote;
		}

		if (lectureQuestionsOnly) {
			questions = questionService.getLectureQuestions(sessionkey);
			questionService.setVotingAdmissions(sessionkey, disable, questions);
		} else if (preparationQuestionsOnly) {
			questions = questionService.getPreparationQuestions(sessionkey);
			questionService.setVotingAdmissions(sessionkey, disable, questions);
		} else {
			questionService.setVotingAdmissionForAllQuestions(sessionkey, disable);
		}
	}

	@ApiOperation(value = "Publish a question, identified by provided id and question in Request Body.",
			nickname = "publishQuestion",
			notes = "publishQuestion(" +
				"@PathVariable final String questionId," +
				"@RequestParam(required = false) final Boolean publish," +
				"@RequestBody final Question question)"
	)
	@RequestMapping(value = "/{questionId}/publish", method = RequestMethod.POST)
	public void publishQuestion(
			@PathVariable final String questionId,
			@RequestParam(required = false) final Boolean publish,
			@RequestBody final Question question
			) {
		if (publish != null) {
			question.setActive(publish);
		}
		questionService.update(question);
	}

	@ApiOperation(value = "Publish all questions",
			nickname = "publishAllQuestions",
			notes = "publishAllQuestions(" +
				"@RequestParam final String sessionkey," +
				"@RequestParam(required = false) final Boolean publish," +
				"@RequestParam(value = \"lecturequestionsonly\", defaultValue = \"false\", required = false) final boolean lectureQuestionsOnly," +
				"@RequestParam(value = \"preparationquestionsonly\", defaultValue = \"false\", required = false) final boolean preparationQuestionsOnly)"
	)
	@RequestMapping(value = "/publish", method = RequestMethod.POST)
	public void publishAllQuestions(
			@RequestParam final String sessionkey,
			@RequestParam(required = false) final Boolean publish,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false", required = false) final boolean lectureQuestionsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false", required = false) final boolean preparationQuestionsOnly
			) {
		boolean p = true;
		List<Question> questions;

		if (publish != null) {
			p = publish;
		}

		if (lectureQuestionsOnly) {
			questions = questionService.getLectureQuestions(sessionkey);
			questionService.publishQuestions(sessionkey, publish, questions);
		} else if (preparationQuestionsOnly) {
			questions = questionService.getPreparationQuestions(sessionkey);
			questionService.publishQuestions(sessionkey, publish, questions);
		} else {
			questionService.publishAll(sessionkey, p);
		}
	}

	@ApiOperation(value = "Publish statistics from question with provided id",
			nickname = "publishStatistics",
			notes = "publishStatistics(" +
				"@PathVariable final String questionId," +
				"@RequestParam(required = false) final Boolean showStatistics," +
				"@RequestBody final Question question)"
	)
	@RequestMapping(value = "/{questionId}/publishstatistics", method = RequestMethod.POST)
	public void publishStatistics(
			@PathVariable final String questionId,
			@RequestParam(required = false) final Boolean showStatistics,
			@RequestBody final Question question
			) {
		if (showStatistics != null) {
			question.setShowStatistic(showStatistics);
		}
		questionService.update(question);
	}

	@ApiOperation(value = "Publish correct answer from question with provided id",
			nickname = "publishCorrectAnswer",
			notes = "publishCorrectAnswer(" +
				"@PathVariable final String questionId," +
				"@RequestParam(required = false) final Boolean showCorrectAnswer," +
				"@RequestBody final Question question)"
	)
	@RequestMapping(value = "/{questionId}/publishcorrectanswer", method = RequestMethod.POST)
	public void publishCorrectAnswer(
			@PathVariable final String questionId,
			@RequestParam(required = false) final Boolean showCorrectAnswer,
			@RequestBody final Question question
			) {
		if (showCorrectAnswer != null) {
			question.setShowAnswer(showCorrectAnswer);
		}
		questionService.update(question);
	}

	@ApiOperation(value = "Get skill questions",
			nickname = "getSkillQuestions",
			notes = "getSkillQuestions(" +
				"@RequestParam final String sessionkey," +
				"@RequestParam(value = \"lecturequestionsonly\", defaultValue = \"false\") final boolean lectureQuestionsOnly," +
				"@RequestParam(value = \"flashcardsonly\", defaultValue = \"false\") final boolean flashcardsOnly," +
				"@RequestParam(value = \"preparationquestionsonly\", defaultValue = \"false\") final boolean preparationQuestionsOnly," +
				"final HttpServletResponse response)"
	)
	@RequestMapping(value = "/", method = RequestMethod.GET)
	@Pagination
	public List<Question> getSkillQuestions(
			@RequestParam final String sessionkey,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") final boolean lectureQuestionsOnly,
			@RequestParam(value = "flashcardsonly", defaultValue = "false") final boolean flashcardsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") final boolean preparationQuestionsOnly,
			@RequestParam(value = "requestImageData", defaultValue = "false") final boolean requestImageData,
			final HttpServletResponse response
			) {
		List<Question> questions;
		if (lectureQuestionsOnly) {
			questions = questionService.getLectureQuestions(sessionkey);
		} else if (flashcardsOnly) {
			questions = questionService.getFlashcards(sessionkey);
		} else if (preparationQuestionsOnly) {
			questions = questionService.getPreparationQuestions(sessionkey);
		} else {
			questions = questionService.getSkillQuestions(sessionkey);
		}
		if (questions == null || questions.isEmpty()) {
			response.setStatus(HttpStatus.NO_CONTENT.value());
			return null;
		} else if (!requestImageData) {
			questions = questionService.replaceImageData(questions);
		}

		return new PaginationListDecorator<Question>(questions, offset, limit);
	}

	@ApiOperation(value = "Delete skill questions",
			nickname = "deleteSkillQuestions",
			notes = "deleteSkillQuestions(" +
				"@RequestParam final String sessionkey," +
				"@RequestParam(value = \"lecturequestionsonly\", defaultValue = \"false\") final boolean lectureQuestionsOnly," +
				"@RequestParam(value = \"flashcardsonly\", defaultValue = \"false\") final boolean flashcardsOnly," +
				"@RequestParam(value = \"preparationquestionsonly\", defaultValue = \"false\") final boolean preparationQuestionsOnly," +
				"final HttpServletResponse response)"
	)
	@RequestMapping(value = { "/" }, method = RequestMethod.DELETE)
	public void deleteSkillQuestions(
			@RequestParam final String sessionkey,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") final boolean lectureQuestionsOnly,
			@RequestParam(value = "flashcardsonly", defaultValue = "false") final boolean flashcardsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") final boolean preparationQuestionsOnly,
			final HttpServletResponse response
			) {
		if (lectureQuestionsOnly) {
			questionService.deleteLectureQuestions(sessionkey);
		} else if (flashcardsOnly) {
			questionService.deleteFlashcards(sessionkey);
		} else if (preparationQuestionsOnly) {
			questionService.deletePreparationQuestions(sessionkey);
		} else {
			questionService.deleteAllQuestions(sessionkey);
		}
	}

	@ApiOperation(hidden=true, value="")
	@DeprecatedApi
	@RequestMapping(value = "/count", method = RequestMethod.GET)
	public int getSkillQuestionCount(
			@RequestParam final String sessionkey,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") final boolean lectureQuestionsOnly,
			@RequestParam(value = "flashcardsonly", defaultValue = "false") final boolean flashcardsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") final boolean preparationQuestionsOnly
			) {
		if (lectureQuestionsOnly) {
			return questionService.getLectureQuestionCount(sessionkey);
		} else if (flashcardsOnly) {
			return questionService.getFlashcardCount(sessionkey);
		} else if (preparationQuestionsOnly) {
			return questionService.getPreparationQuestionCount(sessionkey);
		} else {
			return questionService.getSkillQuestionCount(sessionkey);
		}
	}

	@ApiOperation(value = "Delete answers and questions",
			nickname = "deleteAnswersAndQuestion",
			notes = "deleteAnswersAndQuestion(" +
				"@PathVariable final String questionId)"
	)
	@RequestMapping(value = "/{questionId}", method = RequestMethod.DELETE)
	public void deleteAnswersAndQuestion(
			@PathVariable final String questionId
			) {
		questionService.deleteQuestion(questionId);
	}

	@ApiOperation(hidden=true, value="")
	@DeprecatedApi
	@RequestMapping(value = "/unanswered", method = RequestMethod.GET)
	public List<String> getUnAnsweredSkillQuestionIds(
			@RequestParam final String sessionkey,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") final boolean lectureQuestionsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") final boolean preparationQuestionsOnly
			) {
		List<String> answers;
		if (lectureQuestionsOnly) {
			answers = questionService.getUnAnsweredLectureQuestionIds(sessionkey);
		} else if (preparationQuestionsOnly) {
			answers = questionService.getUnAnsweredPreparationQuestionIds(sessionkey);
		} else {
			answers = questionService.getUnAnsweredQuestionIds(sessionkey);
		}
		if (answers == null || answers.isEmpty()) {
			throw new NoContentException();
		}

		return answers;
	}

	/**
	 * returns a JSON document which represents the given answer of a question.
	 *
	 * @param sessionKey
	 *            Session Keyword to which the question belongs to
	 * @param questionId
	 *            CouchDB Question ID for which the given answer should be
	 *            retrieved
	 * @return JSON Document of {@link Answer} or {@link NotFoundException}
	 * @throws NotFoundException
	 *             if wrong session, wrong question or no answer was given by
	 *             the current user
	 * @throws ForbiddenException
	 *             if not logged in
	 */
	@ApiOperation(hidden=true, value="")
	@DeprecatedApi
	@RequestMapping(value = "/{questionId}/myanswer", method = RequestMethod.GET)
	public Answer getMyAnswer(
			@PathVariable final String questionId,
			final HttpServletResponse response
			) {
		final Answer answer = questionService.getMyAnswer(questionId);
		if (answer == null) {
			response.setStatus(HttpStatus.NO_CONTENT.value());
			return null;
		}

		return answer;
	}

	/**
	 * returns a list of {@link Answer}s encoded as a JSON document for a given
	 * question id. In this case only {@link Answer} <tt>questionId</tt>,
	 * <tt>answerText</tt>, <tt>answerSubject</tt> and <tt>answerCount</tt>
	 * properties are set
	 *
	 * @param sessionKey
	 *            Session Keyword to which the question belongs to
	 * @param questionId
	 *            CouchDB Question ID for which the given answers should be
	 *            retrieved
	 * @return List<{@link Answer}> or {@link NotFoundException}
	 * @throws NotFoundException
	 *             if wrong session, wrong question or no answers was given
	 * @throws ForbiddenException
	 *             if not logged in
	 */
	@ApiOperation(value = "Get answers for a question, identified by provided question ID",
			nickname = "getAnswers",
			notes = "getAnswers(" +
				"@PathVariable final String questionId," +
				"@RequestParam(value = \"piround\", required = false) final Integer piRound," +
				"@RequestParam(value = \"all\", required = false, defaultValue = \"false\") final Boolean allAnswers," +
				"final HttpServletResponse response)"
	)
	@RequestMapping(value = "/{questionId}/answer/", method = RequestMethod.GET)
	public List<Answer> getAnswers(
			@PathVariable final String questionId,
			@RequestParam(value = "piround", required = false) final Integer piRound,
			@RequestParam(value = "all", required = false, defaultValue = "false") final Boolean allAnswers,
			final HttpServletResponse response
			) {
		List<Answer> answers = null;
		if (allAnswers) {
			answers = questionService.getAllAnswers(questionId, -1, -1);
		} else if (null == piRound) {
			answers = questionService.getAnswers(questionId, offset, limit);
		} else {
			if (piRound < 1 || piRound > 2) {
				response.setStatus(HttpStatus.BAD_REQUEST.value());

				return null;
			}
			answers = questionService.getAnswers(questionId, piRound, offset, limit);
		}
		if (answers == null) {
			return new ArrayList<Answer>();
		}
		return answers;
	}

	@ApiOperation(value = "Save answer, provided in the Request Body, for a question, identified by provided question ID",
			nickname = "saveAnswer",
			notes = "saveAnswer(" +
				"@PathVariable final String questionId," +
				"@RequestBody final de.thm.arsnova.entities.transport.Answer answer," +
				"final HttpServletResponse response)"
	)
	@RequestMapping(value = "/{questionId}/answer/", method = RequestMethod.POST)
	public Answer saveAnswer(
			@PathVariable final String questionId,
			@RequestBody final de.thm.arsnova.entities.transport.Answer answer,
			final HttpServletResponse response
			) {
		return questionService.saveAnswer(questionId, answer);
	}

	@ApiOperation(value = "Update answer, provided in Request Body, identified by question ID and answer ID",
			nickname = "updateAnswer",
			notes = "updateAnswer(" +
				"@PathVariable final String questionId," +
				"@PathVariable final String answerId," +
				"@RequestBody final Answer answer," +
				"final HttpServletResponse response)"
	)
	@RequestMapping(value = "/{questionId}/answer/{answerId}", method = RequestMethod.PUT)
	public Answer updateAnswer(
			@PathVariable final String questionId,
			@PathVariable final String answerId,
			@RequestBody final Answer answer,
			final HttpServletResponse response
			) {
		return questionService.updateAnswer(answer);
	}

	@ApiOperation(value = "Get Image, identified by question ID and answer ID",
			nickname = "getImage",
			notes = "getImage(" +
				"@PathVariable final String questionId," +
				"@PathVariable final String answerId," +
				"final HttpServletResponse response)"
	)
	@RequestMapping(value = "/{questionId}/answer/{answerId}/image", method = RequestMethod.GET)
	public String getImage(
			@PathVariable final String questionId,
			@PathVariable final String answerId,
			final HttpServletResponse response
			) {

		return questionService.getImage(questionId, answerId);
	}

	@ApiOperation(value = "Delete answer, identified by question ID and answer ID",
			nickname = "deleteAnswer",
			notes = "deleteAnswer(" +
				"@PathVariable final String questionId," +
				"@PathVariable final String answerId," +
				"final HttpServletResponse response)"
	)
	@RequestMapping(value = "/{questionId}/answer/{answerId}", method = RequestMethod.DELETE)
	public void deleteAnswer(
			@PathVariable final String questionId,
			@PathVariable final String answerId,
			final HttpServletResponse response
			) {
		questionService.deleteAnswer(questionId, answerId);
	}

	@ApiOperation(value = "Delete answers from a question, identified by question ID",
			nickname = "deleteAnswers",
			notes = "deleteAnswers(" +
				"@PathVariable final String questionId," +
				"final HttpServletResponse response)"
	)
	@RequestMapping(value = "/{questionId}/answer/", method = RequestMethod.DELETE)
	public void deleteAnswers(
			@PathVariable final String questionId,
			final HttpServletResponse response
			) {
		questionService.deleteAnswers(questionId);
	}

	@ApiOperation(value = "Delete all answers and questions from a session, identified by sessionkey",
			nickname = "deleteAllQuestionsAnswers",
			notes = "deleteAllQuestionsAnswers(" +
				"@RequestParam final String sessionkey," +
				"@RequestParam(value = \"lecturequestionsonly\", defaultValue = \"false\") final boolean lectureQuestionsOnly," +
				"@RequestParam(value = \"preparationquestionsonly\", defaultValue = \"false\") final boolean preparationQuestionsOnly," +
				"final HttpServletResponse response)"
	)
	@RequestMapping(value = "/answers", method = RequestMethod.DELETE)
	public void deleteAllQuestionsAnswers(
			@RequestParam final String sessionkey,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") final boolean lectureQuestionsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") final boolean preparationQuestionsOnly,
			final HttpServletResponse response
			) {
		if (lectureQuestionsOnly) {
			questionService.deleteAllLectureAnswers(sessionkey);
		} else if (preparationQuestionsOnly) {
			questionService.deleteAllPreparationAnswers(sessionkey);
		} else {
			questionService.deleteAllQuestionsAnswers(sessionkey);
		}
	}

	/**
	 *
	 * @param sessionKey
	 *            Session Keyword to which the question belongs to
	 * @param questionId
	 *            CouchDB Question ID for which the given answers should be
	 *            retrieved
	 * @return count of answers for given question id
	 * @throws NotFoundException
	 *             if wrong session or wrong question
	 * @throws ForbiddenException
	 *             if not logged in
	 */
	@ApiOperation(hidden=true, value="")
	@DeprecatedApi
	@RequestMapping(value = "/{questionId}/answercount", method = RequestMethod.GET)
	public int getAnswerCount(@PathVariable final String questionId) {
		return questionService.getAnswerCount(questionId);
	}

	@ApiOperation(value = "Get the amount of answers for a question, identified by the question ID",
			nickname = "getAllAnswerCount",
			notes = "getAllAnswerCount(@PathVariable final String questionId)"
	)
	@RequestMapping(value = "/{questionId}/allroundanswercount", method = RequestMethod.GET)
	public List<Integer> getAllAnswerCount(@PathVariable final String questionId) {
		return Arrays.asList(
			questionService.getAnswerCount(questionId, 1),
			questionService.getAnswerCount(questionId, 2)
		);
	}

	@ApiOperation(value = "Get the total amount of answers by a question, identified by the question ID",
			nickname = "getTotalAnswerCountByQuestion",
			notes = "getTotalAnswerCountByQuestion(@PathVariable final String questionId)"
	)
	@RequestMapping(value = "/{questionId}/totalanswercount", method = RequestMethod.GET)
	public int getTotalAnswerCountByQuestion(@PathVariable final String questionId) {
		return questionService.getTotalAnswerCountByQuestion(questionId);
	}

	@ApiOperation(value = "Get the amount of answers and abstention answers by a question, identified by the question ID",
			nickname = "getAnswerAndAbstentionCount",
			notes = "getAnswerAndAbstentionCount(@PathVariable final String questionId)"
	)
	@RequestMapping(value = "/{questionId}/answerandabstentioncount", method = RequestMethod.GET)
	public List<Integer> getAnswerAndAbstentionCount(@PathVariable final String questionId) {
		List<Integer> list = Arrays.asList(
			questionService.getAnswerCount(questionId),
			questionService.getAbstentionAnswerCount(questionId)
		);

		return list;
	}

	@ApiOperation(value = "Get all Freetext answers by a question, identified by the question ID",
			nickname = "getFreetextAnswers",
			notes = "getFreetextAnswers(@PathVariable final String questionId)"
	)
	@RequestMapping(value = "/{questionId}/freetextanswer/", method = RequestMethod.GET)
	@Pagination
	public List<Answer> getFreetextAnswers(@PathVariable final String questionId) {
		return questionService.getFreetextAnswers(questionId, offset, limit);
	}

	@ApiOperation(hidden=true, value="")
	@DeprecatedApi
	@RequestMapping(value = "/myanswers", method = RequestMethod.GET)
	public List<Answer> getMyAnswers(@RequestParam final String sessionkey) {
		return questionService.getMyAnswers(sessionkey);
	}

	@ApiOperation(hidden=true, value="")
	@DeprecatedApi
	@RequestMapping(value = "/answercount", method = RequestMethod.GET)
	public int getTotalAnswerCount(
			@RequestParam final String sessionkey,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") final boolean lectureQuestionsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") final boolean preparationQuestionsOnly
			) {
		if (lectureQuestionsOnly) {
			return questionService.countLectureQuestionAnswers(sessionkey);
		} else if (preparationQuestionsOnly) {
			return questionService.countPreparationQuestionAnswers(sessionkey);
		} else {
			return questionService.getTotalAnswerCount(sessionkey);
		}
	}

	@ApiOperation(value = "Set the sort order of the subjects from a session, identified by the sessionkey",
			nickname = "setSubjectSortOrder",
			notes = "setSubjectSortOrder(" +
				"@RequestParam(required = true) final String sessionkey," +
            			"@RequestParam(required = true) final String sorttype," +
            			"@RequestParam(required = true) final String ispreparation," +
				"@RequestBody String[] sortOrder)"
	)
	@ApiResponses(value = {
		@ApiResponse(code = 400, message = "Bad Request - The Api cannot or will not process the request due to something that is perceived to be a client error")
	})
	@RequestMapping(value = "/subjectsort", method = RequestMethod.POST)
	public void setSubjectSortOrder(
			@RequestParam(required = true) final String sessionkey,
            		@RequestParam(required = true) final String sorttype,
            		@RequestParam(required = true) final String ispreparation,
			@RequestBody String[] sortOrder
			) {
		try {
			questionService.setSort(sessionkey, "", sorttype, ispreparation, sortOrder) ;
		} catch (final Exception e) {
			throw new BadRequestException();
		}
	}
	
	@ApiOperation(value = "Get the sort order of the subjects from a session, identified by the sessionkey",
			nickname = "getSubjectSortType",
			notes = "getSubjectSortType(" +
				"@RequestParam(required = true) final String sessionkey," +
				"@RequestParam(required = true) final String ispreparation)"
	)
	@RequestMapping(value = "/subjectsort", method = RequestMethod.GET)
	public String getSubjectSortType(
			@RequestParam(required = true) final String sessionkey,
			@RequestParam(required = true) final String ispreparation
			) {
		return questionService.getSubjectSortType(sessionkey, ispreparation);
	}

	@ApiOperation(value = "Set the sort order of the questions from a subject in a session, identified by the sessionkey",
			nickname = "setQuestionSortOrder",
			notes = "setQuestionSortOrder(" +
				"@RequestParam(required = true) final String sessionkey," +
				"@RequestParam(required = true) final String subject," +
				"@RequestParam(required = true) final String sorttype," +
				"@RequestParam(required = true) final String ispreparation," +
				"@RequestBody String[] sortOrder)"
	)
	@ApiResponses(value = {
		@ApiResponse(code = 400, message = "Bad Request - The Api cannot or will not process the request due to something that is perceived to be a client error")
	})
	@RequestMapping(value = "/questionsort", method = RequestMethod.POST)
	public void setQuestionSortOrder(
			@RequestParam(required = true) final String sessionkey,
			@RequestParam(required = true) final String subject,
			@RequestParam(required = true) final String sorttype,
			@RequestParam(required = true) final String ispreparation,
			@RequestBody String[] sortOrder
			) {
		try {
			questionService.setSort(sessionkey, subject, sorttype, ispreparation, sortOrder);
		} catch (final Exception e) {
			throw new BadRequestException();
		}
	}

	@ApiOperation(value = "Get the sort order of the questions from a subject in a session, identified by the sessionkey",
			nickname = "getQuestionSortType",
			notes = "getQuestionSortType(" +
				"@RequestParam(required = true) final String sessionkey," +
				"@RequestParam(required = true) final String subject," +
				"@RequestParam(required = true, defaultValue = \"false\") final boolean ispreparation)"
	)
	@ApiResponses(value = {
		@ApiResponse(code = 204, message = "No Content - successfully processed the request")
	})
	@RequestMapping(value = "/questionsort", method = RequestMethod.GET)
	public String getQuestionSortType(
			@RequestParam(required = true) final String sessionkey,
			@RequestParam(required = true) final String subject,
			@RequestParam(required = true, defaultValue = "false") final boolean ispreparation
			) {
		String sortType = questionService.getQuestionSortType(sessionkey, ispreparation, subject);
		if (sortType == null) {
			throw new NoContentException();
		}
		return sortType;
	}
}
