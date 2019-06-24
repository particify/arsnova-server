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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.naming.OperationNotSupportedException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.controller.PaginationController;
import de.thm.arsnova.model.ChoiceAnswer;
import de.thm.arsnova.model.ChoiceQuestionContent;
import de.thm.arsnova.model.TextAnswer;
import de.thm.arsnova.model.migration.FromV2Migrator;
import de.thm.arsnova.model.migration.ToV2Migrator;
import de.thm.arsnova.model.migration.v2.Answer;
import de.thm.arsnova.model.migration.v2.Content;
import de.thm.arsnova.service.AnswerService;
import de.thm.arsnova.service.ContentService;
import de.thm.arsnova.service.RoomService;
import de.thm.arsnova.service.TimerService;
import de.thm.arsnova.util.PaginationListDecorator;
import de.thm.arsnova.web.DeprecatedApi;
import de.thm.arsnova.web.Pagination;
import de.thm.arsnova.web.exceptions.BadRequestException;
import de.thm.arsnova.web.exceptions.ForbiddenException;
import de.thm.arsnova.web.exceptions.NoContentException;
import de.thm.arsnova.web.exceptions.NotFoundException;
import de.thm.arsnova.web.exceptions.NotImplementedException;

/**
 * Handles requests related to contents.
 */
@RestController("v2ContentController")
@RequestMapping("/v2/lecturerquestion")
@Api(value = "/lecturerquestion", description = "Content (Skill/Lecturer Question) API")
public class ContentController extends PaginationController {
	@Autowired
	private ContentService contentService;

	@Autowired
	private AnswerService answerService;

	@Autowired
	private RoomService roomService;

	@Autowired
	private TimerService timerService;

	@Autowired
	private ToV2Migrator toV2Migrator;

	@Autowired
	private FromV2Migrator fromV2Migrator;

	@ApiOperation(value = "Get content with provided content Id",
			nickname = "getContent")
	@ApiResponses(value = {
		@ApiResponse(code = 404, message = HTML_STATUS_404)
	})
	@RequestMapping(value = "/{contentId}", method = RequestMethod.GET)
	public Content getContent(@PathVariable final String contentId) {
		final de.thm.arsnova.model.Content content = contentService.get(contentId);
		if (content != null) {
			return toV2Migrator.migrate(content);
		}

		throw new NotFoundException();
	}

	@ApiOperation(value = "Post provided content",
			nickname = "postContent")
	@ApiResponses(value = {
		@ApiResponse(code = 400, message = HTML_STATUS_400)
	})
	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Content postContent(@RequestBody final Content content) {
		de.thm.arsnova.model.Content contentV3 = fromV2Migrator.migrate(content);
		final String roomId = roomService.getIdByShortId(content.getSessionKeyword());
		contentV3.setRoomId(roomId);
		contentService.create(contentV3);

		return toV2Migrator.migrate(contentV3);
	}

