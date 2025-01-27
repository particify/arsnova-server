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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.particify.arsnova.core.service;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.event.AfterDeletionEvent;
import net.particify.arsnova.core.event.BeforeDeletionEvent;
import net.particify.arsnova.core.event.BeforeUpdateEvent;
import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.ContentGroup;
import net.particify.arsnova.core.model.ContentGroupTemplate;
import net.particify.arsnova.core.model.ContentTemplate;
import net.particify.arsnova.core.model.Deletion.Initiator;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.export.ContentCsvImportSummary;
import net.particify.arsnova.core.model.export.ContentExport;
import net.particify.arsnova.core.model.export.CsvImportLineResult;
import net.particify.arsnova.core.persistence.ContentGroupRepository;
import net.particify.arsnova.core.persistence.DeletionRepository;
import net.particify.arsnova.core.web.exceptions.BadRequestException;
import net.particify.arsnova.core.web.exceptions.NotFoundException;

@Service
@Primary
public class ContentGroupServiceImpl extends DefaultEntityServiceImpl<ContentGroup>
    implements ContentGroupService {
  private static final Logger logger = LoggerFactory.getLogger(ContentGroupServiceImpl.class);
  private ContentGroupRepository contentGroupRepository;
  private ContentService contentService;
  private CsvService csvService;

  public ContentGroupServiceImpl(
      final ContentGroupRepository repository,
      final DeletionRepository deletionRepository,
      final CsvService csvService,
      @Qualifier("defaultJsonMessageConverter")
      final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
      final Validator validator) {
    super(
        ContentGroup.class,
        repository,
        deletionRepository,
        jackson2HttpMessageConverter.getObjectMapper(),
        validator);
    this.contentGroupRepository = repository;
    this.csvService = csvService;
  }

  @Autowired
  public void setContentService(final ContentService contentService) {
    this.contentService = contentService;
  }

  @Override
  public void prepareCreate(final ContentGroup contentGroup) {
    if (this.getByRoomIdAndName(contentGroup.getRoomId(), contentGroup.getName()) != null) {
      throw new BadRequestException();
    }
  }

  @Override
  public void prepareUpdate(final ContentGroup contentGroup) {
    final ContentGroup oldContentGroup = this.get(contentGroup.getId());
    if (oldContentGroup != null) {
      // Disallow changing the name of a content group for now
      if (!oldContentGroup.getName().equals(contentGroup.getName())) {
        throw new BadRequestException();
      }
    }
  }

  @Override
  public ContentGroup getByRoomIdAndName(final String roomId, final String name) {
    return contentGroupRepository.findByRoomIdAndName(roomId, name);
  }

  @Override
  public List<ContentGroup> getByRoomId(final String roomId) {
    return contentGroupRepository.findByRoomId(roomId);
  }

  @Override
  public List<ContentGroup> getByRoomIdAndContainingContentId(final String roomId, final String contentId) {
    return getByRoomId(roomId).stream().filter(cg -> cg.containsContent(contentId)).collect(Collectors.toList());
  }

  @Override
  public void addContentToGroup(final String roomId, final String groupName, final String contentId) {
    ContentGroup contentGroup = getByRoomIdAndName(roomId, groupName);
    if (contentGroup == null) {
      contentGroup = new ContentGroup(roomId, groupName);
      contentGroup.getContentIds().add(contentId);
      create(contentGroup);
    } else {
      contentGroup.getContentIds().add(contentId);
      update(contentGroup);
    }
  }

  @Override
  public void removeContentFromGroup(final String groupId, final String contentId) {
    final Optional<ContentGroup> contentGroup = contentGroupRepository.findById(groupId);
    contentGroup.ifPresentOrElse(
        cg -> {
          cg.setContentIds(cg.getContentIds().stream()
              .filter(id -> !id.equals(contentId)).collect(Collectors.toList()));
          update(cg);
        }, () -> {
          throw new NotFoundException("ContentGroup does not exist.");
        });
  }

  @Override
  public ContentGroup createOrUpdateContentGroup(final ContentGroup contentGroup) {
    final List<String> contentIds = contentService.get(contentGroup.getContentIds()).stream()
        .filter(c -> c.getRoomId().equals(contentGroup.getRoomId()))
        .map(Content::getId)
        .distinct()
        .collect(Collectors.toList());
    contentGroup.setContentIds(contentIds);
    if (contentGroup.getId() != null) {
      return update(contentGroup);
    } else {
      return create(contentGroup);
    }
  }

  @Override
  public ContentGroup createFromTemplate(
      final String roomId,
      final ContentGroupTemplate template,
      final List<ContentTemplate> contentTemplates) {
    final ContentGroup contentGroup = new ContentGroup();
    contentGroup.setRoomId(roomId);
    contentGroup.setName(template.getName());
    contentGroup.setGroupType(template.getGroupType());
    contentGroup.setTemplateId(template.getId());
    final List<Content> contents = contentService.createFromTemplates(roomId, template, contentTemplates);
    contentGroup.setContentIds(contents.stream().map(c -> c.getId()).collect(Collectors.toList()));
    create(contentGroup);
    return contentGroup;
  }

  @Override
  public ContentCsvImportSummary importFromCsv(final byte[] csv, final ContentGroup contentGroup) {
    try {
      final List<CsvImportLineResult<Content>> results = csvService.toObject(csv, ContentExport.class).stream()
          .map(r -> new CsvImportLineResult<>(r.line(), r.data() != null ? r.data().toContent() : null))
          .map(r -> r.data() != null && determineCompatibility(contentGroup.getGroupType(), r.data())
              ? r
              : new CsvImportLineResult<Content>(r.line(), null))
          .toList();
      final List<Content> contents = results.stream()
          .filter(r -> r.data() != null)
          .map(r -> r.data())
          .toList();
      for (final Content content : contents) {
        content.setRoomId(contentGroup.getRoomId());
        contentService.create(content);
      }
      contentGroup.getContentIds().addAll(
          contents.stream().map(c -> c.getId()).collect(Collectors.toList()));
      createOrUpdateContentGroup(contentGroup);
      return new ContentCsvImportSummary(
          results.size(),
          contents.size(),
          results.stream().filter(c -> c.data() == null).map(r -> r.line()).toList());
    } catch (final IOException e) {
      throw new BadRequestException("Could not import contents from CSV.", e);
    }
  }

  private boolean determineCompatibility(final ContentGroup.GroupType type, final Content content) {
    if (type == ContentGroup.GroupType.MIXED || content.getFormat() == Content.Format.SLIDE) {
      return true;
    }
    if (type == ContentGroup.GroupType.FLASHCARDS) {
      return content.getFormat() == Content.Format.FLASHCARD;
    }
    final List<Content.Format> quizTypes = List.of(
        Content.Format.CHOICE,
        Content.Format.BINARY,
        Content.Format.NUMERIC,
        Content.Format.SORT,
        Content.Format.SHORT_ANSWER);
    if (type == ContentGroup.GroupType.QUIZ
        && quizTypes.contains(content.getFormat())
        && content.isScorable()) {
      return true;
    }
    final List<Content.Format> surveyTypes = List.of(
        Content.Format.CHOICE,
        Content.Format.BINARY,
        Content.Format.NUMERIC,
        Content.Format.SCALE,
        Content.Format.PRIORITIZATION,
        Content.Format.TEXT,
        Content.Format.WORDCLOUD);
    if (type == ContentGroup.GroupType.SURVEY
        && surveyTypes.contains(content.getFormat())
        && !content.isScorable()) {
      return true;
    }
    return false;
  }

  @Override
  public void startContent(final String groupId, final String contentId, final int round) throws IOException {
    final var group = get(groupId);
    if (!group.containsContent(contentId)) {
      throw new IllegalArgumentException("Content group does not contain content.");
    }
    final var contents = contentService.get(group.getContentIds());
    final var content = contents.stream().filter(c -> c.getId().equals(contentId)).findFirst().orElseThrow();
    if (content == null) {
      throw new NotFoundException();
    }
    if (round > 0) {
      if (!content.startRound(round)) {
        throw new BadRequestException("Invalid round.");
      }
    }
    if (group.getPublishingMode() == ContentGroup.PublishingMode.LIVE) {
      if (!content.startTime()) {
        throw new BadRequestException("Already started.");
      }
    }
    if (group.publishContent(contentId)) {
      update(group);
    }
    final var contentsToStop = contents.stream()
        .filter(c -> !c.getId().equals(contentId) && c.getState().getAnsweringEndTime() != null)
        .toList();
    contentService.patch(contentsToStop, Map.of("answeringEndTime", new Date()), Content::getState);
    contentService.update(content);
  }

  @EventListener
  public void handleContentGroupDeletion(final AfterDeletionEvent<ContentGroup> event) {
    // Delete contents which were part of the deleted group and are not in
    // any other group
    final Set<String> idsWithGroup = getByRoomId(event.getEntity().getRoomId()).stream()
            .flatMap(cg -> cg.getContentIds().stream()).collect(Collectors.toSet());
    final List<String> idsForDeletion = event.getEntity().getContentIds().stream()
            .filter(id -> !idsWithGroup.contains(id)).toList();
    contentService.delete(contentService.get(idsForDeletion), Initiator.CASCADE);
  }

  @EventListener
  public void handleContentGroupUpdate(final BeforeUpdateEvent<ContentGroup> event) {
    // Reset answeringEndTime of contents when live mode is disabled
    if (event.getOldEntity().getPublishingMode() == ContentGroup.PublishingMode.LIVE
        && event.getEntity().getPublishingMode() != ContentGroup.PublishingMode.LIVE) {
      try {
        final HashMap<String, Object> map = new HashMap<>();
        map.put("answeringEndTime", null);
        contentService.patch(
            contentService.get(event.getEntity().getContentIds()).stream()
                .filter(c -> c.getState().getAnsweringEndTime() != null)
                .toList(),
            map,
            Content::getState);
      } catch (final IOException e) {
        logger.error("Failed to patch state of contents.", e);
      }
    }
  }

  @EventListener
  public void handleContentDeletion(final BeforeDeletionEvent<? extends Content> event) {
    final Content content = event.getEntity();
    final List<ContentGroup> contentGroups = getByRoomId(content.getRoomId());
    for (final ContentGroup contentGroup : contentGroups) {
      final List<String> ids = contentGroup.getContentIds();
      if (ids.contains(content.getId())) {
        ids.remove(content.getId());
        update(contentGroup);
      }
    }
  }

  @EventListener
  public void handleRoomDeletion(final BeforeDeletionEvent<Room> event) {
    final Iterable<ContentGroup> contentGroups = contentGroupRepository.findByRoomId(event.getEntity().getId());
    delete(contentGroups, Initiator.CASCADE);
  }
}
