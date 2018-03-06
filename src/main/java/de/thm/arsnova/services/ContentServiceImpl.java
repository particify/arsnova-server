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

import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import de.thm.arsnova.events.*;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.persistance.AnswerRepository;
import de.thm.arsnova.persistance.ContentRepository;
import de.thm.arsnova.persistance.LogEntryRepository;
import de.thm.arsnova.persistance.RoomRepository;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Performs all content related operations.
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

	@Cacheable("contents")
	@Override
	public Content get(final String id) {
		try {
			final Content content = super.get(id);
			if (content.getFormat() != Content.Format.TEXT && 0 == content.getState().getRound()) {
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
			@CacheEvict(value = "lecturecontentlists", key = "#roomId", condition = "#content.getGroups().contains('lecture')"),
			@CacheEvict(value = "preparationcontentlists", key = "#roomId", condition = "#content.getGroups().contains('preparation')"),
			@CacheEvict(value = "flashcardcontentlists", key = "#roomId", condition = "#content.getGroups().contains('flashcard')") },
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
			@CacheEvict(value = "lecturecontentlists", allEntries = true, condition = "#content.getGroups().contains('lecture')"),
			@CacheEvict(value = "preparationcontentlists", allEntries = true, condition = "#content.getGroups().contains('preparation')"),
			@CacheEvict(value = "flashcardcontentlists", allEntries = true, condition = "#content.getGroups().contains('flashcard')") },
			put = {@CachePut(value = "contents", key = "#content.id")})
	public Content update(final Content content) {
		final ClientAuthentication user = userService.getCurrentUser();
		final Content oldContent = contentRepository.findOne(content.getId());
		if (null == oldContent) {
			throw new NotFoundException();
		}

		final Room room = roomRepository.findOne(content.getRoomId());
		if (user == null || room == null || !room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}

		if (content.getFormat() == Content.Format.TEXT) {
			content.getState().setRound(0);
		} else if (content.getState().getRound() < 1 || content.getState().getRound() > 2) {
			content.getState().setRound(oldContent.getState().getRound() > 0 ? oldContent.getState().getRound() : 1);
		}

		content.setId(oldContent.getId());
		content.setRevision(oldContent.getRevision());
		contentRepository.save(content);

		/* TODO: nyi: updating content groups */

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
	public List<Content> getByRoomId(final String roomId) {
		final Room room = roomRepository.findOne(roomId);
		final ClientAuthentication user = userService.getCurrentUser();
		if (room.getOwnerId().equals(user.getId())) {
			return contentRepository.findByRoomIdForSpeaker(roomId);
		} else {
			return contentRepository.findByRoomIdForUsers(roomId);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countByRoomId(final String roomId) {
		return contentRepository.countByRoomId(roomId);
	}

	@Override
	@PreAuthorize("hasPermission(#content.roomId, 'room', 'owner')")
	public Content save(final Content content) {
		final Room room = roomRepository.findOne(content.getRoomId());
		content.setTimestamp(new Date());

		if (content.getFormat() == Content.Format.TEXT) {
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

		for (final String groupName : result.getGroups()) {
			Room.ContentGroup group = room.getContentGroups().getOrDefault(groupName, new Room.ContentGroup());
			group.getContentIds().add(result.getId());
		}
		roomRepository.save(room);

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
			@CacheEvict(value = "lecturecontentlists", allEntries = true /*, condition = "#content.getGroups().contains('lecture')"*/),
			@CacheEvict(value = "preparationcontentlists", allEntries = true /*, condition = "#content.getGroups().contains('preparation')"*/),
			@CacheEvict(value = "flashcardcontentlists", allEntries = true /*, condition = "#content.getGroups().contains('flashcard')"*/) })
	public void delete(final String contentId) {
		final Content content = contentRepository.findOne(contentId);
		if (content == null) {
			throw new NotFoundException();
		}

		final Room room = roomRepository.findOne(content.getRoomId());
		if (room == null) {
			throw new UnauthorizedException();
		}

		for (final String groupName : content.getGroups()) {
			Room.ContentGroup group = room.getContentGroups().getOrDefault(groupName, new Room.ContentGroup());
			group.getContentIds().remove(content.getId());
		}
		roomRepository.save(room);

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
	public void deleteAllContents(final String roomId) {
		final Room room = getRoomWithAuthCheck(roomId);
		deleteBySessionAndVariant(room, "all");
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteLectureContents(final String roomId) {
		final Room room = getRoomWithAuthCheck(roomId);
		deleteBySessionAndVariant(room, "lecture");
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deletePreparationContents(final String roomId) {
		final Room room = getRoomWithAuthCheck(roomId);
		deleteBySessionAndVariant(room, "preparation");
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteFlashcards(final String roomId) {
		final Room room = getRoomWithAuthCheck(roomId);
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
			@CacheEvict(value = "contentlists", key = "#roomId"),
			@CacheEvict(value = "lecturecontentlists", key = "#roomId"),
			@CacheEvict(value = "preparationcontentlists", key = "#roomId"),
			@CacheEvict(value = "flashcardcontentlists", key = "#roomId") })
	public void setVotingAdmissions(final String roomId, final boolean disableVoting, List<Content> contents) {
		final ClientAuthentication user = getCurrentUser();
		final Room room = roomRepository.findOne(roomId);
		if (!room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		/* FIXME: Filter flashcards - flashcard format not yet implemented */
		//contents.stream().filter(c -> c.getFormat() != Format.?).collect(Collectors.toList());
		final Map<String, Object> patches = new HashMap<>();
		patches.put("responsesEnabled", !disableVoting);
		try {
			patch(contents, patches, Content::getState);
			ArsnovaEvent event;
			if (disableVoting) {
				event = new LockVotesEvent(this, room, contents);
			} else {
				event = new UnlockVotesEvent(this, room, contents);
			}
			this.publisher.publishEvent(event);
		} catch (IOException e) {
			logger.error("Patching of contents failed", e);
		}
	}

	private Room getRoomWithAuthCheck(final String roomId) {
		final ClientAuthentication user = userService.getCurrentUser();
		final Room room = roomRepository.findOne(roomId);
		if (user == null || room == null || !room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		return room;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredContentIds(final String roomId) {
		final ClientAuthentication user = getCurrentUser();
		return contentRepository.findUnansweredIdsByRoomIdAndUser(roomId, user);
	}

	private ClientAuthentication getCurrentUser() {
		final ClientAuthentication user = userService.getCurrentUser();
		if (user == null) {
			throw new UnauthorizedException();
		}
		return user;
	}

	/* FIXME: caching */
	@Override
	@PreAuthorize("isAuthenticated()")
	//@Cacheable("lecturecontentlists")
	public List<Content> getLectureContents(final String roomId) {
		final Room room = roomRepository.findOne(roomId);
		final ClientAuthentication user = userService.getCurrentUser();
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
	public List<Content> getFlashcards(final String roomId) {
		final Room room = roomRepository.findOne(roomId);
		final ClientAuthentication user = userService.getCurrentUser();
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
	public List<Content> getPreparationContents(final String roomId) {
		final Room room = roomRepository.findOne(roomId);
		final ClientAuthentication user = userService.getCurrentUser();
		if (room.getOwnerId().equals(user.getId())) {
			return contentRepository.findByRoomIdOnlyPreparationVariant(room.getId());
		} else {
			return contentRepository.findByRoomIdOnlyPreparationVariantAndActive(room.getId());
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countLectureContents(final String roomId) {
		return contentRepository.countLectureVariantByRoomId(roomRepository.findOne(roomId).getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countFlashcards(final String roomId) {
		return contentRepository.countFlashcardVariantRoomId(roomRepository.findOne(roomId).getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countPreparationContents(final String roomId) {
		return contentRepository.countPreparationVariantByRoomId(roomRepository.findOne(roomId).getId());
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public int countFlashcardsForUserInternal(final String roomId) {
		return contentRepository.findByRoomIdOnlyFlashcardVariantAndActive(roomId).size();
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredLectureContentIds(final String roomId) {
		final ClientAuthentication user = getCurrentUser();
		return this.getUnAnsweredLectureContentIds(roomId, user);
	}

	@Override
	public List<String> getUnAnsweredLectureContentIds(final String roomId, final ClientAuthentication user) {
		return contentRepository.findUnansweredIdsByRoomIdAndUserOnlyLectureVariant(roomId, user);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredPreparationContentIds(final String roomId) {
		final ClientAuthentication user = getCurrentUser();
		return this.getUnAnsweredPreparationContentIds(roomId, user);
	}

	@Override
	public List<String> getUnAnsweredPreparationContentIds(final String roomId, final ClientAuthentication user) {
		return contentRepository.findUnansweredIdsByRoomIdAndUserOnlyPreparationVariant(roomId, user);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void publishAll(final String roomId, final boolean publish) {
		/* TODO: resolve redundancies */
		final ClientAuthentication user = getCurrentUser();
		final Room room = roomRepository.findOne(roomId);
		if (!room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		final List<Content> contents = contentRepository.findByRoomId(room.getId());
		publishContents(roomId, publish, contents);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@Caching(evict = { @CacheEvict(value = "contents", allEntries = true),
			@CacheEvict(value = "contentlists", key = "#roomId"),
			@CacheEvict(value = "lecturecontentlists", key = "#roomId"),
			@CacheEvict(value = "preparationcontentlists", key = "#roomId"),
			@CacheEvict(value = "flashcardcontentlists", key = "#roomId") })
	public void publishContents(final String roomId, final boolean publish, List<Content> contents) {
		final ClientAuthentication user = getCurrentUser();
		final Room room = roomRepository.findOne(roomId);
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

	/* TODO: Split and move answer part to AnswerService */
	@Override
	@PreAuthorize("isAuthenticated()")
	@CacheEvict(value = "answerlists", allEntries = true)
	public void deleteAllContentsAnswers(final String roomId) {
		final ClientAuthentication user = getCurrentUser();
		final Room room = roomRepository.findOne(roomId);
		if (!room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}

		final List<Content> contents = contentRepository.findByRoomIdAndVariantAndActive(room.getId());
		resetContentsRoundState(room.getId(), contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());
		answerRepository.deleteAllAnswersForQuestions(contentIds);

		this.publisher.publishEvent(new DeleteAllQuestionsAnswersEvent(this, room));
	}

	/* TODO: Split and move answer part to AnswerService */
	/* TODO: Only evict cache entry for the answer's content. This requires some refactoring. */
	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'owner')")
	@CacheEvict(value = "answerlists", allEntries = true)
	public void deleteAllPreparationAnswers(String roomId) {
		final Room room = roomRepository.findOne(roomId);

		final List<Content> contents = contentRepository.findByRoomIdAndVariantAndActive(room.getId(), "preparation");
		resetContentsRoundState(room.getId(), contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());
		answerRepository.deleteAllAnswersForQuestions(contentIds);

		this.publisher.publishEvent(new DeleteAllPreparationAnswersEvent(this, room));
	}

	/* TODO: Split and move answer part to AnswerService */
	/* TODO: Only evict cache entry for the answer's content. This requires some refactoring. */
	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'owner')")
	@CacheEvict(value = "answerlists", allEntries = true)
	public void deleteAllLectureAnswers(String roomId) {
		final Room room = roomRepository.findOne(roomId);

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