	@ApiOperation(value = "Post provided contents", nickname = "bulkPostContents")
	@ApiResponses(value = {
		@ApiResponse(code = 400, message = HTML_STATUS_400)
	})
	@RequestMapping(value = "/bulk", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public List<Content> bulkPostContents(@RequestBody final List<Content> contents) {
		List<de.thm.arsnova.model.Content> contentsV3 =
				contents.stream().map(c -> contentService.create(fromV2Migrator.migrate(c))).collect(Collectors.toList());
		return contentsV3.stream().map(toV2Migrator::migrate).collect(Collectors.toList());
	}

	@ApiOperation(value = "Update the content, identified by provided id, with the provided content in the Request Body",
			nickname = "updateContent")
	@ApiResponses(value = {
		@ApiResponse(code = 400, message = HTML_STATUS_400)
	})
	@RequestMapping(value = "/{contentId}", method = RequestMethod.PUT)
	public Content updateContent(
			@PathVariable final String contentId,
			@RequestBody final Content content
			) {
		return toV2Migrator.migrate(contentService.update(fromV2Migrator.migrate(content)));
	}

	@ApiOperation(value = "Start new Pi Round on content, identified by provided id, with an optional time",
			nickname = "startPiRound")
	@RequestMapping(value = "/{contentId}/questionimage", method = RequestMethod.GET)
	public String getContentImage(
			@PathVariable final String contentId,
			@RequestParam(value = "fcImage", defaultValue = "false", required = false) final boolean fcImage
			) {

		throw new NotImplementedException();
	}

	@RequestMapping(value = "/{contentId}/startnewpiround", method = RequestMethod.POST)
	public void startPiRound(
			@PathVariable final String contentId,
			@RequestParam(value = "time", defaultValue = "0", required = false) final int time
			) {

		if (time == 0) {
			timerService.startNewRound(contentId);
		} else {
			timerService.startNewRoundDelayed(contentId, time);
		}
	}

	@RequestMapping(value = "/{contentId}/canceldelayedpiround", method = RequestMethod.POST)
	@ApiOperation(value = "Cancel Pi Round on content, identified by provided id",
			nickname = "cancelPiRound")
	public void cancelPiRound(
			@PathVariable final String contentId
			) {
		timerService.cancelRoundChange(contentId);
	}

	@RequestMapping(value = "/{contentId}/resetpiroundstate", method = RequestMethod.POST)
	@ApiOperation(value = "Reset Pi Round on content, identified by provided id",
			nickname = "resetPiContent")
	public void resetPiContent(
			@PathVariable final String contentId
			) {
		timerService.resetRoundState(contentId);
	}

	@ApiOperation(value = "Set voting admission on content, identified by provided id",
			nickname = "setVotingAdmission")
	@RequestMapping(value = "/{contentId}/disablevote", method = RequestMethod.POST)
	public void setVotingAdmission(
			@PathVariable final String contentId,
			@RequestParam(value = "disable", defaultValue = "false", required = false) final Boolean disableVote
			) {
		boolean disable = false;

		if (disableVote != null) {
			disable = disableVote;
		}

		contentService.setVotingAdmission(contentId, disable);
	}

	@ApiOperation(value = "Set voting admission for all contents",
			nickname = "setVotingAdmissionForAllContents")
	@RequestMapping(value = "/disablevote", method = RequestMethod.POST)
	public void setVotingAdmissionForAllContents(
			@RequestParam(value = "sessionkey") final String roomShortId,
			@RequestParam(value = "disable", defaultValue = "false", required = false) final Boolean disableVote,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false", required = false) boolean lectureContentsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false", required = false) boolean preparationContentsOnly
			) {
		String roomId = roomService.getIdByShortId(roomShortId);
		boolean disable = false;
		Iterable<de.thm.arsnova.model.Content> contents;

		if (disableVote != null) {
			disable = disableVote;
		}

		if (lectureContentsOnly) {
			contents = contentService.getByRoomIdAndGroup(roomId, "lecture");
			contentService.setVotingAdmissions(roomId, disable, contents);
		} else if (preparationContentsOnly) {
			contents = contentService.getByRoomIdAndGroup(roomId, "preparation");
			contentService.setVotingAdmissions(roomId, disable, contents);
		} else {
			contents = contentService.getByRoomId(roomId);
			contentService.setVotingAdmissions(roomId, disable, contents);
		}
	}

	@ApiOperation(value = "Publish a content, identified by provided id and content in Request Body.",
			nickname = "publishContent")
	@RequestMapping(value = "/{contentId}/publish", method = RequestMethod.POST)
	public void publishContent(
			@PathVariable final String contentId,
			@RequestParam(defaultValue = "true", required = false) final boolean publish,
			@RequestBody final Content content
			) throws IOException {
		de.thm.arsnova.model.Content contentV3 = fromV2Migrator.migrate(content);
		boolean p = publish;
		if (content != null) {
			p = contentV3.getState().isVisible();
		}
		contentService.patch(contentV3, Collections.singletonMap("visible", p), de.thm.arsnova.model.Content::getState);
	}

