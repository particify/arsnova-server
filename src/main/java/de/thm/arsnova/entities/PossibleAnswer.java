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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This class represents an answer option of a question.
 */
public class PossibleAnswer {

	private String id;
	private String text;
	private boolean correct;
	private int value;

	public String getId() {
		return this.id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

	@JsonIgnore
	public boolean isCorrect() {
		return correct;
	}

	@JsonIgnore
	public void setCorrect(final boolean correct) {
		this.correct = correct;
	}

	public int getValue() {
		return value;
	}

	public void setValue(final int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "PossibleAnswer [id=" + id + ", text=" + text + ", correct=" + correct + "]";
	}
}
