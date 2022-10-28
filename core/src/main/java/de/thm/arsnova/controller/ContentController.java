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

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.model.AnswerStatistics;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.service.AnswerService;
import de.thm.arsnova.service.ContentService;

@RestController
@RequestMapping(ContentController.REQUEST_MAPPING)
public class ContentController extends AbstractEntityController<Content> {
	protected static final String REQUEST_MAPPING = "/content";
	private static final String GET_ANSWER_STATISTICS_MAPPING = DEFAULT_ID_MAPPING + "/stats";
	private static final String DELETE_ANSWERS_MAPPING = DEFAULT_ID_MAPPING + "/answer";
	private static final String CORRECT_CHOICE_INDEXES_MAPPING = DEFAULT_ID_MAPPING + "/correct-choice-indexes";
	private static final String CONTENT_COUNT_MAPPING = NO_ID_MAPPING + "/count";

	private ContentService contentService;
	private AnswerService answerService;

	public ContentController(
			@Qualifier("securedContentService") final ContentService contentService,
			@Qualifier("securedAnswerService") final AnswerService answerService) {
		super(contentService);
		this.contentService = contentService;
		this.answerService = answerService;
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

	@GetMapping(CONTENT_COUNT_MAPPING)
	public List<Integer> getCounts(
			@RequestParam final List<String> roomIds
	) {
		return roomIds.stream().map(roomId -> contentService.countByRoomId(roomId)).collect(Collectors.toList());
	}
}
