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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.	 If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ektorp.DbAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import de.thm.arsnova.event.AfterCreationEvent;
import de.thm.arsnova.event.BeforeCreationEvent;
import de.thm.arsnova.event.BeforeDeletionEvent;
import de.thm.arsnova.event.BulkChangeEvent;
import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.AnswerStatistics;
import de.thm.arsnova.model.ChoiceQuestionContent;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.GridImageContent;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.TextAnswer;
import de.thm.arsnova.persistence.AnswerRepository;
import de.thm.arsnova.security.User;
import de.thm.arsnova.web.exceptions.ForbiddenException;
import de.thm.arsnova.web.exceptions.NotFoundException;
import de.thm.arsnova.web.exceptions.UnauthorizedException;

/**
 * Performs all answer related operations.
 */
@Service
public class AnswerServiceImpl extends DefaultEntityServiceImpl<Answer> implements AnswerService {
	private static final Logger logger = LoggerFactory.getLogger(AnswerServiceImpl.class);

	private final Queue<Answer> answerQueue = new ConcurrentLinkedQueue<>();

	private RoomService roomService;
	private ContentService contentService;
	private ContentGroupService contentGroupService;
	private AnswerRepository answerRepository;
	private UserService userService;

	public AnswerServiceImpl(
			final AnswerRepository repository,
			final RoomService roomService,
			final UserService userService,
			@Qualifier("defaultJsonMessageConverter") final
			MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
			final Validator validator) {
		super(Answer.class, repository, jackson2HttpMessageConverter.getObjectMapper(), validator);
		this.answerRepository = repository;
		this.roomService = roomService;
		this.userService = userService;
	}

	@Autowired
	public void setContentService(final ContentService contentService) {
		this.contentService = contentService;
	}

	@Autowired
	public void setContentGroupService(final ContentGroupService contentGroupService) {
		this.contentGroupService = contentGroupService;
	}

