/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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

import de.thm.arsnova.PaginationListDecorator;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.Content;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.services.IContentService;
import de.thm.arsnova.web.DeprecatedApi;
import de.thm.arsnova.web.Pagination;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles requests related to questions teachers are asking their students.
 */
@RestController
@RequestMapping("/lecturerquestion")
@Api(value = "/lecturerquestion", description = "Operations for Lecture Questions")
public class ContentController extends PaginationController {
	@Autowired
	private IContentService contentService;

	@ApiOperation(value = "Get question with provided question Id",
			nickname = "getQuestion")
	@ApiResponses(value = {
		@ApiResponse(code = 404, message = HTML_STATUS_404)
	})
	@RequestMapping(value = "/{questionId}", method = RequestMethod.GET)
	public Content getQuestion(@PathVariable final String questionId) {
		final Content content = contentService.getQuestion(questionId);
		if (content != null) {
			return content;
		}

		throw new NotFoundException();
	}

	@ApiOperation(value = "Post provided content",
			nickname = "postQuestion")
	@ApiResponses(value = {
		@ApiResponse(code = 400, message = HTML_STATUS_400)
	})
	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Content postQuestion(@RequestBody final Content content) {
		if (contentService.saveQuestion(content) != null) {
			return content;
		}
		throw new BadRequestException();
	}

	@ApiOperation(value = "Post provided contents", nickname = "bulkPostQuestions")
	@ApiResponses(value = {
		@ApiResponse(code = 400, message = HTML_STATUS_400)
	})
	@RequestMapping(value = "/bulk", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public List<Content> bulkPostQuestions(@RequestBody final List<Content> contents) {
		for (final Content content : contents) {
			if (contentService.saveQuestion(content) == null) {
				throw new BadRequestException();
			}
		}
		return contents;
	}

	@ApiOperation(value = "Update the content, identified by provided id, with the provided content in the Request Body",
			nickname = "updateQuestion")
	@ApiResponses(value = {
		@ApiResponse(code = 400, message = HTML_STATUS_400)
	})
	@RequestMapping(value = "/{questionId}", method = RequestMethod.PUT)
	public Content updateQuestion(
			@PathVariable final String questionId,
			@RequestBody final Content content
			) {
		try {
			return contentService.update(content);
		} catch (final Exception e) {
			throw new BadRequestException();
		}
	}

	@ApiOperation(value = "Start new Pi Round on question, identified by provided id, with an optional time",
			nickname = "startPiRound")
	@RequestMapping(value = "/{questionId}/questionimage", method = RequestMethod.GET)
	public String getQuestionImage(
			@PathVariable final String questionId,
			@RequestParam(value = "fcImage", defaultValue = "false", required = false) final boolean fcImage
			) {

		if (fcImage) {
			return contentService.getQuestionFcImage(questionId);
		} else {
			return contentService.getQuestionImage(questionId);
		}
	}

	@RequestMapping(value = "/{questionId}/startnewpiround", method = RequestMethod.POST)
	public void startPiRound(
			@PathVariable final String questionId,
			@RequestParam(value = "time", defaultValue = "0", required = false) final int time
			) {

		if (time == 0) {
			contentService.startNewPiRound(questionId, null);
		} else {
			contentService.startNewPiRoundDelayed(questionId, time);
		}
	}

	@RequestMapping(value = "/{questionId}/canceldelayedpiround", method = RequestMethod.POST)
	@ApiOperation(value = "Cancel Pi Round on question, identified by provided id",
			nickname = "cancelPiRound")
	public void cancelPiRound(
			@PathVariable final String questionId
			) {
		contentService.cancelPiRoundChange(questionId);
	}

	@RequestMapping(value = "/{questionId}/resetpiroundstate", method = RequestMethod.POST)
	@ApiOperation(value = "Reset Pi Round on question, identified by provided id",
			nickname = "resetPiQuestion")
	public void resetPiQuestion(
			@PathVariable final String questionId
			) {
		contentService.resetPiRoundState(questionId);
	}

	@ApiOperation(value = "Set voting admission on question, identified by provided id",
			nickname = "setVotingAdmission")
	@RequestMapping(value = "/{questionId}/disablevote", method = RequestMethod.POST)
	public void setVotingAdmission(
			@PathVariable final String questionId,
			@RequestParam(value = "disable", defaultValue = "false", required = false) final Boolean disableVote
			) {
		boolean disable = false;

		if (disableVote != null) {
			disable = disableVote;
		}

		contentService.setVotingAdmission(questionId, disable);
	}

	@ApiOperation(value = "Set voting admission for all questions",
			nickname = "setVotingAdmissionForAllQuestions")
	@RequestMapping(value = "/disablevote", method = RequestMethod.POST)
	public void setVotingAdmissionForAllQuestions(
			@RequestParam final String sessionkey,
			@RequestParam(value = "disable", defaultValue = "false", required = false) final Boolean disableVote,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false", required = false) final boolean lectureQuestionsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false", required = false) final boolean preparationQuestionsOnly
			) {
		boolean disable = false;
		List<Content> contents;

		if (disableVote != null) {
			disable = disableVote;
		}

		if (lectureQuestionsOnly) {
			contents = contentService.getLectureQuestions(sessionkey);
			contentService.setVotingAdmissions(sessionkey, disable, contents);
		} else if (preparationQuestionsOnly) {
			contents = contentService.getPreparationQuestions(sessionkey);
			contentService.setVotingAdmissions(sessionkey, disable, contents);
		} else {
			contentService.setVotingAdmissionForAllQuestions(sessionkey, disable);
		}
	}

