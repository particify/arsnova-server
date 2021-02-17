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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.View;

public class ContentGroup extends Entity {
	@NotEmpty
	private String roomId;

	@NotBlank
	private String name;

	private List<String> contentIds;
	private boolean published;
	private int firstPublishedIndex = -1;
	private int lastPublishedIndex = -1;
	private boolean statisticsPublished = true;
	private boolean correctOptionsPublished = true;

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
	public List<String> getContentIds() {
		if (contentIds == null) {
			contentIds = new ArrayList<>();
		}

		return contentIds;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setContentIds(final List<String> contentIds) {
		this.contentIds = contentIds;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isPublished() {
		return published;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPublished(final boolean published) {
		this.published = published;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getFirstPublishedIndex() {
		return firstPublishedIndex;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setFirstPublishedIndex(final int firstPublishedIndex) {
		this.firstPublishedIndex = firstPublishedIndex;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getLastPublishedIndex() {
		return lastPublishedIndex;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setLastPublishedIndex(final int lastPublishedIndex) {
		this.lastPublishedIndex = lastPublishedIndex;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isStatisticsPublished() {
		return statisticsPublished;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setStatisticsPublished(final boolean statisticsPublished) {
		this.statisticsPublished = statisticsPublished;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isCorrectOptionsPublished() {
		return correctOptionsPublished;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setCorrectOptionsPublished(final boolean correctOptionsPublished) {
		this.correctOptionsPublished = correctOptionsPublished;
	}

	public boolean isContentPublished(final String contentId) {
		final int i = contentIds.indexOf(contentId);
		return i > -1 && (firstPublishedIndex == -1 || i >= firstPublishedIndex)
				&& (lastPublishedIndex == -1 || i <= lastPublishedIndex);
	}

	public boolean containsContent(final String contentId) {
		return contentIds.contains(contentId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, contentIds);
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

		return Objects.equals(name, that.name)
			&& Objects.equals(contentIds, that.contentIds);
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("name", name)
				.append("contentIds", contentIds)
				.append("published", published)
				.append("firstPublishedIndex", firstPublishedIndex)
				.append("lastPublishedIndex", lastPublishedIndex)
				.append("statisticsPublished", statisticsPublished)
				.append("correctOptionsPublished", correctOptionsPublished)
				.toString();
	}
}
