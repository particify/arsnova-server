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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ektorp.DbAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.annotation.Secured;
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
import de.thm.arsnova.model.MultipleTextsAnswer;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.WordcloudContent;
import de.thm.arsnova.persistence.AnswerRepository;
import de.thm.arsnova.security.User;
import de.thm.arsnova.web.exceptions.ForbiddenException;
import de.thm.arsnova.web.exceptions.NotFoundException;
import de.thm.arsnova.web.exceptions.UnauthorizedException;

/**
 * Performs all answer related operations.
 */
@Service
@Primary
public class AnswerServiceImpl extends DefaultEntityServiceImpl<Answer> implements AnswerService {
	private static final Logger logger = LoggerFactory.getLogger(AnswerServiceImpl.class);
	private static final Pattern specialCharPattern = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]");

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
	public Answer getMyAnswer(final String contentId) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}
		return answerRepository.findByContentIdUserIdPiRound(
				contentId, Answer.class, userService.getCurrentUser().getId(), content.getState().getRound());
	}

	@Override
	public AnswerStatistics getStatistics(final String contentId, final int round) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}
		final AnswerStatistics stats;
		final int optionCount;
		if (content instanceof ChoiceQuestionContent) {
			optionCount = ((ChoiceQuestionContent) content).getOptions().size();
			stats = answerRepository.findByContentIdRound(
					content.getId(), round, optionCount);
		} else if (content instanceof GridImageContent) {
			final GridImageContent.Grid grid = ((GridImageContent) content).getGrid();
			optionCount = grid.getColumns() * grid.getRows();
			stats = answerRepository.findByContentIdRound(
					content.getId(), round, optionCount);
		} else if (content instanceof WordcloudContent) {
			/* Count is not fixed for wordcloud options */
			optionCount = -1;
			stats = getTextStatistics(contentId, round);
		} else {
			throw new IllegalStateException(
					"Content expected to be an instance of ChoiceQuestionContent or GridImageContent");
		}

		if (!(content instanceof WordcloudContent)) {
			/* Fill list with zeros to prevent IndexOutOfBoundsExceptions */
			final List<Integer> independentCounts = stats.getRoundStatistics().get(round - 1).getIndependentCounts();
			while (independentCounts.size() < optionCount) {
				independentCounts.add(0);
			}
		}

		return stats;
	}

	@Override
	public AnswerStatistics getStatistics(final String contentId) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}

		return getStatistics(content.getId(), content.getState().getRound());
	}

	@Override
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

	private AnswerStatistics getTextStatistics(final String contentId, final int round) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}
		final List<MultipleTextsAnswer> answers = answerRepository.findByContentIdRoundForText(contentId, round);
		/* Flatten lists of individual answers to a combined map of texts with
		 * count */
		final Map<String, Long> textCounts = answers.stream()
				.flatMap(a -> a.getTexts().stream())
				.collect(Collectors.groupingBy(
						Function.identity(),
						Collectors.counting()));
		final AnswerStatistics stats = new AnswerStatistics();
		final AnswerStatistics.TextRoundStatistics roundStats = new AnswerStatistics.TextRoundStatistics();
		roundStats.setRound(round);
		roundStats.setAbstentionCount((int) answers.stream().filter(a -> a.getTexts().isEmpty()).count());
		/* Group by text similarity and then choose the most common variant as
		 * key and calculate the new count */
		final Map<String, Integer> countsBySimilarity = textCounts.entrySet().stream()
				.collect(Collectors.groupingBy(e ->
						specialCharPattern.matcher(e.getKey().toLowerCase()).replaceAll("")))
				.entrySet().stream()
				.collect(Collectors.toMap(
						/* Select most common variant as key */
						entry -> entry.getValue().stream()
								.max(Map.Entry.comparingByValue())
								.map(Map.Entry::getKey)
								.orElse(""),
						/* Calculate sum of counts of similar texts */
						entry -> entry.getValue().stream()
								.map(Map.Entry::getValue)
								.reduce(Long::sum)
								.map(Long::intValue)
								.orElse(0)));
		roundStats.setIndependentCounts(countsBySimilarity.values().stream().collect(Collectors.toList()));
		roundStats.setTexts(countsBySimilarity.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList()));
		stats.setRoundStatistics(new ArrayList(Collections.nCopies(round, null)));
		stats.getRoundStatistics().set(round - 1, roundStats);

		return stats;
	}

	@Override
	public List<String> getAnswerIdsByContentId(final String contentId) {
		return answerRepository.findIdsByContentId(contentId);
	}

	@Override
	public List<String> getAnswerIdsByCreatorIdRoomId(final String creatorId, final String roomId) {
		return answerRepository.findIdsByCreatorIdRoomId(creatorId, roomId);
	}

	@Override
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
	public Answer getAnswerByContentIdAndUserIdAndCurrentRound(final String contentId, final String userId) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}

		final int piRound = content.getState().getRound();

		return answerRepository.findByContentIdUserIdPiRound(contentId, Answer.class, userId, piRound);
	}

	@Override
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

	@EventListener
	@Secured({"ROLE_USER", "RUN_AS_SYSTEM"})
	public void handleContentDeletion(final BeforeDeletionEvent<Content> event) {
		final Iterable<Answer> answers = answerRepository.findStubsByContentId(event.getEntity().getId());
		answers.forEach(a -> a.setRoomId(event.getEntity().getRoomId()));
		delete(answers);
	}

	private static class TextStatEntry {
		private List<String> variants;
		private int count;
	}
}
