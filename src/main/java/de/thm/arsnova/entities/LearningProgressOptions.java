/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
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
package de.thm.arsnova.entities;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
/**
 * A session's settings regarding the calculation of the learning progress.
 */
@ApiModel(value = "learning progress options", description = "the learning progress entity")
public class LearningProgressOptions {

	private String type = "questions";

	private String questionVariant = "";

	public LearningProgressOptions(LearningProgressOptions learningProgressOptions) {
		this();
		this.type = learningProgressOptions.getType();
		this.questionVariant = learningProgressOptions.getQuestionVariant();
	}

	public LearningProgressOptions() {}

	@ApiModelProperty(required = true, value = "the type")
	public String getType() {
		return type;
	}

	public void setType(String learningProgressType) {
		this.type = learningProgressType;
	}

	@ApiModelProperty(required = true, value = "either lecture or preparation")
	public String getQuestionVariant() {
		return questionVariant;
	}

	public void setQuestionVariant(String questionVariant) {
		this.questionVariant = questionVariant;
	}
}
