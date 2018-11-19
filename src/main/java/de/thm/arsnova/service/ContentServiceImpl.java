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
package de.thm.arsnova.service;

import de.thm.arsnova.event.AfterCreationEvent;
import de.thm.arsnova.event.AfterDeletionEvent;
import de.thm.arsnova.event.BeforeCreationEvent;
import de.thm.arsnova.event.BeforeDeletionEvent;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.persistence.AnswerRepository;
import de.thm.arsnova.persistence.ContentRepository;
import de.thm.arsnova.persistence.LogEntryRepository;
import de.thm.arsnova.persistence.RoomRepository;
import de.thm.arsnova.security.User;
import de.thm.arsnova.web.exceptions.NotFoundException;
import de.thm.arsnova.web.exceptions.UnauthorizedException;
import org.ektorp.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Performs all content related operations.
 */
@Service
public class ContentServiceImpl extends DefaultEntityServiceImpl<Content> implements ContentService {
	private UserService userService;

	private LogEntryRepository dbLogger;

	private RoomRepository roomRepository;

	private ContentRepository contentRepository;

	private AnswerRepository answerRepository;

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

			Room room = roomRepository.findOne(content.getRoomId());
			content.setGroups(room.getContentGroups().stream()
					.map(Room.ContentGroup::getName).collect(Collectors.toSet()));

