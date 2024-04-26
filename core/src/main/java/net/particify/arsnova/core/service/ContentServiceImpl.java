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
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.event.BeforeDeletionEvent;
import net.particify.arsnova.core.model.ChoiceQuestionContent;
import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.ContentGroupTemplate;
import net.particify.arsnova.core.model.ContentTemplate;
import net.particify.arsnova.core.model.Deletion.Initiator;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.WordcloudContent;
import net.particify.arsnova.core.model.export.ContentExport;
import net.particify.arsnova.core.persistence.AnswerRepository;
import net.particify.arsnova.core.persistence.ContentRepository;
import net.particify.arsnova.core.persistence.DeletionRepository;
import net.particify.arsnova.core.web.exceptions.BadRequestException;
import net.particify.arsnova.core.web.exceptions.NotFoundException;

/**
 * Performs all content related operations.
 */
@Service
@Primary
public class ContentServiceImpl extends DefaultEntityServiceImpl<Content> implements ContentService {
  private ContentRepository contentRepository;

  private CsvService csvService;

  public ContentServiceImpl(
      final ContentRepository repository,
      final AnswerRepository answerRepository,
      final DeletionRepository deletionRepository,
      final CsvService csvService,
      @Qualifier("defaultJsonMessageConverter")
      final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
      final Validator validator) {
    super(Content.class, repository, deletionRepository, jackson2HttpMessageConverter.getObjectMapper(), validator);
    this.contentRepository = repository;
    this.csvService = csvService;
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
    delete(contents, Initiator.CASCADE);
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

  @Override
  public List<Content> createFromTemplates(
      final String roomId,
      final ContentGroupTemplate contentGroupTemplate,
      final List<ContentTemplate> templates) {
    final List<Content> contents = templates.stream()
        .map(t -> {
          final Content copy = t.getContent().copy();
          copy.setTemplateId(t.getId());
          copy.setGroupTemplateId(contentGroupTemplate.getId());
          copy.setRoomId(roomId);
          return copy;
        })
        .collect(Collectors.toList());
    return create(contents);
  }

  @Override
  public void start(final String contentId) {
    final Content content = get(contentId);
    if (content == null) {
      throw new NotFoundException();
    }
    if (content.getState().getAnsweringEndTime() != null) {
      throw new BadRequestException("Already started.");
    }
    content.getState().setAnsweringEndTime(Date.from(Instant.now().plusSeconds(content.getDuration())));
    update(content);
  }
}
