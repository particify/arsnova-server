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

package de.thm.arsnova.model.migration.v2;

import com.fasterxml.jackson.annotation.JsonView;
import java.io.Serializable;

import de.thm.arsnova.model.serialization.View;

/**
 * Represents an Answer (Possible Answer) of Content.
 */
public class AnswerOption implements Serializable {

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

	@JsonView({View.Persistence.class, View.Public.class})
	public String getText() {
		return text;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setText(final String text) {
		this.text = text;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isCorrect() {
		return correct;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setCorrect(final boolean correct) {
		this.correct = correct;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getValue() {
		return value;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setValue(final int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "AnswerOption [id=" + id + ", text=" + text + ", correct=" + correct + "]";
	}
}
