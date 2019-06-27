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

package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Objects;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.View;

public class Attachment extends Entity {
	private String mediaType;
	private long size;
	private String originalSourceUrl;
	private String storageLocation;

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getId() {
		return id;
	}

	@Override
	@JsonView(View.Persistence.class)
	public void setId(final String id) {
		this.id = id;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getRevision() {
		return rev;
	}

	@Override
	@JsonView(View.Public.class)
	public void setRevision(final String rev) {
		this.rev = rev;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getMediaType() {
		return mediaType;
	}

	@JsonView(View.Persistence.class)
	public void setMediaType(final String mediaType) {
		this.mediaType = mediaType;
	}

	@JsonView(View.Persistence.class)
	public long getSize() {
		return size;
	}

	@JsonView(View.Persistence.class)
	public void setSize(final long size) {
		this.size = size;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getOriginalSourceUrl() {
		return originalSourceUrl;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setOriginalSourceUrl(final String originalSourceUrl) {
		this.originalSourceUrl = originalSourceUrl;
	}

	@JsonView(View.Persistence.class)
	public String getStorageLocation() {
		return storageLocation;
	}

	@JsonView(View.Persistence.class)
	public void setStorageLocation(final String storageLocation) {
		this.storageLocation = storageLocation;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * All fields of <tt>Attachment</tt> are included in equality checks.
	 * </p>
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!super.equals(o)) {
			return false;
		}
		final Attachment that = (Attachment) o;

		return size == that.size
				&& Objects.equals(mediaType, that.mediaType)
				&& Objects.equals(originalSourceUrl, that.originalSourceUrl)
				&& Objects.equals(storageLocation, that.storageLocation);
	}

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("mediaType", mediaType)
				.append("size", size)
				.append("originalSourceUrl", originalSourceUrl)
				.append("storageLocation", storageLocation);
	}
}
