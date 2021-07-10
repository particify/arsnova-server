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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import de.thm.arsnova.event.BeforeDeletionEvent;
import de.thm.arsnova.model.ChoiceQuestionContent;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.ContentGroup;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.export.ContentExport;
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
@Primary
public class ContentServiceImpl extends DefaultEntityServiceImpl<Content> implements ContentService {
	private UserService userService;

	private RoomService roomService;

	private LogEntryRepository dbLogger;

	private ContentRepository contentRepository;

	private ContentGroupServiceImpl contentGroupService;

	private AnswerService answerService;

	private AnswerRepository answerRepository;

	private CsvService csvService;

	private static final Logger logger = LoggerFactory.getLogger(ContentServiceImpl.class);

	public ContentServiceImpl(
			final ContentRepository repository,
			final RoomService roomService,
			final AnswerRepository answerRepository,
			final LogEntryRepository dbLogger,
			final UserService userService,
			final CsvService csvService,
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
			final Validator validator) {
		super(Content.class, repository, jackson2HttpMessageConverter.getObjectMapper(), validator);
		this.contentRepository = repository;
		this.roomService = roomService;
		this.answerRepository = answerRepository;
		this.dbLogger = dbLogger;
		this.userService = userService;
		this.csvService = csvService;
	}

	@Autowired
	public void setAnswerService(final AnswerService answerService) {
		this.answerService = answerService;
	}

	@Autowired
	public void setContentGroupService(final ContentGroupServiceImpl contentGroupService) {
		this.contentGroupService = contentGroupService;
	}

	@Override
	protected void modifyRetrieved(final Content content) {
		if (content.getFormat() != Content.Format.TEXT && 0 == content.getState().getRound()) {
			/* needed for legacy questions whose piRound property has not been set */
			content.getState().setRound(1);
		}
	}

	/* FIXME: caching */
	@Override
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
		final ContentGroup contentGroup = contentGroupService.getByRoomIdAndName(roomId, group);

		if (contentGroup == null) {
			throw new NotFoundException("Content group does not exist.");
		}

		return get(contentGroup.getContentIds());
	}

	@Override
	public int countByRoomId(final String roomId) {
		return contentRepository.countByRoomId(roomId);
	}

	@Override
	public int countByRoomIdAndGroup(final String roomId, final String group) {
		final ContentGroup contentGroup = contentGroupService.getByRoomIdAndName(roomId, group);

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
		final List<ContentGroup> contentGroups = contentGroupService.getByRoomId(content.getRoomId());
		for (final ContentGroup contentGroup : contentGroups) {
			final List<String> ids = contentGroup.getContentIds();
			if (ids.contains(content.getId())) {
				ids.remove(content.getId());
				contentGroupService.update(contentGroup);
			}
		}
	}

	@Override
	public List<Integer> getCorrectChoiceIndexes(final String contentId) {
		final Content content = get(contentId);
		if (content instanceof ChoiceQuestionContent) {
			final ChoiceQuestionContent choiceQuestionContent = (ChoiceQuestionContent) content;
			return choiceQuestionContent.getCorrectOptionIndexes();
		}

		throw new IllegalArgumentException("Content has no choice indexes.");
	}

	@Override
	public byte[] exportToCsv(final List<String> contentIds, final String charset) throws JsonProcessingException {
		final List<Content> contents = get(contentIds);
		return csvService.toCsv(
				contents.stream().map(c -> new ContentExport(c)).collect(Collectors.toList()),
				ContentExport.class,
				charset == null ? StandardCharsets.UTF_8 : Charset.forName(charset));
	}

	@Override
	public byte[] exportToTsv(final List<String> contentIds, final String charset) throws JsonProcessingException {
		final List<Content> contents = get(contentIds);
		return csvService.toTsv(
				contents.stream().map(c -> new ContentExport(c)).collect(Collectors.toList()),
				ContentExport.class,
				charset == null ? StandardCharsets.UTF_8 : Charset.forName(charset));
	}

	private void deleteByRoomAndGroupName(final Room room, final String groupName) {
		if ("all".equals(groupName)) {
			delete(contentRepository.findStubsByRoomId(room.getId()));
		} else {
			final List<String> ids = contentGroupService.getByRoomIdAndName(room.getId(), groupName).getContentIds();
			final Iterable<Content> contents = contentRepository.findStubsByIds(ids);
			contents.forEach(c -> c.setRoomId(room.getId()));
			delete(contents);
		}
	}

	@EventListener
	@Secured({"ROLE_USER", "RUN_AS_SYSTEM"})
	public void handleRoomDeletion(final BeforeDeletionEvent<Room> event) {
		final Iterable<Content> contents = contentRepository.findStubsByRoomId(event.getEntity().getId());
		delete(contents);
	}
}
