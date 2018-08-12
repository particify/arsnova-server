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
package de.thm.arsnova.websocket.message;

/**
 * Represents a question.
 */
public class Content {

	private final String _id;
	private final String variant;

	public Content(de.thm.arsnova.model.Content content) {
		this._id = content.getId();
		/* FIXME: Message does not support content with multiple groups */
		this.variant = content.getGroups().toArray(new String[1])[0];
	}

	public String get_id() {
		return _id;
	}

	public String getVariant() {
		return variant;
	}
}
