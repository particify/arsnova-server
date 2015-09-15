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
 * This class represents an answer option of a question.
 */
@ApiModel(value = "session/answer", description = "the Possible Answer entity")
public class PossibleAnswer {

	private String id;
	private String text;
	private boolean correct;
	private int value;

	@ApiModelProperty(required = true, value = "the id")
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@ApiModelProperty(required = true, value = "the text")
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@ApiModelProperty(required = true, value = "true for a correct answer")
	public boolean isCorrect() {
		return correct;
	}

	public void setCorrect(boolean correct) {
		this.correct = correct;
	}

	@ApiModelProperty(required = true, value = "the value")
	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "PossibleAnswer [id=" + id + ", text=" + text + ", correct=" + correct + "]";
	}
}
