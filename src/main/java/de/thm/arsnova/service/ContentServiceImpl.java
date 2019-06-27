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

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.event.BeforeDeletionEvent;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.Room.ContentGroup;
import de.thm.arsnova.persistence.AnswerRepository;
import de.thm.arsnova.persistence.ContentRepository;
import de.thm.arsnova.persistence.LogEntryRepository;
import de.thm.arsnova.security.User;
import de.thm.arsnova.web.exceptions.NotFoundException;
import de.thm.arsnova.web.exceptions.UnauthorizedException;

/**
 * Performs all content related operations.
 */
@Service
public class ContentServiceImpl extends DefaultEntityServiceImpl<Content> implements ContentService {
	private UserService userService;

	private RoomService roomService;

	private LogEntryRepository dbLogger;

	private ContentRepository contentRepository;

	private AnswerService answerService;

	private AnswerRepository answerRepository;

	private static final Logger logger = LoggerFactory.getLogger(ContentServiceImpl.class);

	public ContentServiceImpl(
			final ContentRepository repository,
			final RoomService roomService,
			final AnswerRepository answerRepository,
			final LogEntryRepository dbLogger,
			final UserService userService,
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
		super(Content.class, repository, jackson2HttpMessageConverter.getObjectMapper());
		this.contentRepository = repository;
		this.roomService = roomService;
		this.answerRepository = answerRepository;
		this.dbLogger = dbLogger;
		this.userService = userService;
	}

	@Autowired
	public void setAnswerService(final AnswerService answerService) {
		this.answerService = answerService;
	}

	@Override
	protected void modifyRetrieved(final Content content) {
		if (content.getFormat() != Content.Format.TEXT && 0 == content.getState().getRound()) {
			/* needed for legacy questions whose piRound property has not been set */
			content.getState().setRound(1);
		}

		final Room room = roomService.get(content.getRoomId());
		content.setGroups(room.getContentGroups().stream()
				.map(Room.ContentGroup::getName).filter(g -> !g.isEmpty()).collect(Collectors.toSet()));
	}

	/* FIXME: caching */
	@Override
	@PreAuthorize("isAuthenticated()")
	//@Cacheable("contentlists")
	public List<Content> getByRoomId(final String roomId) {
		final Room room = roomService.get(roomId);
		final User user = userService.getCurrentUser();
		if (room.getOwnerId().equals(user.getId())) {
			return contentRepository.findByRoomIdForSpeaker(roomId);
		} else {
			return contentRepository.findByRoomIdForUsers(roomId);
		}
	}

