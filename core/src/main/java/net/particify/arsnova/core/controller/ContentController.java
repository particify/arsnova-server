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

package net.particify.arsnova.core.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.core.config.AppConfig;
import net.particify.arsnova.core.model.AnswerStatistics;
import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.WordContent;
import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.service.AnswerService;
import net.particify.arsnova.core.service.ContentService;
import net.particify.arsnova.core.service.DuplicationService;
import net.particify.arsnova.core.web.exceptions.BadRequestException;
import net.particify.arsnova.core.web.exceptions.NotFoundException;

@RestController
@EntityRequestMapping(ContentController.REQUEST_MAPPING)
public class ContentController extends AbstractEntityController<Content> {
  protected static final String REQUEST_MAPPING = "/room/{roomId}/content";
  private static final String GET_ANSWER_STATISTICS_MAPPING = DEFAULT_ID_MAPPING + "/stats";
  private static final String DELETE_ANSWERS_MAPPING = DEFAULT_ID_MAPPING + "/answer";
  private static final String CORRECT_CHOICE_INDEXES_MAPPING = DEFAULT_ID_MAPPING + "/correct-choice-indexes";
  private static final String CORRECT_TERMS_MAPPING = DEFAULT_ID_MAPPING + "/correct-terms";
  private static final String DUPLICATE_MAPPING = DEFAULT_ID_MAPPING + "/duplicate";
  private static final String CONTENT_COUNT_MAPPING = NO_ID_MAPPING + "/count";
  private static final String EXPORT_MAPPING = NO_ID_MAPPING + "/export";
  private static final String BANNED_KEYWORDS_MAPPING = DEFAULT_ID_MAPPING + "/banned-keywords";
  private static final String STOP_MAPPING = DEFAULT_ID_MAPPING + "/stop";
  private static final String START_ROUND_MAPPING = DEFAULT_ID_MAPPING + "/start-round";

  private ContentService contentService;
  private AnswerService answerService;
  private DuplicationService duplicationService;

  public ContentController(
      @Qualifier("securedContentService") final ContentService contentService,
      @Qualifier("securedAnswerService") final AnswerService answerService,
      @Qualifier("securedDuplicationService") final DuplicationService duplicationService) {
    super(contentService);
    this.contentService = contentService;
    this.answerService = answerService;
    this.duplicationService = duplicationService;
  }

  @Override
  protected String getMapping() {
    return REQUEST_MAPPING;
  }

  @GetMapping(GET_ANSWER_STATISTICS_MAPPING)
  public AnswerStatistics getAnswerStatistics(@PathVariable final String id) {
    return answerService.getAllStatistics(id);
  }

  @DeleteMapping(DELETE_ANSWERS_MAPPING)
  public void deleteAnswers(@PathVariable final String id) {
    answerService.deleteAnswers(id);
  }

  @GetMapping(CORRECT_CHOICE_INDEXES_MAPPING)
  public List<Integer> getCorrectOptionIndexes(@PathVariable final String id) {
    return contentService.getCorrectChoiceIndexes(id);
  }

  @GetMapping(CORRECT_TERMS_MAPPING)
  public Set<String> getCorrectTerms(@PathVariable final String id) {
    return contentService.getCorrectTerms(id);
  }

  @PostMapping(DUPLICATE_MAPPING)
  public Content duplicate(
      @PathVariable final String id,
      @RequestBody final DuplicateRequestEntity duplicateRequestEntity) {
    final Content content = contentService.get(id);
    if (content == null) {
      throw new NotFoundException();
    }
    return duplicationService.duplicateContent(content, duplicateRequestEntity.contentGroupId);
  }

  @GetMapping(CONTENT_COUNT_MAPPING)
  public List<Integer> getCounts(
      @RequestParam final List<String> roomIds
  ) {
    return roomIds.stream().map(roomId -> contentService.countByRoomId(roomId)).collect(Collectors.toList());
  }

  @PostMapping(value = EXPORT_MAPPING, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<?> export(@RequestBody final ExportRequestEntity exportRequestEntity)
      throws JsonProcessingException {
    final ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment");
    switch (exportRequestEntity.fileType) {
      case CSV:
        return builder
            .contentType(AppConfig.CSV_MEDIA_TYPE)
            .body(contentService.exportToCsv(exportRequestEntity.contentIds, exportRequestEntity.charset));
      case TSV:
        return builder
            .contentType(AppConfig.TSV_MEDIA_TYPE)
            .body(contentService.exportToTsv(exportRequestEntity.contentIds, exportRequestEntity.charset));
      default:
        throw new BadRequestException("Unsupported export type.");
    }
  }

  @PostMapping(BANNED_KEYWORDS_MAPPING)
  public void addToBannedKeywords(
      @PathVariable final String id,
      @RequestBody final BannedKeywordRequestEntity bannedKeywordEntity) {
    final Content content = contentService.get(id);
    if (!(content instanceof WordContent)) {
      throw new BadRequestException("Only word contents are supported.");
    }
    final WordContent wordContent = (WordContent) content;
    contentService.addToBannedKeywords(wordContent, bannedKeywordEntity.keyword);
  }

  @DeleteMapping(BANNED_KEYWORDS_MAPPING)
  public void clearBannedKeywords(@PathVariable final String id) {
    final Content content = contentService.get(id);
    if (!(content instanceof WordContent)) {
      throw new BadRequestException("Only word contents are supported.");
    }
    final WordContent wordContent = (WordContent) content;
    contentService.clearBannedKeywords(wordContent);
  }

  @PostMapping(STOP_MAPPING)
  public void stop(@PathVariable final String id) {
    contentService.stop(id);
  }

  @PostMapping(START_ROUND_MAPPING)
  public void startRound(@PathVariable final String id, @RequestParam final int round) {
    contentService.startRound(id, round);
  }

  @JsonView(View.Public.class)
  private static class ExportRequestEntity {
    private FileType fileType;
    private List<String> contentIds;
    private String charset;

    public void setFileType(final FileType fileType) {
      this.fileType = fileType;
    }

    public void setContentIds(final List<String> contentIds) {
      this.contentIds = contentIds;
    }

    public void setCharset(final String charset) {
      this.charset = charset;
    }

    private enum FileType {
      CSV,
      TSV
    }
  }

  private static class DuplicateRequestEntity {
    private String contentGroupId;

    public void setContentGroupId(final String contentGroupId) {
      this.contentGroupId = contentGroupId;
    }
  }

  private record BannedKeywordRequestEntity(String keyword) { }
}
