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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.	 If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.View;

public class ContentGroup extends Entity {
	@NotEmpty
	private String roomId;

	@NotBlank
	private String name;

	private Set<String> contentIds;
	private boolean autoSort;

	public ContentGroup() {

	}

	public ContentGroup(final String roomId, final String name) {
		this.roomId = roomId;
		this.name = name;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getRoomId() {
		return roomId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setRoomId(final String roomId) {
		this.roomId = roomId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getName() {
		return this.name;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setName(final String name) {
		this.name = name;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Set<String> getContentIds() {
		if (contentIds == null) {
			contentIds = new LinkedHashSet<>();
		}

		return contentIds;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	@JsonDeserialize(as = LinkedHashSet.class)
	public void setContentIds(final Set<String> contentIds) {
		this.contentIds = contentIds;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isAutoSort() {
		return autoSort;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAutoSort(final boolean autoSort) {
		this.autoSort = autoSort;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, contentIds, autoSort);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ContentGroup that = (ContentGroup) o;

		return autoSort == that.autoSort
			&& Objects.equals(name, that.name)
			&& Objects.equals(contentIds, that.contentIds);
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
			.append("name", name)
			.append("contentIds", contentIds)
			.append("autoSort", autoSort)
			.toString();
	}
}
