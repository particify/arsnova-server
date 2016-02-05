/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2016 The ARSnova Team
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
package de.thm.arsnova.socket.message;

/**
 * Represents a question.
 */
public class Question {

	private final String _id;
	private final String variant;

	public Question(de.thm.arsnova.entities.Question question) {
		this._id = question.get_id();
		this.variant = question.getQuestionVariant();
	}

	public String get_id() {
		return _id;
	}

	public String getVariant() {
		return variant;
	}
}