	@ApiOperation(value = "Publish a content, identified by provided id and content in Request Body.",
			nickname = "publishQuestion")
	@RequestMapping(value = "/{questionId}/publish", method = RequestMethod.POST)
	public void publishQuestion(
			@PathVariable final String questionId,
			@RequestParam(required = false) final Boolean publish,
			@RequestBody final Content content
			) {
		if (publish != null) {
			content.setActive(publish);
		}
		contentService.update(content);
	}

	@ApiOperation(value = "Publish all questions",
			nickname = "publishAllQuestions")
	@RequestMapping(value = "/publish", method = RequestMethod.POST)
	public void publishAllQuestions(
			@RequestParam final String sessionkey,
			@RequestParam(required = false) final Boolean publish,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false", required = false) final boolean lectureQuestionsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false", required = false) final boolean preparationQuestionsOnly
			) {
		boolean p = publish == null || publish;
		List<Content> contents;

		if (lectureQuestionsOnly) {
			contents = contentService.getLectureQuestions(sessionkey);
			contentService.publishQuestions(sessionkey, p, contents);
		} else if (preparationQuestionsOnly) {
			contents = contentService.getPreparationQuestions(sessionkey);
			contentService.publishQuestions(sessionkey, p, contents);
		} else {
			contentService.publishAll(sessionkey, p);
		}
	}

	@ApiOperation(value = "Publish statistics from content with provided id",
			nickname = "publishStatistics")
	@RequestMapping(value = "/{questionId}/publishstatistics", method = RequestMethod.POST)
	public void publishStatistics(
			@PathVariable final String questionId,
			@RequestParam(required = false) final Boolean showStatistics,
			@RequestBody final Content content
			) {
		if (showStatistics != null) {
			content.setShowStatistic(showStatistics);
		}
		contentService.update(content);
	}

	@ApiOperation(value = "Publish correct answer from content with provided id",
			nickname = "publishCorrectAnswer")
	@RequestMapping(value = "/{questionId}/publishcorrectanswer", method = RequestMethod.POST)
	public void publishCorrectAnswer(
			@PathVariable final String questionId,
			@RequestParam(required = false) final Boolean showCorrectAnswer,
			@RequestBody final Content content
			) {
		if (showCorrectAnswer != null) {
			content.setShowAnswer(showCorrectAnswer);
		}
		contentService.update(content);
	}

	@ApiOperation(value = "Get skill questions",
			nickname = "getSkillQuestions")
	@RequestMapping(value = "/", method = RequestMethod.GET)
	@Pagination
	public List<Content> getSkillQuestions(
			@RequestParam final String sessionkey,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") final boolean lectureQuestionsOnly,
			@RequestParam(value = "flashcardsonly", defaultValue = "false") final boolean flashcardsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") final boolean preparationQuestionsOnly,
			@RequestParam(value = "requestImageData", defaultValue = "false") final boolean requestImageData,
			final HttpServletResponse response
			) {
		List<Content> contents;
		if (lectureQuestionsOnly) {
			contents = contentService.getLectureQuestions(sessionkey);
		} else if (flashcardsOnly) {
			contents = contentService.getFlashcards(sessionkey);
		} else if (preparationQuestionsOnly) {
			contents = contentService.getPreparationQuestions(sessionkey);
		} else {
			contents = contentService.getSkillQuestions(sessionkey);
		}
		if (contents == null || contents.isEmpty()) {
			response.setStatus(HttpStatus.NO_CONTENT.value());
			return null;
		} else if (!requestImageData) {
			contents = contentService.replaceImageData(contents);
		}

		return new PaginationListDecorator<>(contents, offset, limit);
	}

	@ApiOperation(value = "Delete skill questions",
			nickname = "deleteSkillQuestions")
	@RequestMapping(value = { "/" }, method = RequestMethod.DELETE)
	public void deleteSkillQuestions(
			@RequestParam final String sessionkey,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") final boolean lectureQuestionsOnly,
			@RequestParam(value = "flashcardsonly", defaultValue = "false") final boolean flashcardsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") final boolean preparationQuestionsOnly,
			final HttpServletResponse response
			) {
		if (lectureQuestionsOnly) {
			contentService.deleteLectureQuestions(sessionkey);
		} else if (flashcardsOnly) {
			contentService.deleteFlashcards(sessionkey);
		} else if (preparationQuestionsOnly) {
			contentService.deletePreparationQuestions(sessionkey);
		} else {
			contentService.deleteAllQuestions(sessionkey);
		}
	}

	@ApiOperation(value = "Get the amount of skill questions by the sessionkey",
			nickname = "getSkillQuestionCount")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/count", method = RequestMethod.GET)
	public int getSkillQuestionCount(
			@RequestParam final String sessionkey,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") final boolean lectureQuestionsOnly,
			@RequestParam(value = "flashcardsonly", defaultValue = "false") final boolean flashcardsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") final boolean preparationQuestionsOnly
			) {
		if (lectureQuestionsOnly) {
			return contentService.getLectureQuestionCount(sessionkey);
		} else if (flashcardsOnly) {
			return contentService.getFlashcardCount(sessionkey);
		} else if (preparationQuestionsOnly) {
			return contentService.getPreparationQuestionCount(sessionkey);
		} else {
			return contentService.getSkillQuestionCount(sessionkey);
		}
	}

	@ApiOperation(value = "Delete answers and questions",
			nickname = "deleteAnswersAndQuestion")
	@RequestMapping(value = "/{questionId}", method = RequestMethod.DELETE)
	public void deleteAnswersAndQuestion(
			@PathVariable final String questionId
			) {
		contentService.deleteQuestion(questionId);
	}

	@ApiOperation(value = "Get unanswered skill question ID by provided session ID",
			nickname = "getUnAnsweredSkillQuestionIds")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/unanswered", method = RequestMethod.GET)
	public List<String> getUnAnsweredSkillQuestionIds(
			@RequestParam final String sessionkey,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") final boolean lectureQuestionsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") final boolean preparationQuestionsOnly
			) {
		List<String> answers;
		if (lectureQuestionsOnly) {
			answers = contentService.getUnAnsweredLectureQuestionIds(sessionkey);
		} else if (preparationQuestionsOnly) {
			answers = contentService.getUnAnsweredPreparationQuestionIds(sessionkey);
		} else {
			answers = contentService.getUnAnsweredQuestionIds(sessionkey);
		}
		if (answers == null || answers.isEmpty()) {
			throw new NoContentException();
		}

		return answers;
	}

	/**
	 * returns a JSON document which represents the given answer of a question.
	 *
	 * @param questionId
	 *            CouchDB Content ID for which the given answer should be
	 *            retrieved
	 * @return JSON Document of {@link Answer} or {@link NotFoundException}
	 * @throws NotFoundException
	 *             if wrong session, wrong question or no answer was given by
	 *             the current user
	 * @throws ForbiddenException
	 *             if not logged in
	 */
	@ApiOperation(value = "Get my answer for a question, identified by provided question ID",
			nickname = "getMyAnswer")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/{questionId}/myanswer", method = RequestMethod.GET)
	public Answer getMyAnswer(
			@PathVariable final String questionId,
			final HttpServletResponse response
			) {
		final Answer answer = contentService.getMyAnswer(questionId);
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
	 * @param questionId
	 *            CouchDB Content ID for which the given answers should be
	 *            retrieved
	 * @throws NotFoundException
	 *             if wrong session, wrong question or no answers was given
	 * @throws ForbiddenException
	 *             if not logged in
	 */
	@ApiOperation(value = "Get answers for a question, identified by provided question ID",
			nickname = "getAnswers")
	@RequestMapping(value = "/{questionId}/answer/", method = RequestMethod.GET)
	public List<Answer> getAnswers(
			@PathVariable final String questionId,
			@RequestParam(value = "piround", required = false) final Integer piRound,
			@RequestParam(value = "all", required = false, defaultValue = "false") final Boolean allAnswers,
			final HttpServletResponse response
			) {
		List<Answer> answers;
		if (allAnswers) {
			answers = contentService.getAllAnswers(questionId, -1, -1);
		} else if (null == piRound) {
			answers = contentService.getAnswers(questionId, offset, limit);
		} else {
			if (piRound < 1 || piRound > 2) {
				response.setStatus(HttpStatus.BAD_REQUEST.value());

				return null;
			}
			answers = contentService.getAnswers(questionId, piRound, offset, limit);
		}
		if (answers == null) {
			return new ArrayList<>();
		}
		return answers;
	}

	@ApiOperation(value = "Save answer, provided in the Request Body, for a question, identified by provided question ID",
			nickname = "saveAnswer")
	@RequestMapping(value = "/{questionId}/answer/", method = RequestMethod.POST)
	public Answer saveAnswer(
			@PathVariable final String questionId,
			@RequestBody final de.thm.arsnova.entities.transport.Answer answer,
			final HttpServletResponse response
			) {
		return contentService.saveAnswer(questionId, answer);
	}

	@ApiOperation(value = "Update answer, provided in Request Body, identified by question ID and answer ID",
			nickname = "updateAnswer")
	@RequestMapping(value = "/{questionId}/answer/{answerId}", method = RequestMethod.PUT)
	public Answer updateAnswer(
			@PathVariable final String questionId,
			@PathVariable final String answerId,
			@RequestBody final Answer answer,
			final HttpServletResponse response
			) {
		return contentService.updateAnswer(answer);
	}

	@ApiOperation(value = "Get Image, identified by question ID and answer ID",
			nickname = "getImage")
	@RequestMapping(value = "/{questionId}/answer/{answerId}/image", method = RequestMethod.GET)
	public String getImage(
			@PathVariable final String questionId,
			@PathVariable final String answerId,
			final HttpServletResponse response
			) {

		return contentService.getImage(questionId, answerId);
	}

	@ApiOperation(value = "Delete answer, identified by question ID and answer ID",
			nickname = "deleteAnswer")
	@RequestMapping(value = "/{questionId}/answer/{answerId}", method = RequestMethod.DELETE)
	public void deleteAnswer(
			@PathVariable final String questionId,
			@PathVariable final String answerId,
			final HttpServletResponse response
			) {
		contentService.deleteAnswer(questionId, answerId);
	}

	@ApiOperation(value = "Delete answers from a question, identified by question ID",
			nickname = "deleteAnswers")
	@RequestMapping(value = "/{questionId}/answer/", method = RequestMethod.DELETE)
	public void deleteAnswers(
			@PathVariable final String questionId,
			final HttpServletResponse response
			) {
		contentService.deleteAnswers(questionId);
	}

	@ApiOperation(value = "Delete all answers and questions from a session, identified by sessionkey",
			nickname = "deleteAllQuestionsAnswers")
	@RequestMapping(value = "/answers", method = RequestMethod.DELETE)
	public void deleteAllQuestionsAnswers(
			@RequestParam final String sessionkey,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") final boolean lectureQuestionsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") final boolean preparationQuestionsOnly,
			final HttpServletResponse response
			) {
		if (lectureQuestionsOnly) {
			contentService.deleteAllLectureAnswers(sessionkey);
		} else if (preparationQuestionsOnly) {
			contentService.deleteAllPreparationAnswers(sessionkey);
		} else {
			contentService.deleteAllQuestionsAnswers(sessionkey);
		}
	}

	/**
	 *
	 * @param questionId
	 *            CouchDB Content ID for which the given answers should be
	 *            retrieved
	 * @return count of answers for given question id
	 * @throws NotFoundException
	 *             if wrong session or wrong question
	 * @throws ForbiddenException
	 *             if not logged in
	 */
	@ApiOperation(value = "Get the amount of answers for a question, identified by question ID",
			nickname = "getAnswerCount")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/{questionId}/answercount", method = RequestMethod.GET)
	public int getAnswerCount(@PathVariable final String questionId) {
		return contentService.getAnswerCount(questionId);
	}

	@ApiOperation(value = "Get the amount of answers for a question, identified by the question ID",
			nickname = "getAllAnswerCount")
	@RequestMapping(value = "/{questionId}/allroundanswercount", method = RequestMethod.GET)
	public List<Integer> getAllAnswerCount(@PathVariable final String questionId) {
		return Arrays.asList(
			contentService.getAnswerCount(questionId, 1),
			contentService.getAnswerCount(questionId, 2)
		);
	}

	@ApiOperation(value = "Get the total amount of answers by a question, identified by the question ID",
			nickname = "getTotalAnswerCountByQuestion")
	@RequestMapping(value = "/{questionId}/totalanswercount", method = RequestMethod.GET)
	public int getTotalAnswerCountByQuestion(@PathVariable final String questionId) {
		return contentService.getTotalAnswerCountByQuestion(questionId);
	}

	@ApiOperation(value = "Get the amount of answers and abstention answers by a question, identified by the question ID",
			nickname = "getAnswerAndAbstentionCount")
	@RequestMapping(value = "/{questionId}/answerandabstentioncount", method = RequestMethod.GET)
	public List<Integer> getAnswerAndAbstentionCount(@PathVariable final String questionId) {
		return Arrays.asList(
			contentService.getAnswerCount(questionId),
			contentService.getAbstentionAnswerCount(questionId)
		);
	}

	@ApiOperation(value = "Get all Freetext answers by a question, identified by the question ID",
			nickname = "getFreetextAnswers")
	@RequestMapping(value = "/{questionId}/freetextanswer/", method = RequestMethod.GET)
	@Pagination
	public List<Answer> getFreetextAnswers(@PathVariable final String questionId) {
		return contentService.getFreetextAnswers(questionId, offset, limit);
	}

	@ApiOperation(value = "Get my answers of an session, identified by the sessionkey",
			nickname = "getMyAnswers")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/myanswers", method = RequestMethod.GET)
	public List<Answer> getMyAnswers(@RequestParam final String sessionkey) {
		return contentService.getMyAnswers(sessionkey);
	}

	@ApiOperation(value = "Get the total amount of answers of an session, identified by the sessionkey",
			nickname = "getTotalAnswerCount")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/answercount", method = RequestMethod.GET)
	public int getTotalAnswerCount(
			@RequestParam final String sessionkey,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") final boolean lectureQuestionsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") final boolean preparationQuestionsOnly
			) {
		if (lectureQuestionsOnly) {
			return contentService.countLectureQuestionAnswers(sessionkey);
		} else if (preparationQuestionsOnly) {
			return contentService.countPreparationQuestionAnswers(sessionkey);
		} else {
			return contentService.getTotalAnswerCount(sessionkey);
		}
	}
}