			return content;
		} catch (final DocumentNotFoundException e) {
			logger.error("Could not get content {}.", id, e);
		}

		return null;
	}

	/* FIXME: caching */
	@Override
	@PreAuthorize("isAuthenticated()")
	//@Cacheable("contentlists")
	public List<Content> getByRoomId(final String roomId) {
		final Room room = roomRepository.findOne(roomId);
		final User user = userService.getCurrentUser();
		if (room.getOwnerId().equals(user.getId())) {
			return contentRepository.findByRoomIdForSpeaker(roomId);
		} else {
			return contentRepository.findByRoomIdForUsers(roomId);
		}
	}

	@Override
	public Iterable<Content> getByRoomIdAndGroup(final String roomId, final String group) {
		final Room room = roomRepository.findOne(roomId);
		Room.ContentGroup contentGroup = null;
		for (Room.ContentGroup cg : room.getContentGroups()) {
			if (cg.getName().equals(group)) {
				contentGroup = cg;
			}
		}
		if (contentGroup == null) {
			throw new NotFoundException("Content group does not exist.");
		}

		return get(contentGroup.getContentIds());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countByRoomId(final String roomId) {
		return contentRepository.countByRoomId(roomId);
	}

	@Override
	public int countByRoomIdAndGroup(final String roomId, final String group) {
		final Room room = roomRepository.findOne(roomId);
		Room.ContentGroup contentGroup = null;
		for (Room.ContentGroup cg : room.getContentGroups()) {
			if (cg.getName().equals(group)) {
				contentGroup = cg;
			}
		}
		if (contentGroup == null) {
			throw new NotFoundException("Content group does not exist.");
		}

		return contentGroup.getContentIds().size();
	}

	@Override
	public void prepareCreate(final Content content) {
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
	}

	@Override
	public void finalizeCreate(final Content content) {
		/* Update content groups of room */
		final Room room = roomRepository.findOne(content.getRoomId());
		final Set<Room.ContentGroup> contentGroups = room.getContentGroups();
		for (final Room.ContentGroup cg : contentGroups) {
			if (content.getGroups().contains(cg.getName())) {
				cg.getContentIds().add(content.getId());
				content.getGroups().remove(cg.getName());
			}
		}
		for (final String newContentGroups : content.getGroups()) {
			Room.ContentGroup newGroup = new Room.ContentGroup();
			Set<String> newContentIds = new HashSet<String>();
			newContentIds.add(content.getId());
			newGroup.setName(newContentGroups);
			newGroup.setAutoSort(true);
			newGroup.setContentIds(newContentIds);
			room.getContentGroups().add(newGroup);
		}
		eventPublisher.publishEvent(new BeforeCreationEvent<>(this, content));
		roomRepository.save(room);
		eventPublisher.publishEvent(new AfterCreationEvent<>(this, content));
	}

	@Override
	public void prepareUpdate(final Content content) {
		final User user = userService.getCurrentUser();
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
	}

	@Override
	public void finalizeUpdate(final Content content) {
		/* Update content groups of room */
		final Room room = roomRepository.findOne(content.getRoomId());
		for (final Room.ContentGroup cg : room.getContentGroups()) {
			if (content.getGroups().contains(cg.getName())) {
				cg.getContentIds().add(content.getId());
				content.getGroups().remove(cg.getName());
			} else {
				cg.getContentIds().remove(content.getId());
				if (cg.getContentIds().isEmpty()) {
					room.getContentGroups().remove(cg);
				}
			}
		}
		for (final String newContentGroups : content.getGroups()) {
			Room.ContentGroup newGroup = new Room.ContentGroup();
			Set<String> newContentIds = new HashSet<String>();
			newContentIds.add(content.getId());
			newGroup.setName(newContentGroups);
			newGroup.setAutoSort(true);
			newGroup.setContentIds(newContentIds);
			room.getContentGroups().add(newGroup);
		}
		roomRepository.save(room);

		/* TODO: not sure yet how to refactor this code - we need access to the old and new entity
		if (!oldContent.getState().isVisible() && content.getState().isVisible()) {
			final UnlockQuestionEvent event = new UnlockQuestionEvent(this, room, content);
			this.publisher.publishEvent(event);
		} else if (oldContent.getState().isVisible() && !content.getState().isVisible()) {
			final LockQuestionEvent event = new LockQuestionEvent(this, room, content);
			this.publisher.publishEvent(event);
		}
		*/
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

		for (final Room.ContentGroup contentGroup : room.getContentGroups()) {
			if (content.getGroups().contains(contentGroup.getName())) {
				contentGroup.getContentIds().remove(contentId);
			}
		}
		roomRepository.save(room);

		try {
			final int count = answerRepository.deleteByContentId(contentId);
			eventPublisher.publishEvent(new BeforeDeletionEvent<>(this, content));
			contentRepository.deleteById(contentId);
			eventPublisher.publishEvent(new AfterDeletionEvent<>(this, content));
			dbLogger.log("delete", "type", "content", "answerCount", count);
		} catch (final IllegalArgumentException e) {
			logger.error("Could not delete content {}.", contentId, e);
		}
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

		/* TODO: use EntityService! */
		final int answerCount = answerRepository.deleteByContentIds(contentIds);
		final int contentCount = contentRepository.deleteByRoomId(room.getId());
		dbLogger.log("delete", "type", "question", "questionCount", contentCount);
		dbLogger.log("delete", "type", "answer", "answerCount", answerCount);
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
		content.getState().setResponsesEnabled(!disableVoting);

		if (!disableVoting && !content.getState().isVisible()) {
			content.getState().setVisible(true);
			update(content);
		} else {
			update(content);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@Caching(evict = { @CacheEvict(value = "contents", allEntries = true),
			@CacheEvict(value = "contentlists", key = "#roomId"),
			@CacheEvict(value = "lecturecontentlists", key = "#roomId"),
			@CacheEvict(value = "preparationcontentlists", key = "#roomId"),
			@CacheEvict(value = "flashcardcontentlists", key = "#roomId") })
	public void setVotingAdmissions(final String roomId, final boolean disableVoting, Iterable<Content> contents) {
		final User user = getCurrentUser();
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
		} catch (IOException e) {
			logger.error("Patching of contents failed", e);
		}
	}

	private Room getRoomWithAuthCheck(final String roomId) {
		final User user = userService.getCurrentUser();
		final Room room = roomRepository.findOne(roomId);
		if (user == null || room == null || !room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		return room;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredContentIds(final String roomId) {
		final User user = getCurrentUser();
		return contentRepository.findUnansweredIdsByRoomIdAndUser(roomId, user.getId());
	}

	private User getCurrentUser() {
		final User user = userService.getCurrentUser();
		if (user == null) {
			throw new UnauthorizedException();
		}
		return user;
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
		final User user = getCurrentUser();
		return this.getUnAnsweredLectureContentIds(roomId, user.getId());
	}

	@Override
	public List<String> getUnAnsweredLectureContentIds(final String roomId, final String userId) {
		return contentRepository.findUnansweredIdsByRoomIdAndUserOnlyLectureVariant(roomId, userId);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredPreparationContentIds(final String roomId) {
		final User user = getCurrentUser();
		return this.getUnAnsweredPreparationContentIds(roomId, user.getId());
	}

	@Override
	public List<String> getUnAnsweredPreparationContentIds(final String roomId, final String userId) {
		return contentRepository.findUnansweredIdsByRoomIdAndUserOnlyPreparationVariant(roomId, userId);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void publishAll(final String roomId, final boolean publish) throws IOException {
		/* TODO: resolve redundancies */
		final User user = getCurrentUser();
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
	public void publishContents(final String roomId, final boolean publish, Iterable<Content> contents) throws IOException {
		final User user = getCurrentUser();
		final Room room = roomRepository.findOne(roomId);
		if (!room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		patch(contents, Collections.singletonMap("visible", publish), Content::getState);
	}

	/* TODO: Split and move answer part to AnswerService */
	@Override
	@PreAuthorize("isAuthenticated()")
	@CacheEvict(value = "answerlists", allEntries = true)
	public void deleteAllContentsAnswers(final String roomId) {
		final User user = getCurrentUser();
		final Room room = roomRepository.findOne(roomId);
		if (!room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}

		final List<Content> contents = contentRepository.findByRoomIdAndVariantAndActive(room.getId());
		resetContentsRoundState(room.getId(), contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());
		/* TODO: use EntityService! */
		answerRepository.deleteAllAnswersForQuestions(contentIds);
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
		/* TODO: use EntityService! */
		answerRepository.deleteAllAnswersForQuestions(contentIds);
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
		/* TODO: use EntityService! */
		answerRepository.deleteAllAnswersForQuestions(contentIds);
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
		contentRepository.saveAll(contents);
	}
}
