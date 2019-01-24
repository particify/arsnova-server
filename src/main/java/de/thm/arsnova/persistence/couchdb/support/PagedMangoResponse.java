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
package de.thm.arsnova.persistence.couchdb.support;

import java.util.List;

/**
 * Contains the entities of the response and the bookmark to query the next page.
 *
 * @param <T>
 * @author Daniel Gerhardt
 */
public class PagedMangoResponse<T> {
	private List<T> entities;
	private String bookmark;

	public PagedMangoResponse(final List<T> entities, final String bookmark) {
		this.entities = entities;
		this.bookmark = bookmark;
	}

	public List<T> getEntities() {
		return entities;
	}

	public String getBookmark() {
		return bookmark;
	}
}
