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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.model.Answer;
import de.thm.arsnova.service.AnswerService;

@RestController
@RequestMapping(AnswerController.REQUEST_MAPPING)
public class AnswerController extends AbstractEntityController<Answer> {
	protected static final String REQUEST_MAPPING = "/answer";

	private AnswerService answerService;

	public AnswerController(
			@Qualifier("securedAnswerService") final AnswerService answerService) {
		super(answerService);
		this.answerService = answerService;
	}

	@Override
	protected String getMapping() {
		return REQUEST_MAPPING;
	}
}
