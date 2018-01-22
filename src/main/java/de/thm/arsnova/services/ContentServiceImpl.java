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
import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.TextAnswer;
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.transport.AnswerQueueElement;
import de.thm.arsnova.events.*;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.persistance.AnswerRepository;
import de.thm.arsnova.persistance.ContentRepository;
import de.thm.arsnova.persistance.LogEntryRepository;
import de.thm.arsnova.persistance.RoomRepository;
import org.ektorp.DbAccessException;
import org.ektorp.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Performs all content and answer related operations.
 */
@Service
public class ContentServiceImpl extends DefaultEntityServiceImpl<Content> implements ContentService, ApplicationEventPublisherAware {
	private UserService userService;

	private LogEntryRepository dbLogger;

	private RoomRepository roomRepository;

	private ContentRepository contentRepository;

	private AnswerRepository answerRepository;

	private ApplicationEventPublisher publisher;

	private static final Logger logger = LoggerFactory.getLogger(ContentServiceImpl.class);

	private final Queue<AnswerQueueElement> answerQueue = new ConcurrentLinkedQueue<>();

	public ContentServiceImpl(
			ContentRepository repository,
			AnswerRepository answerRepository,
			RoomRepository roomRepository,
			LogEntryRepository dbLogger,
			UserService userService,
			@Qualifier("defaultJsonMessageConverter") MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
		super(Content.class, repository, jackson2HttpMessageConverter.getObjectMapper());
		this.contentRepository = repository;
		this.answerRepository = answerRepository;
		this.roomRepository = roomRepository;
		this.dbLogger = dbLogger;
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
			answerRepository.save(answerList);

			// Send NewAnswerEvents ...
			for (AnswerQueueElement e : elements) {
				this.publisher.publishEvent(new NewAnswerEvent(this, e.getRoom(), e.getAnswer(), e.getUser(), e.getQuestion()));
			}
		} catch (final DbAccessException e) {
			logger.error("Could not bulk save answers from queue.", e);
		}
	}

	@Cacheable("contents")
	@Override
	public Content get(final String id) {
		try {
			final Content content = super.get(id);
			if (!"freetext".equals(content.getFormat()) && 0 == content.getState().getRound()) {
			/* needed for legacy questions whose piRound property has not been set */
				content.getState().setRound(1);
			}
			//content.setSessionKeyword(roomRepository.getSessionFromId(content.getRoomId()).getKeyword());

			return content;
		} catch (final DocumentNotFoundException e) {
			logger.error("Could not get content {}.", id, e);
		}

		return null;
	}

	@Override
	@Caching(evict = {@CacheEvict(value = "contentlists", key = "#roomId"),
			@CacheEvict(value = "lecturecontentlists", key = "#roomId", condition = "#content.getGroup().equals('lecture')"),
			@CacheEvict(value = "preparationcontentlists", key = "#roomId", condition = "#content.getGroup().equals('preparation')"),
			@CacheEvict(value = "flashcardcontentlists", key = "#roomId", condition = "#content.getGroup().equals('flashcard')") },
			put = {@CachePut(value = "contents", key = "#content.id")})
	public Content save(final String roomId, final Content content) {
		content.setRoomId(roomId);
		try {
			contentRepository.save(content);

			return content;
		} catch (final IllegalArgumentException e) {
			logger.error("Could not save content {}.", content, e);
		}

		return null;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@Caching(evict = {
			@CacheEvict(value = "contentlists", allEntries = true),
			@CacheEvict(value = "lecturecontentlists", allEntries = true, condition = "#content.getGroup().equals('lecture')"),
			@CacheEvict(value = "preparationcontentlists", allEntries = true, condition = "#content.getGroup().equals('preparation')"),
			@CacheEvict(value = "flashcardcontentlists", allEntries = true, condition = "#content.getGroup().equals('flashcard')") },
			put = {@CachePut(value = "contents", key = "#content.id")})
	public Content update(final Content content) {
		final UserAuthentication user = userService.getCurrentUser();
		final Content oldContent = contentRepository.findOne(content.getId());
		if (null == oldContent) {
			throw new NotFoundException();
		}

		final Room room = roomRepository.findOne(content.getRoomId());
		if (user == null || room == null || !room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}

		if ("freetext".equals(content.getFormat())) {
			content.getState().setRound(0);
		} else if (content.getState().getRound() < 1 || content.getState().getRound() > 2) {
			content.getState().setRound(oldContent.getState().getRound() > 0 ? oldContent.getState().getRound() : 1);
		}

		content.setId(oldContent.getId());
		content.setRevision(oldContent.getRevision());
		contentRepository.save(content);

		if (!oldContent.getState().isVisible() && content.getState().isVisible()) {
			final UnlockQuestionEvent event = new UnlockQuestionEvent(this, room, content);
			this.publisher.publishEvent(event);
		} else if (oldContent.getState().isVisible() && !content.getState().isVisible()) {
			final LockQuestionEvent event = new LockQuestionEvent(this, room, content);
			this.publisher.publishEvent(event);
		}
		return content;
	}

	/* FIXME: caching */
	@Override
	@PreAuthorize("isAuthenticated()")
	//@Cacheable("contentlists")
	public List<Content> getByRoomShortId(final String roomShortId) {
		final Room room = getRoom(roomShortId);
		final UserAuthentication user = userService.getCurrentUser();
		if (room.getOwnerId().equals(user.getId())) {
			return contentRepository.findByRoomIdForSpeaker(room.getId());
		} else {
			return contentRepository.findByRoomIdForUsers(room.getId());
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countByRoomShortId(final String roomShortId) {
		final Room room = roomRepository.findByShortId(roomShortId);
		return contentRepository.countByRoomId(room.getId());
	}

	/* FIXME: #content.getShortId() cannot be checked since keyword is no longer set for content. */
	@Override
	@PreAuthorize("hasPermission(#content.getShortId(), 'session', 'owner')")
	public Content save(final Content content) {
		final Room room = roomRepository.findOne(content.getRoomId());
		content.setTimestamp(new Date());

		if ("freetext".equals(content.getFormat())) {
			content.getState().setRound(0);
		} else if (content.getState().getRound() < 1 || content.getState().getRound() > 2) {
			content.getState().setRound(1);
		}

		/* FIXME: migrate
		// convert imageurl to base64 if neccessary
		if ("grid".equals(content.getFormat()) && !content.getImage().startsWith("http")) {
			// base64 adds offset to filesize, formula taken from: http://en.wikipedia.org/wiki/Base64#MIME
			final int fileSize = (int) ((content.getImage().length() - 814) / 1.37);
			if (fileSize > uploadFileSizeByte) {
				logger.error("Could not save file. File is too large with {} Byte.", fileSize);
				throw new BadRequestException();
			}
		}
		*/

		final Content result = save(room.getId(), content);

		final NewQuestionEvent event = new NewQuestionEvent(this, room, result);
		this.publisher.publishEvent(event);

		return result;
	}

	/* TODO: Only evict cache entry for the content's session. This requires some refactoring. */
	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'owner')")
	@Caching(evict = {
			@CacheEvict("answerlists"),
			@CacheEvict(value = "contents", key = "#contentId"),
			@CacheEvict(value = "contentlists", allEntries = true),
			@CacheEvict(value = "lecturecontentlists", allEntries = true /*, condition = "#content.getGroup().equals('lecture')"*/),
			@CacheEvict(value = "preparationcontentlists", allEntries = true /*, condition = "#content.getGroup().equals('preparation')"*/),
			@CacheEvict(value = "flashcardcontentlists", allEntries = true /*, condition = "#content.getGroup().equals('flashcard')"*/) })
	public void delete(final String contentId) {
		final Content content = contentRepository.findOne(contentId);
		if (content == null) {
			throw new NotFoundException();
		}

		final Room room = roomRepository.findOne(content.getRoomId());
		if (room == null) {
			throw new UnauthorizedException();
		}

		try {
			final int count = answerRepository.deleteByContentId(contentId);
			contentRepository.delete(contentId);
			dbLogger.log("delete", "type", "content", "answerCount", count);
		} catch (final IllegalArgumentException e) {
			logger.error("Could not delete content {}.", contentId, e);
		}

		final DeleteQuestionEvent event = new DeleteQuestionEvent(this, room, content);
		this.publisher.publishEvent(event);
	}

	@PreAuthorize("hasPermission(#session, 'owner')")
	@Caching(evict = {
			@CacheEvict(value = "contents", allEntries = true),
			@CacheEvict(value = "contentlists", key = "#room.getId()"),
			@CacheEvict(value = "lecturecontentlists", key = "#room.getId()", condition = "'lecture'.equals(#variant)"),
			@CacheEvict(value = "preparationcontentlists", key = "#room.getId()", condition = "'preparation'.equals(#variant)"),
			@CacheEvict(value = "flashcardcontentlists", key = "#room.getId()", condition = "'flashcard'.equals(#variant)") })
	private void deleteBySessionAndVariant(final Room room, final String variant) {
		final List<String> contentIds;
		if ("all".equals(variant)) {
			contentIds = contentRepository.findIdsByRoomId(room.getId());
		} else {
			contentIds = contentRepository.findIdsByRoomIdAndVariant(room.getId(), variant);
		}

		final int answerCount = answerRepository.deleteByContentIds(contentIds);
		final int contentCount = contentRepository.deleteByRoomId(room.getId());
		dbLogger.log("delete", "type", "question", "questionCount", contentCount);
		dbLogger.log("delete", "type", "answer", "answerCount", answerCount);

		final DeleteAllQuestionsEvent event = new DeleteAllQuestionsEvent(this, room);
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteAllContent(final String roomShortId) {
		final Room room = getRoomWithAuthCheck(roomShortId);
		deleteBySessionAndVariant(room, "all");
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteLectureQuestions(final String roomShortId) {
		final Room room = getRoomWithAuthCheck(roomShortId);
		deleteBySessionAndVariant(room, "lecture");
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deletePreparationQuestions(final String roomShortId) {
		final Room room = getRoomWithAuthCheck(roomShortId);
		deleteBySessionAndVariant(room, "preparation");
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteFlashcards(final String roomShortId) {
		final Room room = getRoomWithAuthCheck(roomShortId);
		deleteBySessionAndVariant(room, "flashcard");
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'owner')")
	public void setVotingAdmission(final String contentId, final boolean disableVoting) {
		final Content content = contentRepository.findOne(contentId);
		final Room room = roomRepository.findOne(content.getRoomId());
		content.getState().setResponsesEnabled(!disableVoting);

		if (!disableVoting && !content.getState().isVisible()) {
			content.getState().setVisible(true);
			update(content);
		} else {
			update(content);
		}
		ArsnovaEvent event;
		if (disableVoting) {
			event = new LockVoteEvent(this, room, content);
		} else {
			event = new UnlockVoteEvent(this, room, content);
		}
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@Caching(evict = { @CacheEvict(value = "contents", allEntries = true),
			@CacheEvict(value = "contentlists", key = "#roomShortId"),
			@CacheEvict(value = "lecturecontentlists", key = "#roomShortId"),
			@CacheEvict(value = "preparationcontentlists", key = "#roomShortId"),
			@CacheEvict(value = "flashcardcontentlists", key = "#roomShortId") })
	public void setVotingAdmissions(final String roomShortId, final boolean disableVoting, List<Content> contents) {
		final UserAuthentication user = getCurrentUser();
		final Room room = getRoom(roomShortId);
		if (!room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		for (final Content q : contents) {
			if (!"flashcard".equals(q.getRoomId())) {
				q.getState().setResponsesEnabled(!disableVoting);
			}
		}
		ArsnovaEvent event;
		if (disableVoting) {
			event = new LockVotesEvent(this, room, contents);
		} else {
			event = new UnlockVotesEvent(this, room, contents);
		}
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void setVotingAdmissionForAllContents(final String roomShortId, final boolean disableVoting) {
		final UserAuthentication user = getCurrentUser();
		final Room room = getRoom(roomShortId);
		if (!room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		final List<Content> contents = contentRepository.findByRoomId(room.getId());
		setVotingAdmissionForAllContents(room.getId(), disableVoting);
	}

	private Room getRoomWithAuthCheck(final String shortId) {
		final UserAuthentication user = userService.getCurrentUser();
		final Room room = roomRepository.findByShortId(shortId);
		if (user == null || room == null || !room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		return room;
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'owner')")
	public void deleteAnswers(final String contentId) {
		final Content content = contentRepository.findOne(contentId);
		content.resetState();
		/* FIXME: cancel timer */
		update(content);
		answerRepository.deleteByContentId(content.getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredQuestionIds(final String roomShortId) {
		final UserAuthentication user = getCurrentUser();
		final Room room = getRoom(roomShortId);
		return contentRepository.findUnansweredIdsByRoomIdAndUser(room.getId(), user);
	}

	private UserAuthentication getCurrentUser() {
		final UserAuthentication user = userService.getCurrentUser();
		if (user == null) {
			throw new UnauthorizedException();
		}
		return user;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Answer getMyAnswer(final String contentId) {
		final Content content = get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}
		return answerRepository.findByContentIdUserPiRound(contentId, Answer.class, userService.getCurrentUser(), content.getState().getRound());
	}

	@Override
	public void getFreetextAnswerAndMarkRead(final String answerId, final UserAuthentication user) {
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
	public AnswerStatistics getStatistics(final String contentId, final int piRound) {
		final Content content = contentRepository.findOne(contentId);
		if (content == null) {
			throw new NotFoundException();
		}

		return answerRepository.findByContentIdPiRound(content.getId(), piRound);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public AnswerStatistics getStatistics(final String contentId) {
		final Content content = get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}

		return getStatistics(content.getId(), content.getState().getRound());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public AnswerStatistics getAllStatistics(final String contentId) {
		final Content content = get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}
		AnswerStatistics stats = answerRepository.findByContentIdPiRound(content.getId(), 1);
		AnswerStatistics stats2 = answerRepository.findByContentIdPiRound(content.getId(), 2);
		stats.getRoundStatistics().add(stats2.getRoundStatistics().get(0));

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
		final Content content = get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}

		return getTextAnswersByContentId(contentId, offset, limit);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countAnswersByContentIdAndRound(final String contentId) {
		final Content content = get(contentId);
		if (content == null) {
			return 0;
		}

		if ("freetext".equals(content.getFormat())) {
			return answerRepository.countByContentId(content.getId());
		} else {
			return answerRepository.countByContentIdRound(content.getId(), content.getState().getRound());
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countAnswersByContentIdAndRound(final String contentId, final int piRound) {
		final Content content = get(contentId);
		if (content == null) {
			return 0;
		}

		return answerRepository.countByContentIdRound(content.getId(), piRound);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countTotalAbstentionsByContentId(final String contentId) {
		final Content content = get(contentId);
		if (content == null) {
			return 0;
		}

		return answerRepository.countByContentId(contentId);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countTotalAnswersByContentId(final String contentId) {
		final Content content = get(contentId);
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
	public List<Answer> getMyAnswersByRoomShortId(final String roomShortId) {
		final Room room = getRoom(roomShortId);
		// Load contents first because we are only interested in answers of the latest piRound.
		final List<Content> contents = getByRoomShortId(roomShortId);
		final Map<String, Content> contentIdToContent = new HashMap<>();
		for (final Content content : contents) {
			contentIdToContent.put(content.getId(), content);
		}

		/* filter answers by active piRound per content */
		final List<Answer> answers = answerRepository.findByUserRoomId(userService.getCurrentUser(), room.getId());
		final List<Answer> filteredAnswers = new ArrayList<>();
		for (final Answer answer : answers) {
			final Content content = contentIdToContent.get(answer.getContentId());
			if (content == null) {
				// Content is not present. Most likely it has been locked by the
				// Room's creator. Locked Questions do not appear in this list.
				continue;
			}
			if (0 == answer.getRound() && !"freetext".equals(content.getFormat())) {
				answer.setRound(1);
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
	public int countTotalAnswersByRoomShortId(final String roomShortId) {
		return answerRepository.countByRoomShortId(roomShortId);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@CacheEvict(value = "answerlists", key = "#contentId")
	public Answer saveAnswer(final String contentId, final Answer answer) {
		final UserAuthentication user = getCurrentUser();
		final Content content = get(contentId);
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

		if ("freetext".equals(content.getFormat())) {
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
		final UserAuthentication user = userService.getCurrentUser();
		final Answer realAnswer = this.getMyAnswer(answer.getContentId());
		if (user == null || realAnswer == null || !user.getId().equals(realAnswer.getCreatorId())) {
			throw new UnauthorizedException();
		}

		final Content content = get(answer.getContentId());
		/* FIXME: migrate
		if ("freetext".equals(content.getFormat())) {
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
		final UserAuthentication user = userService.getCurrentUser();
		final Room room = roomRepository.findOne(content.getRoomId());
		if (user == null || room == null || !room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		answerRepository.delete(answerId);

		this.publisher.publishEvent(new DeleteAnswerEvent(this, room, content));
	}

	/* FIXME: caching */
	@Override
	@PreAuthorize("isAuthenticated()")
	//@Cacheable("lecturecontentlists")
	public List<Content> getLectureQuestions(final String roomShortId) {
		final Room room = getRoom(roomShortId);
		final UserAuthentication user = userService.getCurrentUser();
		if (room.getOwnerId().equals(user.getId())) {
			return contentRepository.findByRoomIdOnlyLectureVariant(room.getId());
		} else {
			return contentRepository.findByRoomIdOnlyLectureVariantAndActive(room.getId());
		}
	}

	/* FIXME: caching */
	@Override
	@PreAuthorize("isAuthenticated()")
	//@Cacheable("flashcardcontentlists")
	public List<Content> getFlashcards(final String roomShortId) {
		final Room room = getRoom(roomShortId);
		final UserAuthentication user = userService.getCurrentUser();
		if (room.getOwnerId().equals(user.getId())) {
			return contentRepository.findByRoomIdOnlyFlashcardVariant(room.getId());
		} else {
			return contentRepository.findByRoomIdOnlyFlashcardVariantAndActive(room.getId());
		}
	}

	/* FIXME: caching */
	@Override
	@PreAuthorize("isAuthenticated()")
	//@Cacheable("preparationcontentlists")
	public List<Content> getPreparationQuestions(final String roomShortId) {
		final Room room = getRoom(roomShortId);
		final UserAuthentication user = userService.getCurrentUser();
		if (room.getOwnerId().equals(user.getId())) {
			return contentRepository.findByRoomIdOnlyPreparationVariant(room.getId());
		} else {
			return contentRepository.findByRoomIdOnlyPreparationVariantAndActive(room.getId());
		}
	}

	private Room getRoom(final String roomShortId) {
		final Room room = roomRepository.findByShortId(roomShortId);
		if (room == null) {
			throw new NotFoundException();
		}
		return room;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countLectureQuestions(final String roomShortId) {
		return contentRepository.countLectureVariantByRoomId(getRoom(roomShortId).getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countFlashcards(final String roomShortId) {
		return contentRepository.countFlashcardVariantRoomId(getRoom(roomShortId).getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countPreparationQuestions(final String roomShortId) {
		return contentRepository.countPreparationVariantByRoomId(getRoom(roomShortId).getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countLectureQuestionAnswers(final String roomShortId) {
		return this.countLectureQuestionAnswersInternal(roomShortId);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public int countLectureQuestionAnswersInternal(final String roomShortId) {
		return answerRepository.countByRoomIdOnlyLectureVariant(getRoom(roomShortId).getId());
	}

	@Override
	public Map<String, Object> countAnswersAndAbstentionsInternal(final String contentId) {
		final Content content = get(contentId);
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
	public int countPreparationQuestionAnswers(final String roomShortId) {
		return this.countPreparationQuestionAnswersInternal(roomShortId);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public int countPreparationQuestionAnswersInternal(final String roomShortId) {
		return answerRepository.countByRoomIdOnlyPreparationVariant(getRoom(roomShortId).getId());
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public int countFlashcardsForUserInternal(final String roomShortId) {
		return contentRepository.findByRoomIdOnlyFlashcardVariantAndActive(getRoom(roomShortId).getId()).size();
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredLectureQuestionIds(final String roomShortId) {
		final UserAuthentication user = getCurrentUser();
		return this.getUnAnsweredLectureQuestionIds(roomShortId, user);
	}

	@Override
	public List<String> getUnAnsweredLectureQuestionIds(final String roomShortId, final UserAuthentication user) {
		final Room room = getRoom(roomShortId);
		return contentRepository.findUnansweredIdsByRoomIdAndUserOnlyLectureVariant(room.getId(), user);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredPreparationQuestionIds(final String roomShortId) {
		final UserAuthentication user = getCurrentUser();
		return this.getUnAnsweredPreparationQuestionIds(roomShortId, user);
	}

	@Override
	public List<String> getUnAnsweredPreparationQuestionIds(final String roomShortId, final UserAuthentication user) {
		final Room room = getRoom(roomShortId);
		return contentRepository.findUnansweredIdsByRoomIdAndUserOnlyPreparationVariant(room.getId(), user);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void publishAll(final String roomShortId, final boolean publish) {
		/* TODO: resolve redundancies */
		final UserAuthentication user = getCurrentUser();
		final Room room = getRoom(roomShortId);
		if (!room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		final List<Content> contents = contentRepository.findByRoomId(room.getId());
		publishQuestions(roomShortId, publish, contents);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@Caching(evict = { @CacheEvict(value = "contents", allEntries = true),
			@CacheEvict(value = "contentlists", key = "#roomShortId"),
			@CacheEvict(value = "lecturecontentlists", key = "#roomShortId"),
			@CacheEvict(value = "preparationcontentlists", key = "#roomShortId"),
			@CacheEvict(value = "flashcardcontentlists", key = "#roomShortId") })
	public void publishQuestions(final String roomShortId, final boolean publish, List<Content> contents) {
		final UserAuthentication user = getCurrentUser();
		final Room room = getRoom(roomShortId);
		if (!room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		for (final Content content : contents) {
			content.getState().setVisible(publish);
		}
		contentRepository.save(contents);
		ArsnovaEvent event;
		if (publish) {
			event = new UnlockQuestionsEvent(this, room, contents);
		} else {
			event = new LockQuestionsEvent(this, room, contents);
		}
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@CacheEvict(value = "answerlists", allEntries = true)
	public void deleteAllQuestionsAnswers(final String roomShortId) {
		final UserAuthentication user = getCurrentUser();
		final Room room = getRoom(roomShortId);
		if (!room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}

		final List<Content> contents = contentRepository.findByRoomIdAndVariantAndActive(room.getId());
		resetContentsRoundState(room.getId(), contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());
		answerRepository.deleteAllAnswersForQuestions(contentIds);

		this.publisher.publishEvent(new DeleteAllQuestionsAnswersEvent(this, room));
	}

	/* TODO: Only evict cache entry for the answer's content. This requires some refactoring. */
	@Override
	@PreAuthorize("hasPermission(#roomShortId, 'session', 'owner')")
	@CacheEvict(value = "answerlists", allEntries = true)
	public void deleteAllPreparationAnswers(String roomShortId) {
		final Room room = getRoom(roomShortId);

		final List<Content> contents = contentRepository.findByRoomIdAndVariantAndActive(room.getId(), "preparation");
		resetContentsRoundState(room.getId(), contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());
		answerRepository.deleteAllAnswersForQuestions(contentIds);

		this.publisher.publishEvent(new DeleteAllPreparationAnswersEvent(this, room));
	}

	/* TODO: Only evict cache entry for the answer's content. This requires some refactoring. */
	@Override
	@PreAuthorize("hasPermission(#roomShortId, 'session', 'owner')")
	@CacheEvict(value = "answerlists", allEntries = true)
	public void deleteAllLectureAnswers(String roomShortId) {
		final Room room = getRoom(roomShortId);

		final List<Content> contents = contentRepository.findByRoomIdAndVariantAndActive(room.getId(), "lecture");
		resetContentsRoundState(room.getId(), contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());
		answerRepository.deleteAllAnswersForQuestions(contentIds);

		this.publisher.publishEvent(new DeleteAllLectureAnswersEvent(this, room));
	}

	@Caching(evict = {
			@CacheEvict(value = "contents", allEntries = true),
			@CacheEvict(value = "contentlists", key = "#roomId"),
			@CacheEvict(value = "lecturecontentlists", key = "#roomId"),
			@CacheEvict(value = "preparationcontentlists", key = "#roomId"),
			@CacheEvict(value = "flashcardcontentlists", key = "#roomId") })
	private void resetContentsRoundState(final String roomId, final List<Content> contents) {
		for (final Content q : contents) {
			/* TODO: Check if setting the sessionId is necessary. */
			q.setRoomId(roomId);
			q.resetState();
		}
		contentRepository.save(contents);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}
}
