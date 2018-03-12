/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team
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

import de.thm.arsnova.entities.AnswerStatistics;
import de.thm.arsnova.entities.Content;
import de.thm.arsnova.services.AnswerService;
import de.thm.arsnova.services.ContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ContentController.REQUEST_MAPPING)
public class ContentController extends AbstractEntityController<Content> {
	protected static final String REQUEST_MAPPING = "/content";
	private static final String GET_ANSWER_STATISTICS_MAPPING = DEFAULT_ID_MAPPING + "/stats";

	private ContentService contentService;
	private AnswerService answerService;

	public ContentController(final ContentService contentService, final AnswerService answerService) {
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
}
