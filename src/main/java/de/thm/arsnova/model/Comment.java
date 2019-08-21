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
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.View;

public class Comment extends Entity {
	@NotEmpty
	private String roomId;

	@NotEmpty
	private String creatorId;

	@NotBlank
	private String subject;

	@NotBlank
	private String body;

	@NotNull
	private Date timestamp;

	private boolean read;
	private Map<String, Map<String, ?>> extensions;

	@JsonView({View.Persistence.class, View.Public.class})
	public String getRoomId() {
		return roomId;
	}

	@JsonView(View.Persistence.class)
	public void setRoomId(final String roomId) {
		this.roomId = roomId;
	}

	@JsonView(View.Persistence.class)
	public String getCreatorId() {
		return creatorId;
	}

	@JsonView(View.Persistence.class)
	public void setCreatorId(final String creatorId) {
		this.creatorId = creatorId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getSubject() {
		return subject;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSubject(final String subject) {
		this.subject = subject;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getBody() {
		return body;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setBody(final String body) {
		this.body = body;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Date getTimestamp() {
		return timestamp;
	}

	@JsonView(View.Persistence.class)
	public void setTimestamp(final Date timestamp) {
		this.timestamp = timestamp;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isRead() {
		return read;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setRead(final boolean read) {
		this.read = read;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Map<String, Map<String, ?>> getExtensions() {
		return extensions;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setExtensions(final Map<String, Map<String, ?>> extensions) {
		this.extensions = extensions;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The following fields of <tt>LogEntry</tt> are excluded from equality checks:
	 * {@link #extensions}.
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
		final Comment comment = (Comment) o;

		return read == comment.read
				&& Objects.equals(roomId, comment.roomId)
				&& Objects.equals(creatorId, comment.creatorId)
				&& Objects.equals(subject, comment.subject)
				&& Objects.equals(body, comment.body)
				&& Objects.equals(timestamp, comment.timestamp)
				&& Objects.equals(extensions, comment.extensions);
	}

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("roomId", roomId)
				.append("creatorId", creatorId)
				.append("subject", subject)
				.append("body", body)
				.append("timestamp", timestamp)
				.append("read", read);
	}
}
