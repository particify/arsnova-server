/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
package de.thm.arsnova.services;

import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.AnswerStatistics;
import de.thm.arsnova.entities.ChoiceQuestionContent;
import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.TextAnswer;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import de.thm.arsnova.entities.transport.AnswerQueueElement;
import de.thm.arsnova.events.DeleteAnswerEvent;
import de.thm.arsnova.events.NewAnswerEvent;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.persistance.AnswerRepository;
import de.thm.arsnova.persistance.ContentRepository;
import de.thm.arsnova.persistance.RoomRepository;
import org.ektorp.DbAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Performs all answer related operations.
 */
@Service
public class AnswerServiceImpl extends DefaultEntityServiceImpl<Answer>
		implements AnswerService, ApplicationEventPublisherAware {
	private static final Logger logger = LoggerFactory.getLogger(ContentServiceImpl.class);

	private final Queue<AnswerQueueElement> answerQueue = new ConcurrentLinkedQueue<>();

	private ApplicationEventPublisher publisher;

	private RoomRepository roomRepository;
	private ContentRepository contentRepository;
	private AnswerRepository answerRepository;
	private ContentService contentService;
	private UserService userService;

	public AnswerServiceImpl(
			AnswerRepository repository,
			ContentRepository contentRepository,
			RoomRepository roomRepository,
			ContentService contentService,
			UserService userService,
			@Qualifier("defaultJsonMessageConverter") MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
		super(Answer.class, repository, jackson2HttpMessageConverter.getObjectMapper());
		this.answerRepository = repository;
		this.contentRepository = contentRepository;
		this.roomRepository = roomRepository;
		this.contentService = contentService;
		this.userService = userService;
	}

	@Scheduled(fixedDelay = 5000)
	public void flushAnswerQueue() {
		if (answerQueue.isEmpty()) {
			// no need to send an empty bulk request.
			return;
		}

		final List<Answer> answerList = new ArrayList<>();
		final List<AnswerQueueElement> elements = new ArrayList<>();
		AnswerQueueElement entry;
		while ((entry = this.answerQueue.poll()) != null) {
			final Answer answer = entry.getAnswer();
			answerList.add(answer);
			elements.add(entry);
		}
		try {
			answerRepository.saveAll(answerList);

			// Send NewAnswerEvents ...
			for (AnswerQueueElement e : elements) {
				this.publisher.publishEvent(new NewAnswerEvent(this, e.getRoom(), e.getAnswer(), e.getUser(), e.getQuestion()));
			}
		} catch (final DbAccessException e) {
			logger.error("Could not bulk save answers from queue.", e);
		}
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'owner')")
	public void deleteAnswers(final String contentId) {
		final Content content = contentRepository.findOne(contentId);
		content.resetState();
		/* FIXME: cancel timer */
		contentService.update(content);
		answerRepository.deleteByContentId(content.getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Answer getMyAnswer(final String contentId) {
		final Content content = contentService.get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}
		return answerRepository.findByContentIdUserPiRound(contentId, Answer.class, userService.getCurrentUser(), content.getState().getRound());
	}

	@Override
	public void getFreetextAnswerAndMarkRead(final String answerId, final ClientAuthentication user) {
		final Answer answer = answerRepository.findOne(answerId);
		if (!(answer instanceof TextAnswer)) {
			throw new NotFoundException();
		}
		final TextAnswer textAnswer = (TextAnswer) answer;
		if (textAnswer.isRead()) {
			return;
		}
		final Room room = roomRepository.findOne(textAnswer.getRoomId());
		if (room.getOwnerId().equals(user.getId())) {
			textAnswer.setRead(true);
			answerRepository.save(textAnswer);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public AnswerStatistics getStatistics(final String contentId, final int round) {
		final ChoiceQuestionContent content = (ChoiceQuestionContent) contentRepository.findOne(contentId);
		if (content == null) {
			throw new NotFoundException();
		}
		AnswerStatistics stats = answerRepository.findByContentIdRound(
				content.getId(), round, content.getOptions().size());
		/* Fill list with zeros to prevent IndexOutOfBoundsExceptions */
		List<Integer> independentCounts = stats.getRoundStatistics().get(round - 1).getIndependentCounts();
		while (independentCounts.size() < content.getOptions().size()) {
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
		AnswerStatistics stats = getStatistics(content.getId(), 1);
		AnswerStatistics stats2 = getStatistics(content.getId(), 2);
		stats.getRoundStatistics().add(stats2.getRoundStatistics().get(1));

		return stats;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<TextAnswer> getTextAnswers(final String contentId, final int piRound, final int offset, final int limit) {
		/* FIXME: round support not implemented */
		final Content content = contentRepository.findOne(contentId);
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
	public List<TextAnswer> getTextAnswersByContentId(final String contentId, final int offset, final int limit) {
		final List<TextAnswer> answers = answerRepository.findByContentId(contentId, TextAnswer.class, offset, limit);
		if (answers == null) {
			throw new NotFoundException();
		}

		return answers;
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
		final List<Answer> answers = answerRepository.findByUserRoomId(userService.getCurrentUser(), roomId);
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
	@PreAuthorize("isAuthenticated()")
	@CacheEvict(value = "answerlists", key = "#contentId")
	public Answer saveAnswer(final String contentId, final Answer answer) {
		final ClientAuthentication user = userService.getCurrentUser();
		final Content content = contentService.get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}
		final Room room = roomRepository.findOne(content.getRoomId());

		answer.setCreatorId(user.getId());
		answer.setContentId(content.getId());
		answer.setRoomId(room.getId());

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

		this.answerQueue.offer(new AnswerQueueElement(room, content, answer, user));

		return answer;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@CacheEvict(value = "answerlists", allEntries = true)
	public Answer updateAnswer(final Answer answer) {
		final ClientAuthentication user = userService.getCurrentUser();
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
		final Room room = roomRepository.findOne(content.getRoomId());
		answer.setCreatorId(user.getId());
		answer.setContentId(content.getId());
		answer.setRoomId(room.getId());
		answerRepository.save(realAnswer);
		this.publisher.publishEvent(new NewAnswerEvent(this, room, answer, user, content));

		return answer;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@CacheEvict(value = "answerlists", allEntries = true)
	public void deleteAnswer(final String contentId, final String answerId) {
		final Content content = contentRepository.findOne(contentId);
		if (content == null) {
			throw new NotFoundException();
		}
		final ClientAuthentication user = userService.getCurrentUser();
		final Room room = roomRepository.findOne(content.getRoomId());
		if (user == null || room == null || !room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		answerRepository.deleteById(answerId);

		this.publisher.publishEvent(new DeleteAnswerEvent(this, room, content));
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public int countLectureQuestionAnswersInternal(final String roomId) {
		return answerRepository.countByRoomIdOnlyLectureVariant(roomRepository.findOne(roomId).getId());
	}

	@Override
	public Map<String, Object> countAnswersAndAbstentionsInternal(final String contentId) {
		final Content content = contentService.get(contentId);
		HashMap<String, Object> map = new HashMap<>();

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
	public int countPreparationQuestionAnswersInternal(final String roomId) {
		return answerRepository.countByRoomIdOnlyPreparationVariant(roomRepository.findOne(roomId).getId());
	}

	@Override
	public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}
}
