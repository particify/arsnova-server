package de.thm.arsnova.service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import de.thm.arsnova.event.RoomDuplicationEvent;
import de.thm.arsnova.model.ChoiceQuestionContent;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.ContentGroup;
import de.thm.arsnova.model.GridImageContent;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.ScaleChoiceContent;
import de.thm.arsnova.model.WordcloudContent;
import de.thm.arsnova.web.exceptions.NotFoundException;

@Service
@Primary
public class DuplicationServiceImpl implements ApplicationEventPublisherAware, DuplicationService {
	private RoomService roomService;
	private ContentGroupService contentGroupService;
	private ContentService contentService;
	private ApplicationEventPublisher applicationEventPublisher;

	public DuplicationServiceImpl(
			final RoomService roomService,
			final ContentGroupService contentGroupService,
			final ContentService contentService) {
		this.roomService = roomService;
		this.contentGroupService = contentGroupService;
		this.contentService = contentService;
	}

	@Override
	public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public Room duplicateRoomCascading(final Room room) {
		final Room roomCopy = duplicateRoom(room);
		contentGroupService.getByRoomId(room.getRoomId()).forEach(cg -> duplicateContentGroup(cg, roomCopy));
		applicationEventPublisher.publishEvent(new RoomDuplicationEvent(this, room, roomCopy));
		return roomCopy;
	}

	@Override
	public Content duplicateContent(final Content content, final String contentGroupId) {
		final ContentGroup contentGroup = contentGroupService.get(contentGroupId);
		if (contentGroup == null) {
			throw new NotFoundException("Content group does not exist.");
		}
		final Content contentCopy;
		if (content instanceof ChoiceQuestionContent) {
			contentCopy = new ChoiceQuestionContent((ChoiceQuestionContent) content);
		} else if (content instanceof ScaleChoiceContent) {
			contentCopy = new ScaleChoiceContent((ScaleChoiceContent) content);
		} else if (content instanceof WordcloudContent) {
			contentCopy = new WordcloudContent((WordcloudContent) content);
		} else if (content instanceof GridImageContent) {
			contentCopy = new GridImageContent((GridImageContent) content);
		} else {
			throw new IllegalArgumentException("Unsupported subtype: " + content.getClass().getSimpleName());
		}
		contentCopy.setRoomId(contentGroup.getRoomId());
		contentService.create(contentCopy);
		contentGroup.getContentIds().add(contentCopy.getId());
		contentGroupService.createOrUpdateContentGroup(contentGroup);

		return contentCopy;
	}

	private Room duplicateRoom(final Room room) {
		final Room.ImportMetadata importMetadata = new Room.ImportMetadata();
		importMetadata.setSource("DUPLICATION");
		importMetadata.setTimestamp(new Date());
		final Room roomCopy = new Room(room);
		roomCopy.setShortId(roomService.generateShortId());
		roomCopy.setImportMetadata(importMetadata);

		return roomService.create(roomCopy);
	}

	private ContentGroup duplicateContentGroup(final ContentGroup contentGroup, final Room room) {
		final List<Content> contents = contentService.get(contentGroup.getContentIds());
		final List<Content> contentCopies = contents.stream().map(content -> {
			final Content contentCopy;
			if (content instanceof ChoiceQuestionContent) {
				contentCopy = new ChoiceQuestionContent((ChoiceQuestionContent) content);
			} else if (content instanceof ScaleChoiceContent) {
				contentCopy = new ScaleChoiceContent((ScaleChoiceContent) content);
			} else if (content instanceof WordcloudContent) {
				contentCopy = new WordcloudContent((WordcloudContent) content);
			} else if (content instanceof GridImageContent) {
				contentCopy = new GridImageContent((GridImageContent) content);
			} else {
				throw new IllegalArgumentException("Unsupported subtype: " + content.getClass().getSimpleName());
			}
			contentCopy.setRoomId(room.getRoomId());
			return contentCopy;
		}).collect(Collectors.toList());
		contentService.create(contentCopies);
		final ContentGroup contentGroupCopy = new ContentGroup(contentGroup);
		contentGroupCopy.setRoomId(room.getRoomId());
		contentGroupCopy.setContentIds(contentCopies.stream()
				.map(c -> c.getId())
				.collect(Collectors.toList()));
		contentGroupService.create(contentGroupCopy);

		return contentGroupCopy;
	}
}