	@ApiOperation(value = "Publish all contents",
			nickname = "publishAllContents")
	@RequestMapping(value = "/publish", method = RequestMethod.POST)
	public void publishAllContents(
			@RequestParam(value = "sessionkey") final String roomShortId,
			@RequestParam(required = false) final Boolean publish,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false", required = false) boolean lectureContentsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false", required = false) boolean preparationContentsOnly
			) throws IOException {
		String roomId = roomService.getIdByShortId(roomShortId);
		boolean p = publish == null || publish;
		Iterable<de.thm.arsnova.model.Content> contents;

		if (lectureContentsOnly) {
			contents = contentService.getByRoomIdAndGroup(roomId, "lecture");
			contentService.publishContents(roomId, p, contents);
		} else if (preparationContentsOnly) {
			contents = contentService.getByRoomIdAndGroup(roomId, "preparation");
			contentService.publishContents(roomId, p, contents);
		} else {
			contentService.publishAll(roomId, p);
		}
	}

	@ApiOperation(value = "Publish statistics from content with provided id",
			nickname = "publishStatistics")
	@RequestMapping(value = "/{contentId}/publishstatistics", method = RequestMethod.POST)
	public void publishStatistics(
			@PathVariable final String contentId,
			@RequestParam(defaultValue = "true", required = false) final Boolean showStatistics,
			@RequestBody final Content content
			) throws IOException {
		de.thm.arsnova.model.Content contentV3 = fromV2Migrator.migrate(content);
		boolean p = showStatistics;
		if (content != null) {
			p = contentV3.getState().isResponsesVisible();
		}
		contentService.patch(contentV3, Collections.singletonMap("responsesVisible", p), de.thm.arsnova.model.Content::getState);
	}

	@ApiOperation(value = "Publish correct answer from content with provided id",
			nickname = "publishCorrectAnswer")
	@RequestMapping(value = "/{contentId}/publishcorrectanswer", method = RequestMethod.POST)
	public void publishCorrectAnswer(
			@PathVariable final String contentId,
			@RequestParam(defaultValue = "true", required = false) final boolean showCorrectAnswer,
			@RequestBody final Content content
			) throws IOException {
		de.thm.arsnova.model.Content contentV3 = fromV2Migrator.migrate(content);
		boolean p = showCorrectAnswer;
		if (content != null) {
			p = contentV3.getState().isSolutionVisible();
		}
		contentService.patch(contentV3, Collections.singletonMap("solutionVisible", p), de.thm.arsnova.model.Content::getState);
	}

	@ApiOperation(value = "Get contents",
			nickname = "getContents")
	@RequestMapping(value = "/", method = RequestMethod.GET)
	@Pagination
	public List<Content> getContents(
			@RequestParam(value = "sessionkey") final String roomShortId,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") boolean lectureContentsOnly,
			@RequestParam(value = "flashcardsonly", defaultValue = "false") boolean flashcardsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") boolean preparationContentsOnly,
			@RequestParam(value = "requestImageData", defaultValue = "false") boolean requestImageData,
			final HttpServletResponse response
			) {
		String roomId = roomService.getIdByShortId(roomShortId);
		Iterable<de.thm.arsnova.model.Content> contents;
		if (lectureContentsOnly) {
			contents = contentService.getByRoomIdAndGroup(roomId, "lecture");
		} else if (flashcardsOnly) {
			contents = contentService.getByRoomIdAndGroup(roomId, "flashcard");
		} else if (preparationContentsOnly) {
			contents = contentService.getByRoomIdAndGroup(roomId, "preparation");
		} else {
			contents = contentService.getByRoomId(roomId);
		}
		if (contents == null || !contents.iterator().hasNext()) {
			response.setStatus(HttpStatus.NO_CONTENT.value());
			return null;
		}

		return new PaginationListDecorator<>(StreamSupport.stream(contents.spliterator(), false)
				.map(toV2Migrator::migrate).collect(Collectors.toList()), offset, limit);
	}

	@ApiOperation(value = "Delete contents",
			nickname = "deleteContents")
	@RequestMapping(value = { "/" }, method = RequestMethod.DELETE)
	public void deleteContents(
			@RequestParam(value = "sessionkey") final String roomShortId,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") boolean lectureContentsOnly,
			@RequestParam(value = "flashcardsonly", defaultValue = "false") boolean flashcardsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") boolean preparationContentsOnly,
			final HttpServletResponse response
			) {
		String roomId = roomService.getIdByShortId(roomShortId);
		/* FIXME: Content variant is ignored for now */
		lectureContentsOnly = preparationContentsOnly = flashcardsOnly = false;
		if (lectureContentsOnly) {
			contentService.deleteLectureContents(roomId);
		} else if (preparationContentsOnly) {
			contentService.deletePreparationContents(roomId);
		} else if (flashcardsOnly) {
			contentService.deleteFlashcards(roomId);
		} else {
			contentService.deleteAllContents(roomId);
		}
	}

	@ApiOperation(value = "Get the amount of contents by the room-key",
			nickname = "getContentCount")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/count", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public String getContentCount(
			@RequestParam(value = "sessionkey") final String roomShortId,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") boolean lectureContentsOnly,
			@RequestParam(value = "flashcardsonly", defaultValue = "false") boolean flashcardsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") boolean preparationContentsOnly
			) {
		String roomId = roomService.getIdByShortId(roomShortId);
		int count;
		if (lectureContentsOnly) {
			count = contentService.countByRoomIdAndGroup(roomId, "lecture");
		} else if (preparationContentsOnly) {
			count = contentService.countByRoomIdAndGroup(roomId, "preparation");
		} else if (flashcardsOnly) {
			count = contentService.countByRoomIdAndGroup(roomId, "flashcard");
		} else {
			count = contentService.countByRoomId(roomId);
		}

		return String.valueOf(count);
	}

	@ApiOperation(value = "Delete answers and content",
			nickname = "deleteAnswersAndContent")
	@RequestMapping(value = "/{contentId}", method = RequestMethod.DELETE)
	public void deleteAnswersAndContent(
			@PathVariable final String contentId
			) {
		contentService.delete(contentId);
	}

	@ApiOperation(value = "Get unanswered content IDs by provided room short ID",
			nickname = "getUnAnsweredContentIds")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/unanswered", method = RequestMethod.GET)
	public List<String> getUnAnsweredContentIds(
			@RequestParam(value = "sessionkey") final String roomShortId,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") boolean lectureContentsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") boolean preparationContentsOnly
			) {
		String roomId = roomService.getIdByShortId(roomShortId);
		List<String> answers;
		/* FIXME: Content variant is ignored for now */
		lectureContentsOnly = preparationContentsOnly = false;
		if (lectureContentsOnly) {
			answers = contentService.getUnAnsweredLectureContentIds(roomId);
		} else if (preparationContentsOnly) {
			answers = contentService.getUnAnsweredPreparationContentIds(roomId);
		} else {
			answers = contentService.getUnAnsweredContentIds(roomId);
		}
		if (answers == null || answers.isEmpty()) {
			throw new NoContentException();
		}

		return answers;
	}

	/**
	 * returns a JSON document which represents the given answer of a content.
	 *
	 * @param contentId
	 *            CouchDB Content ID for which the given answer should be
	 *            retrieved
	 * @return JSON Document of {@link Answer} or {@link NotFoundException}
	 * @throws NotFoundException
	 *             if wrong room, wrong content or no answer was given by
	 *             the current user
	 * @throws ForbiddenException
	 *             if not logged in
	 */
	@ApiOperation(value = "Get my answer for a content, identified by provided content ID",
			nickname = "getMyAnswer")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/{contentId}/myanswer", method = RequestMethod.GET)
	public Answer getMyAnswer(
			@PathVariable final String contentId,
			final HttpServletResponse response
			) {
		final de.thm.arsnova.model.Content content = contentService.get(contentId);
		final de.thm.arsnova.model.Answer answer = answerService.getMyAnswer(contentId);
		if (answer == null) {
			response.setStatus(HttpStatus.NO_CONTENT.value());
			return null;
		}

		if (content.getFormat().equals(de.thm.arsnova.model.Content.Format.TEXT)) {
			return toV2Migrator.migrate((TextAnswer) answer);
		} else {
			return toV2Migrator.migrate((ChoiceAnswer) answer, (ChoiceQuestionContent) content);
		}
	}

	/**
	 * returns a list of {@link Answer}s encoded as a JSON document for a given
	 * content id. In this case only {@link Answer} <tt>contentId</tt>,
	 * <tt>answerText</tt>, <tt>answerSubject</tt> and <tt>answerCount</tt>
	 * properties are set
	 *
	 * @param contentId
	 *            CouchDB Content ID for which the given answers should be
	 *            retrieved
	 * @throws NotFoundException
	 *             if wrong room, wrong content or no answers was given
	 * @throws ForbiddenException
	 *             if not logged in
	 */
	@ApiOperation(value = "Get answers for a content, identified by provided content ID",
			nickname = "getAnswers")
	@RequestMapping(value = "/{contentId}/answer/", method = RequestMethod.GET)
	public List<Answer> getAnswers(
			@PathVariable final String contentId,
			@RequestParam(value = "piround", required = false) final Integer piRound,
			@RequestParam(value = "all", required = false, defaultValue = "false") final Boolean allAnswers,
			final HttpServletResponse response) {
		final de.thm.arsnova.model.Content content = contentService.get(contentId);
		if (content instanceof ChoiceQuestionContent) {
			return toV2Migrator.migrate(answerService.getAllStatistics(contentId),
					(ChoiceQuestionContent) content, content.getState().getRound());
		} else {
			List<de.thm.arsnova.model.TextAnswer> answers;
			if (allAnswers) {
				answers = answerService.getAllTextAnswers(contentId, -1, -1);
			} else if (null == piRound) {
				answers = answerService.getTextAnswers(contentId, offset, limit);
			} else {
				if (piRound < 1 || piRound > 2) {
					response.setStatus(HttpStatus.BAD_REQUEST.value());

					return null;
				}
				answers = answerService.getTextAnswers(contentId, piRound, offset, limit);
			}
			if (answers == null) {
				return new ArrayList<>();
			}
			return answers.stream().map(toV2Migrator::migrate).collect(Collectors.toList());
		}
	}

	@ApiOperation(value = "Save answer, provided in the Request Body, for a content, identified by provided content ID",
			nickname = "saveAnswer")
	@RequestMapping(value = "/{contentId}/answer/", method = RequestMethod.POST)
	public Answer saveAnswer(
			@PathVariable final String contentId,
			@RequestBody final Answer answer,
			final HttpServletResponse response
			) {
		final de.thm.arsnova.model.Content content = contentService.get(contentId);
		final de.thm.arsnova.model.Answer answerV3 = fromV2Migrator.migrate(answer, content);
		if (!contentId.equals(answerV3.getContentId())) {
			throw new BadRequestException("Mismatching content IDs.");
		}

		if (answerV3 instanceof TextAnswer) {
			return toV2Migrator.migrate((TextAnswer) answerService.create(answerV3));
		} else {
			return  toV2Migrator.migrate((ChoiceAnswer) answerService.create(answerV3), (ChoiceQuestionContent) content);
		}
	}

	@ApiOperation(value = "Update answer, provided in Request Body, identified by content ID and answer ID",
			nickname = "updateAnswer")
	@RequestMapping(value = "/{contentId}/answer/{answerId}", method = RequestMethod.PUT)
	public Answer updateAnswer(
			@PathVariable final String contentId,
			@PathVariable final String answerId,
			@RequestBody final Answer answer,
			final HttpServletResponse response
			) {
		final de.thm.arsnova.model.Content content = contentService.get(contentId);
		final de.thm.arsnova.model.Answer answerV3 = fromV2Migrator.migrate(answer, content);

		if (answerV3 instanceof TextAnswer) {
			return toV2Migrator.migrate((TextAnswer) answerService.update(answerV3));
		} else {
			return  toV2Migrator.migrate((ChoiceAnswer) answerService.update(answerV3), (ChoiceQuestionContent) content);
		}
	}

	@ApiOperation(value = "Get Image, identified by content ID and answer ID",
			nickname = "getImage")
	@RequestMapping(value = "/{contentId}/answer/{answerId}/image", method = RequestMethod.GET)
	public String getImage(
			@PathVariable final String contentId,
			@PathVariable final String answerId,
			final HttpServletResponse response
			) {

		throw new NotImplementedException();
	}

	@ApiOperation(value = "Delete answer, identified by content ID and answer ID",
			nickname = "deleteAnswer")
	@RequestMapping(value = "/{contentId}/answer/{answerId}", method = RequestMethod.DELETE)
	public void deleteAnswer(
			@PathVariable final String contentId,
			@PathVariable final String answerId,
			final HttpServletResponse response
			) {
		answerService.delete(answerService.get(answerId));
	}

	@ApiOperation(value = "Delete answers from a content, identified by content ID",
			nickname = "deleteAnswers")
	@RequestMapping(value = "/{contentId}/answer/", method = RequestMethod.DELETE)
	public void deleteAnswers(
			@PathVariable final String contentId,
			final HttpServletResponse response
			) {
		answerService.deleteAnswers(contentId);
	}

	@ApiOperation(value = "Delete all answers and contents from a room, identified by room short ID",
			nickname = "deleteAllContentsAnswers")
	@RequestMapping(value = "/answers", method = RequestMethod.DELETE)
	public void deleteAllContentsAnswers(
			@RequestParam(value = "sessionkey") final String roomShortId,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") boolean lectureContentsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") boolean preparationContentsOnly,
			final HttpServletResponse response
			) {
		String roomId = roomService.getIdByShortId(roomShortId);
		/* FIXME: Content variant is ignored for now */
		lectureContentsOnly = preparationContentsOnly = false;
		if (lectureContentsOnly) {
			contentService.deleteAllLectureAnswers(roomId);
		} else if (preparationContentsOnly) {
			contentService.deleteAllPreparationAnswers(roomId);
		} else {
			contentService.deleteAllContentsAnswers(roomId);
		}
	}

	/**
	 *
	 * @param contentId
	 *            Content ID for which the given answers should be
	 *            retrieved
	 * @return count of answers for given content ID
	 * @throws NotFoundException
	 *             if wrong room or wrong content
	 * @throws ForbiddenException
	 *             if not logged in
	 */
	@ApiOperation(value = "Get the amount of answers for a content, identified by content ID",
			nickname = "getAnswerCount")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/{contentId}/answercount", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public String getAnswerCount(@PathVariable final String contentId) {
		return String.valueOf(answerService.countAnswersByContentIdAndRound(contentId));
	}

	@ApiOperation(value = "Get the amount of answers for a content, identified by the content ID",
			nickname = "getAllAnswerCount")
	@RequestMapping(value = "/{contentId}/allroundanswercount", method = RequestMethod.GET)
	public List<Integer> getAllAnswerCount(@PathVariable final String contentId) {
		return Arrays.asList(
				answerService.countAnswersByContentIdAndRound(contentId, 1),
				answerService.countAnswersByContentIdAndRound(contentId, 2)
		);
	}

	@ApiOperation(value = "Get the total amount of answers by a content, identified by the content ID",
			nickname = "getTotalAnswerCountByContent")
	@RequestMapping(value = "/{contentId}/totalanswercount", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public String getTotalAnswerCountByContent(@PathVariable final String contentId) {
		return String.valueOf(answerService.countTotalAnswersByContentId(contentId));
	}

	@ApiOperation(value = "Get the amount of answers and abstention answers by a content, identified by the content ID",
			nickname = "getAnswerAndAbstentionCount")
	@RequestMapping(value = "/{contentId}/answerandabstentioncount", method = RequestMethod.GET)
	public List<Integer> getAnswerAndAbstentionCount(@PathVariable final String contentId) {
		return Arrays.asList(
				answerService.countAnswersByContentIdAndRound(contentId),
				answerService.countTotalAbstentionsByContentId(contentId)
		);
	}

	@ApiOperation(value = "Get all Freetext answers by a content, identified by the content ID",
			nickname = "getFreetextAnswers")
	@RequestMapping(value = "/{contentId}/freetextanswer/", method = RequestMethod.GET)
	@Pagination
	public List<Answer> getFreetextAnswers(@PathVariable final String contentId) {
		return answerService.getTextAnswersByContentId(contentId, offset, limit).stream()
				.map(toV2Migrator::migrate).collect(Collectors.toList());
	}

	@ApiOperation(value = "Get my answers of an room, identified by the room short ID",
			nickname = "getMyAnswers")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/myanswers", method = RequestMethod.GET)
	public List<Answer> getMyAnswers(@RequestParam(value = "sessionkey") final String roomShortId) throws OperationNotSupportedException {
		return answerService.getMyAnswersByRoomId(roomService.getIdByShortId(roomShortId)).stream()
				.map(a -> {
					if (a instanceof ChoiceAnswer) {
						return toV2Migrator.migrate(
								(ChoiceAnswer) a, (ChoiceQuestionContent) contentService.get(a.getContentId()));
					} else {
						return toV2Migrator.migrate((TextAnswer) a);
					}
				}).collect(Collectors.toList());
	}

	@ApiOperation(value = "Get the total amount of answers of a room, identified by the room short ID",
			nickname = "getTotalAnswerCount")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/answercount", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public String getTotalAnswerCount(
			@RequestParam(value = "sessionkey") final String roomShortId,
			@RequestParam(value = "lecturequestionsonly", defaultValue = "false") boolean lectureContentsOnly,
			@RequestParam(value = "preparationquestionsonly", defaultValue = "false") boolean preparationContentsOnly
			) {
		String roomId = roomService.getIdByShortId(roomShortId);
		int count = 0;
		/* FIXME: Content variant is ignored for now */
		lectureContentsOnly = preparationContentsOnly = false;
		if (lectureContentsOnly) {
			count = answerService.countLectureContentAnswers(roomId);
		} else if (preparationContentsOnly) {
			count = answerService.countPreparationContentAnswers(roomId);
		} else {
			count = answerService.countTotalAnswersByRoomId(roomId);
		}

		return String.valueOf(count);
	}
}