	@Scheduled(fixedDelay = 5000)
	public void flushAnswerQueue() {
		if (answerQueue.isEmpty()) {
			// no need to send an empty bulk request.
			return;
		}

		final List<Answer> answers = new ArrayList<>();
		Answer entry;
		while ((entry = this.answerQueue.poll()) != null) {
			answers.add(entry);
		}
		try {
			for (final Answer e : answers) {
				this.eventPublisher.publishEvent(new BeforeCreationEvent<>(this, e));
			}
			answerRepository.saveAll(answers);
			for (final Answer e : answers) {
				this.eventPublisher.publishEvent(new AfterCreationEvent<>(this, e));
			}
			this.eventPublisher.publishEvent(new BulkChangeEvent<>(this, Answer.class, answers));
		} catch (final DbAccessException e) {
			logger.error("Could not bulk save answers from queue.", e);
		}
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'owner')")
	public void deleteAnswers(final String contentId) {
		final Content content = contentService.get(contentId);
		content.resetState();
		/* FIXME: cancel timer */
		contentService.update(content);
		final Iterable<Answer> answers = answerRepository.findStubsByContentId(content.getId());
		answers.forEach(a -> a.setRoomId(content.getRoomId()));
		delete(answers);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Answer getMyAnswer(final String contentId) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}
		return answerRepository.findByContentIdUserIdPiRound(
				contentId, Answer.class, userService.getCurrentUser().getId(), content.getState().getRound());
	}

	@Override
	public void getFreetextAnswerAndMarkRead(final String answerId, final String userId) {
		final Answer answer = get(answerId);
		if (!(answer instanceof TextAnswer)) {
			throw new NotFoundException();
		}
		final TextAnswer textAnswer = (TextAnswer) answer;
		if (textAnswer.isRead()) {
			return;
		}
		final Room room = roomService.get(textAnswer.getRoomId());
		if (room.getOwnerId().equals(userId)) {
			textAnswer.setRead(true);
			update(textAnswer);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public AnswerStatistics getStatistics(final String contentId, final int round) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}
		final int optionCount;
		if (content instanceof ChoiceQuestionContent) {
			optionCount = ((ChoiceQuestionContent) content).getOptions().size();
		} else if (content instanceof GridImageContent) {
			final GridImageContent.Grid grid = ((GridImageContent) content).getGrid();
			optionCount = grid.getColumns() * grid.getRows();
		} else {
			throw new IllegalStateException(
					"Content expected to be an instance of ChoiceQuestionContent or GridImageContent");
		}

		final AnswerStatistics stats = answerRepository.findByContentIdRound(
				content.getId(), round, optionCount);
		/* Fill list with zeros to prevent IndexOutOfBoundsExceptions */
		final List<Integer> independentCounts = stats.getRoundStatistics().get(round - 1).getIndependentCounts();
		while (independentCounts.size() < optionCount) {
			independentCounts.add(0);
		}

		return stats;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public AnswerStatistics getStatistics(final String contentId) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}

		return getStatistics(content.getId(), content.getState().getRound());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public AnswerStatistics getAllStatistics(final String contentId) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}
		final AnswerStatistics stats = getStatistics(content.getId(), 1);
		final AnswerStatistics stats2 = getStatistics(content.getId(), 2);
		stats.getRoundStatistics().add(stats2.getRoundStatistics().get(1));

		return stats;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<TextAnswer> getTextAnswers(final String contentId, final int piRound, final int offset, final int limit) {
		/* FIXME: round support not implemented */
		final Content content = contentService.get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}

		return getTextAnswersByContentId(contentId, offset, limit);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<TextAnswer> getTextAnswers(final String contentId, final int offset, final int limit) {
		return getTextAnswers(contentId, 0, offset, limit);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<TextAnswer> getAllTextAnswers(final String contentId, final int offset, final int limit) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}

		return getTextAnswersByContentId(contentId, offset, limit);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countAnswersByContentIdAndRound(final String contentId) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			return 0;
		}

		if (content.getFormat() == Content.Format.TEXT) {
			return answerRepository.countByContentId(content.getId());
		} else {
			return answerRepository.countByContentIdRound(content.getId(), content.getState().getRound());
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countAnswersByContentIdAndRound(final String contentId, final int piRound) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			return 0;
		}

		return answerRepository.countByContentIdRound(content.getId(), piRound);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countTotalAbstentionsByContentId(final String contentId) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			return 0;
		}

		return answerRepository.countByContentId(contentId);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countTotalAnswersByContentId(final String contentId) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			return 0;
		}

		return answerRepository.countByContentId(content.getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getAnswerIdsByContentId(final String contentId) {
		return answerRepository.findIdsByContentId(contentId);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getAnswerIdsByCreatorIdRoomId(final String creatorId, final String roomId) {
		return answerRepository.findIdsByCreatorIdRoomId(creatorId, roomId);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getAnswerIdsByCreatorIdContentIdsRound(
			final String creatorId, final List<String> contentIds, final int round) {
		final List<String> ids = new ArrayList<>();
		return Stream.concat(
				/* TODO:
				 *   Currently round 0 is always added because of text answers.
				 *   It might be better to always use round 1 for text or allow multiple rounds.
				 *   This would require refactoring in other parts of the application. */
				answerRepository.findIdsByCreatorIdContentIdsRound(creatorId, contentIds, 0).stream(),
				answerRepository.findIdsByCreatorIdContentIdsRound(creatorId, contentIds, round).stream()
		).collect(Collectors.toList());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<TextAnswer> getTextAnswersByContentId(final String contentId, final int offset, final int limit) {
		final List<TextAnswer> answers = answerRepository.findByContentId(contentId, TextAnswer.class, offset, limit);
		if (answers == null) {
			throw new NotFoundException();
		}

		return answers;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Answer getAnswerByContentIdAndUserIdAndCurrentRound(final String contentId, final String userId) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}

		final int piRound = content.getState().getRound();

		return answerRepository.findByContentIdUserIdPiRound(contentId, Answer.class, userId, piRound);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getMyAnswersByRoomId(final String roomId) {
		// Load contents first because we are only interested in answers of the latest piRound.
		final List<Content> contents = contentService.getByRoomId(roomId);
		final Map<String, Content> contentIdToContent = new HashMap<>();
		for (final Content content : contents) {
			contentIdToContent.put(content.getId(), content);
		}

		/* filter answers by active piRound per content */
		final List<Answer> answers = answerRepository.findByUserIdRoomId(userService.getCurrentUser().getId(), roomId);
		final List<Answer> filteredAnswers = new ArrayList<>();
		for (final Answer answer : answers) {
			final Content content = contentIdToContent.get(answer.getContentId());
			if (content == null) {
				// Content is not present. Most likely it has been locked by the
				// Room's creator. Locked Questions do not appear in this list.
				continue;
			}

			// discard all answers that aren't in the same piRound as the content
			if (answer.getRound() == content.getState().getRound()) {
				filteredAnswers.add(answer);
			}
		}

		return filteredAnswers;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countTotalAnswersByRoomId(final String roomId) {
		return answerRepository.countByRoomId(roomId);
	}

	@Override
	@PreAuthorize("isAuthenticated() && hasPermission(#answer, 'create')")
	public Answer create(final Answer answer) {
		prepareCreate(answer);
		answerQueue.offer(answer);
		finalizeCreate(answer);

		return answer;
	}

	@Override
	protected void prepareCreate(final Answer answer) {
		final User user = userService.getCurrentUser();
		final Content content = contentService.get(answer.getContentId());
		if (content == null) {
			throw new NotFoundException();
		}

		final Answer maybeExistingAnswer = answerRepository.findByContentIdUserIdPiRound(
				content.getId(),
				Answer.class,
				user.getId(),
				content.getState().getRound());

		if (maybeExistingAnswer != null) {
			throw new ForbiddenException();
		}

		if (answer.getCreatorId() == null) {
			answer.setCreatorId(user.getId());
		}
		answer.setRoomId(content.getRoomId());

		/* FIXME: migrate
		answer.setQuestionValue(content.calculateValue(answer));
		*/

		if (content.getFormat() == Content.Format.TEXT) {
			answer.setRound(0);
			/* FIXME: migrate
			imageUtils.generateThumbnailImage(answer);
			if (content.isFixedAnswer() && content.getBody() != null) {
				answer.setAnswerTextRaw(answer.getAnswerText());

				if (content.isStrictMode()) {
					content.checkTextStrictOptions(answer);
				}
				answer.setQuestionValue(content.evaluateCorrectAnswerFixedText(answer.getAnswerTextRaw()));
				answer.setSuccessfulFreeTextAnswer(content.isSuccessfulFreeTextAnswer(answer.getAnswerTextRaw()));
			}
			*/
		} else {
			answer.setRound(content.getState().getRound());
		}
	}

	@Override
	protected void prepareUpdate(final Answer answer) {
		final User user = userService.getCurrentUser();
		final Answer realAnswer = this.getMyAnswer(answer.getContentId());
		if (user == null || realAnswer == null || !user.getId().equals(realAnswer.getCreatorId())) {
			throw new UnauthorizedException();
		}

		final Content content = contentService.get(answer.getContentId());
		/* FIXME: migrate
		if (content.getFormat() == Content.Format.TEXT) {
			imageUtils.generateThumbnailImage(realAnswer);
			content.checkTextStrictOptions(realAnswer);
		}
		*/
		final Room room = roomService.get(content.getRoomId());
		answer.setCreatorId(user.getId());
		answer.setContentId(content.getId());
		answer.setRoomId(room.getId());
	}

	@Override
	public Map<String, Object> countAnswersAndAbstentionsInternal(final String contentId) {
		final Content content = contentService.get(contentId);
		final HashMap<String, Object> map = new HashMap<>();

		if (content == null) {
			return null;
		}

		map.put("_id", contentId);
		map.put("answers", answerRepository.countByContentIdRound(content.getId(), content.getState().getRound()));
		map.put("abstentions", answerRepository.countByContentId(contentId));

		return map;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countLectureContentAnswers(final String roomId) {
		return this.countLectureQuestionAnswersInternal(roomId);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countPreparationContentAnswers(final String roomId) {
		return this.countPreparationQuestionAnswersInternal(roomId);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public int countLectureQuestionAnswersInternal(final String roomId) {
		final Set<String> contentIds =
				contentGroupService.getByRoomIdAndName(roomId, "lecture").getContentIds();
		return answerRepository.countByContentIds(contentIds);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public int countPreparationQuestionAnswersInternal(final String roomId) {
		final Set<String> contentIds =
				contentGroupService.getByRoomIdAndName(roomId, "preparation").getContentIds();
		return answerRepository.countByContentIds(contentIds);
	}

	@EventListener
	@Secured({"ROLE_USER", "RUN_AS_SYSTEM"})
	public void handleContentDeletion(final BeforeDeletionEvent<Content> event) {
		final Iterable<Answer> answers = answerRepository.findStubsByContentId(event.getEntity().getId());
		answers.forEach(a -> a.setRoomId(event.getEntity().getRoomId()));
		delete(answers);
	}
}
