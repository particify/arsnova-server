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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.ApiModel;

import de.thm.arsnova.model.serialization.View;

/**
 * A Room (Session) a user has visited previously.
 */
@ApiModel(value = "VisitedRoom",
		description = "Visited Room (Session) entity - An entry of the Room History for the Logged In entity")
public class VisitedRoom {
	@JsonProperty("_id")
	private String id;
	private String name;
	private String keyword;

	public VisitedRoom() {
	}

	public VisitedRoom(Room s) {
		this.id = s.getId();
		this.name = s.getName();
		this.keyword = s.getKeyword();
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getId() {
		return id;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setId(final String id) {
		this.id = id;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getName() {
		return name;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setName(String name) {
		this.name = name;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getKeyword() {
		return keyword;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	@Override
	public String toString() {
		return "VisitedRoom [id=" + id + ", name=" + name + ", keyword="
				+ keyword + "]";
	}
}
