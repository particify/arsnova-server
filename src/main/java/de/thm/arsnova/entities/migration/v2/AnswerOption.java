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
package de.thm.arsnova.entities.migration.v2;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * Represents an Answer (Possible Answer) of Content.
 */
@ApiModel(value = "AnswerOption", description = "Answer Option (Possible Answer) entity")
public class AnswerOption implements Serializable {

	private String id;
	private String text;
	private boolean correct;
	private int value;

	@ApiModelProperty(required = true, value = "the ID")
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@ApiModelProperty(required = true, value = "the text")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getText() {
		return text;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setText(String text) {
		this.text = text;
	}

	@ApiModelProperty(required = true, value = "true for a correct answer")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isCorrect() {
		return correct;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setCorrect(boolean correct) {
		this.correct = correct;
	}

	@ApiModelProperty(required = true, value = "the value")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getValue() {
		return value;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setValue(int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "AnswerOption [id=" + id + ", text=" + text + ", correct=" + correct + "]";
	}
}