	@Override
	public Iterable<Content> getByRoomIdAndGroup(final String roomId, final String group) {
		final Room room = roomService.get(roomId);
		Room.ContentGroup contentGroup = null;
		for (final Room.ContentGroup cg : room.getContentGroups()) {
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
		final Room room = roomService.get(roomId);
		Room.ContentGroup contentGroup = null;
		for (final Room.ContentGroup cg : room.getContentGroups()) {
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
	protected void finalizeCreate(final Content content) {
		/* Update content groups of room */
		final Room room = roomService.get(content.getRoomId());
		final Map<String, Room.ContentGroup> groups = room.getContentGroupsAsMap();
		for (final String groupName : content.getGroups()) {
			final Room.ContentGroup group = groups.getOrDefault(groupName, new Room.ContentGroup());
			groups.put(groupName, group);
			group.getContentIds().add(content.getId());
			group.setName(groupName);
			group.setAutoSort(true);
		}
		room.setContentGroupsFromMap(groups);
		roomService.update(room);
	}

	@Override
	protected void prepareUpdate(final Content content) {
		final User user = userService.getCurrentUser();
		final Content oldContent = get(content.getId());
		if (null == oldContent) {
			throw new NotFoundException();
		}

		final Room room = roomService.get(content.getRoomId());
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
	protected void finalizeUpdate(final Content content) {
		/* Update content groups of room */
		final Room room = roomService.get(content.getRoomId());
		final Set<String> contentsGroupNames = content.getGroups();
		final Set<String> allGroupNames = new HashSet<>(contentsGroupNames);
		final Map<String, Room.ContentGroup> groups = room.getContentGroupsAsMap();
		allGroupNames.addAll(groups.keySet());
		for (final String groupName : allGroupNames) {
			final Room.ContentGroup group = groups.getOrDefault(groupName, new Room.ContentGroup());
			if (contentsGroupNames.contains(groupName)) {
				group.getContentIds().add(content.getId());
				group.setName(groupName);
				group.setAutoSort(true);
			} else {
				group.getContentIds().remove(content.getId());
			}
		}
		room.setContentGroupsFromMap(groups);
		roomService.update(room);

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

	@Override
	protected void prepareDelete(final Content content) {
		final Room room = roomService.get(content.getRoomId());
		for (final ContentGroup group : room.getContentGroups()) {
			group.getContentIds().remove(content.getId());
		}
		roomService.update(room);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void delete(final String contentId) {
		final Content content = get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}

		try {
			delete(content);
		} catch (final IllegalArgumentException e) {
			logger.error("Could not delete content {}.", contentId, e);
		}
	}

	@PreAuthorize("isAuthenticated()")
	private void deleteBySessionAndVariant(final Room room, final String variant) {
		final Iterable<Content> contents;
		if ("all".equals(variant)) {
			contents = contentRepository.findStubsByRoomId(room.getId());
		} else {
			contents = contentRepository.findStubsByRoomIdAndVariant(room.getId(), variant);
		}

		delete(contents);
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
		final Content content = get(contentId);
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
	public void setVotingAdmissions(final String roomId, final boolean disableVoting, final Iterable<Content> contents) {
		final User user = getCurrentUser();
		final Room room = roomService.get(roomId);
		if (!room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		/* FIXME: Filter flashcards - flashcard format not yet implemented */
		//contents.stream().filter(c -> c.getFormat() != Format.?).collect(Collectors.toList());
		final Map<String, Object> patches = new HashMap<>();
		patches.put("responsesEnabled", !disableVoting);
		try {
			patch(contents, patches, Content::getState);
		} catch (final IOException e) {
			logger.error("Patching of contents failed", e);
		}
	}

	private Room getRoomWithAuthCheck(final String roomId) {
		final User user = userService.getCurrentUser();
		final Room room = roomService.get(roomId);
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
		final Room room = roomService.get(roomId);
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
	public void publishContents(final String roomId, final boolean publish, final Iterable<Content> contents)
			throws IOException {
		final User user = getCurrentUser();
		final Room room = roomService.get(roomId);
		if (!room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}
		patch(contents, Collections.singletonMap("visible", publish), Content::getState);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteAllContentsAnswers(final String roomId) {
		final User user = getCurrentUser();
		final Room room = roomService.get(roomId);
		if (!room.getOwnerId().equals(user.getId())) {
			throw new UnauthorizedException();
		}

		final List<Content> contents = contentRepository.findByRoomIdAndVariantAndActive(room.getId());
		resetContentsRoundState(room.getId(), contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());
		answerService.delete(answerRepository.findStubsByContentIds(contentIds));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteAllPreparationAnswers(final String roomId) {
		final Room room = roomService.get(roomId);

		final List<Content> contents = contentRepository.findByRoomIdAndVariantAndActive(room.getId(), "preparation");
		resetContentsRoundState(room.getId(), contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());
		answerService.delete(answerRepository.findStubsByContentIds(contentIds));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteAllLectureAnswers(final String roomId) {
		final Room room = roomService.get(roomId);

		final List<Content> contents = contentRepository.findByRoomIdAndVariantAndActive(room.getId(), "lecture");
		resetContentsRoundState(room.getId(), contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());
		answerService.delete(answerRepository.findStubsByContentIds(contentIds));
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

	@EventListener
	@Secured({"ROLE_USER", "RUN_AS_SYSTEM"})
	public void handleRoomDeletion(final BeforeDeletionEvent<Room> event) {
		final Iterable<Content> contents = contentRepository.findStubsByRoomId(event.getEntity().getId());
		delete(contents);
	}
}
