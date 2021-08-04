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

package de.thm.arsnova.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
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

import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.model.AnswerStatistics;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.service.AnswerService;
import de.thm.arsnova.service.ContentService;
import de.thm.arsnova.service.DuplicationService;
import de.thm.arsnova.web.exceptions.BadRequestException;
import de.thm.arsnova.web.exceptions.NotFoundException;

@RestController
@EntityRequestMapping(ContentController.REQUEST_MAPPING)
public class ContentController extends AbstractEntityController<Content> {
	protected static final String REQUEST_MAPPING = "/content";
	private static final String GET_ANSWER_STATISTICS_MAPPING = DEFAULT_ID_MAPPING + "/stats";
	private static final String DELETE_ANSWERS_MAPPING = DEFAULT_ID_MAPPING + "/answer";
	private static final String CORRECT_CHOICE_INDEXES_MAPPING = DEFAULT_ID_MAPPING + "/correct-choice-indexes";
	private static final String DUPLICATE_MAPPING = DEFAULT_ID_MAPPING + "/duplicate";
	private static final String CONTENT_COUNT_MAPPING = NO_ID_MAPPING + "/count";
	private static final String EXPORT_MAPPING = NO_ID_MAPPING + "/export";

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
}
