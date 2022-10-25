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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.event.AfterDeletionEvent;
import net.particify.arsnova.core.event.BeforeDeletionEvent;
import net.particify.arsnova.core.model.ChoiceQuestionContent;
import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.ContentGroup;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.WordcloudContent;
import net.particify.arsnova.core.model.export.ContentExport;
import net.particify.arsnova.core.persistence.AnswerRepository;
import net.particify.arsnova.core.persistence.ContentRepository;
import net.particify.arsnova.core.web.exceptions.BadRequestException;
import net.particify.arsnova.core.web.exceptions.NotFoundException;

/**
 * Performs all content related operations.
 */
@Service
@Primary
public class ContentServiceImpl extends DefaultEntityServiceImpl<Content> implements ContentService {
  private ContentRepository contentRepository;

  private ContentGroupServiceImpl contentGroupService;

  private CsvService csvService;

  public ContentServiceImpl(
      final ContentRepository repository,
      final AnswerRepository answerRepository,
      final CsvService csvService,
      @Qualifier("defaultJsonMessageConverter")
      final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
      final Validator validator) {
    super(Content.class, repository, jackson2HttpMessageConverter.getObjectMapper(), validator);
    this.contentRepository = repository;
    this.csvService = csvService;
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

  /**
    * Retrieves all contents of a room.
    */
  @Override
  public List<Content> getByRoomId(final String roomId) {
    return get(contentRepository.findIdsByRoomId(roomId));
  }

  @Override
  public int countByRoomId(final String roomId) {
    return contentRepository.countByRoomId(roomId);
  }

  @Override
  public void prepareCreate(final Content content) {
    content.setTimestamp(new Date());

    if (content.getFormat() == Content.Format.TEXT) {
      content.getState().setRound(0);
    } else if (content.getState().getRound() < 1 || content.getState().getRound() > 2) {
      content.getState().setRound(1);
    }
  }

  @Override
  protected void prepareUpdate(final Content content) {
    final Content oldContent = get(content.getId());
    if (null == oldContent) {
      throw new NotFoundException();
    }

    if (!content.getRoomId().equals(oldContent.getRoomId())) {
      throw new BadRequestException();
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

  @EventListener
  public void handleRoomDeletion(final BeforeDeletionEvent<Room> event) {
    final Iterable<Content> contents = contentRepository.findStubsByRoomId(event.getEntity().getId());
    delete(contents);
  }

  @EventListener
  public void handleContentGroupDeletion(final AfterDeletionEvent<ContentGroup> event) {
    // Delete contents which were part of the deleted group and are not in
    // any other group
    final Set<String> idsWithGroup = contentGroupService.getByRoomId(event.getEntity().getRoomId()).stream()
        .flatMap(cg -> cg.getContentIds().stream()).collect(Collectors.toSet());
    final List<String> idsForDeletion = event.getEntity().getContentIds().stream()
        .filter(id -> !idsWithGroup.contains(id)).toList();
    delete(get(idsForDeletion));
  }

  @Override
  public void addToBannedKeywords(final WordcloudContent wordcloudContent, final String keyword) {
    wordcloudContent.addBannedKeyword(keyword);
    update(wordcloudContent);
  }

  @Override
  public void clearBannedKeywords(final WordcloudContent wordcloudContent) {
    wordcloudContent.getBannedKeywords().clear();
    update(wordcloudContent);
  }
}
