package net.particify.arsnova.core.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.event.RoomDuplicationEvent;
import net.particify.arsnova.core.model.ChoiceQuestionContent;
import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.ContentGroup;
import net.particify.arsnova.core.model.GridImageContent;
import net.particify.arsnova.core.model.NumericContent;
import net.particify.arsnova.core.model.PrioritizationChoiceContent;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.RoomSettings;
import net.particify.arsnova.core.model.ScaleChoiceContent;
import net.particify.arsnova.core.model.ShortAnswerContent;
import net.particify.arsnova.core.model.WordcloudContent;
import net.particify.arsnova.core.web.exceptions.NotFoundException;

@Service
@Primary
public class DuplicationServiceImpl implements ApplicationEventPublisherAware, DuplicationService {
  private static Duration TEMPORARY_DURATION = Duration.parse("P2D");
  private RoomService roomService;
  private RoomSettingsService roomSettingsService;
  private ContentGroupService contentGroupService;
  private ContentService contentService;
  private ApplicationEventPublisher applicationEventPublisher;

  public DuplicationServiceImpl(
      @Nullable final RoomService roomService,
      final RoomSettingsService roomSettingsService,
      final ContentGroupService contentGroupService,
      final ContentService contentService,
      final SystemProperties systemProperties) {
    if (roomService == null && !systemProperties.isExternalDataManagement()) {
      throw new IllegalStateException("RoomService is null but external data management is disabled.");
    }
    this.roomService = roomService;
    this.roomSettingsService = roomSettingsService;
    this.contentGroupService = contentGroupService;
    this.contentService = contentService;
  }

  @Override
  public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public Room duplicateRoomCascading(final Room room, final boolean temporary, final String newName) {
    final Room roomCopy = duplicateRoom(room, temporary, newName);
    duplicateRoomSettings(room, roomCopy);
    applicationEventPublisher.publishEvent(new RoomDuplicationEvent(this, room, roomCopy));
    return roomCopy;
  }

  @Override
  public Content duplicateContent(final Content content, final String contentGroupId) {
    final ContentGroup contentGroup = contentGroupService.get(contentGroupId);
    if (contentGroup == null) {
      throw new NotFoundException("Content group does not exist.");
    }
    final Content contentCopy = duplicateContentInstance(content);
    contentCopy.setRoomId(contentGroup.getRoomId());
    contentService.create(contentCopy);
    contentGroup.getContentIds().add(contentCopy.getId());
    contentGroupService.createOrUpdateContentGroup(contentGroup);

    return contentCopy;
  }

  private Room duplicateRoom(final Room room, final boolean temporary, final String newName) {
    final Room.ImportMetadata importMetadata = new Room.ImportMetadata();
    importMetadata.setSource("DUPLICATION");
    importMetadata.setTimestamp(new Date());
    final Room roomCopy = new Room(room);
    roomCopy.setShortId(roomService.generateShortId());
    roomCopy.setImportMetadata(importMetadata);
    if (temporary) {
      final Date scheduledDate = Date.from(
          LocalDateTime.now().plus(TEMPORARY_DURATION)
              .atZone(ZoneId.systemDefault()).toInstant());
      roomCopy.setScheduledDeletion(scheduledDate);
    }
    if (!newName.isEmpty()) {
      roomCopy.setName(newName);
    }

    return roomService.create(roomCopy);
  }

  private RoomSettings duplicateRoomSettings(final Room originalRoom, final Room duplicatedRoom) {
    final RoomSettings settings = roomSettingsService.getByRoomId(originalRoom.getRoomId());
    if (settings == null) {
      return null;
    }
    final RoomSettings settingsCopy = new RoomSettings(settings);
    settingsCopy.setRoomId(duplicatedRoom.getRoomId());
    return roomSettingsService.create(settingsCopy);
  }

  private ContentGroup duplicateContentGroup(final ContentGroup contentGroup, final Room room) {
    final List<Content> contents = contentService.get(contentGroup.getContentIds());
    final List<Content> contentCopies = contents.stream().map(content -> {
      final Content contentCopy = duplicateContentInstance(content);
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

  private Content duplicateContentInstance(final Content content) {
    // Make sure to add logic for specialized classes before more generic ones.
    if (content instanceof ScaleChoiceContent) {
      return new ScaleChoiceContent((ScaleChoiceContent) content);
    } else if (content instanceof PrioritizationChoiceContent) {
      return new PrioritizationChoiceContent((PrioritizationChoiceContent) content);
    } else if (content instanceof ChoiceQuestionContent) {
      return new ChoiceQuestionContent((ChoiceQuestionContent) content);
    } else if (content instanceof WordcloudContent) {
      return new WordcloudContent((WordcloudContent) content);
    } else if (content instanceof ShortAnswerContent shortAnswerContent) {
      return new ShortAnswerContent(shortAnswerContent);
    } else if (content instanceof GridImageContent) {
      return new GridImageContent((GridImageContent) content);
    } else if (content instanceof NumericContent) {
      return new NumericContent((NumericContent) content);
    } else if (content.getClass() == Content.class) {
      // Not using instanceof here so it does not apply to subclasses
      return new Content(content);
    } else {
      throw new IllegalArgumentException("Unsupported subtype: " + content.getClass().getSimpleName());
    }
  }

  @EventListener
  public void handleRoomDuplication(final RoomDuplicationEvent event) {
    contentGroupService.getByRoomId(event.getOriginalRoom().getId())
        .forEach(cg -> duplicateContentGroup(cg, event.getDuplicateRoom()));
  }
}
