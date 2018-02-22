/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * A session's settings regarding the calculation of the score.
 */
@ApiModel(value = "score options", description = "the score entity")
public class ScoreOptions implements Serializable {

	private String type = "questions";

	private String questionVariant = "";

	public ScoreOptions(ScoreOptions scoreOptions) {
		this();
		this.type = scoreOptions.getType();
		this.questionVariant = scoreOptions.getQuestionVariant();
	}

	public ScoreOptions() { }

	@ApiModelProperty(required = true, value = "the type")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getType() {
		return type;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setType(String type) {
		this.type = type;
	}

	@ApiModelProperty(required = true, value = "either lecture or preparation")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getQuestionVariant() {
		return questionVariant;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setQuestionVariant(String questionVariant) {
		this.questionVariant = questionVariant;
	}
}
