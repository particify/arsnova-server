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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.FormatContentTypeIdResolver;
import de.thm.arsnova.model.serialization.View;

@JsonTypeInfo(
		use = JsonTypeInfo.Id.CUSTOM,
		property = "format",
		visible = true,
		defaultImpl = Content.class
)
@JsonTypeIdResolver(FormatContentTypeIdResolver.class)
public class Content extends Entity {
	public enum Format {
		CHOICE,
		BINARY,
		SCALE,
		NUMBER,
		TEXT,
		GRID
	}

	public static class State {
		private int round = 1;
		private Date roundEndTimestamp;
		private boolean visible = true;
		private boolean solutionVisible = false;
		private boolean responsesEnabled = true;
		private boolean responsesVisible = false;

		@JsonView({View.Persistence.class, View.Public.class})
		public int getRound() {
			return round;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setRound(final int round) {
			this.round = round;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public Date getRoundEndTimestamp() {
			return roundEndTimestamp;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setRoundEndTimestamp(Date roundEndTimestamp) {
			this.roundEndTimestamp = roundEndTimestamp;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public boolean isVisible() {
			return visible;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public boolean isSolutionVisible() {
			return solutionVisible;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setSolutionVisible(final boolean solutionVisible) {
			this.solutionVisible = solutionVisible;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setVisible(final boolean visible) {
			this.visible = visible;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public boolean isResponsesEnabled() {
			return responsesEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setResponsesEnabled(final boolean responsesEnabled) {
			this.responsesEnabled = responsesEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public boolean isResponsesVisible() {
			return responsesVisible;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setResponsesVisible(final boolean responsesVisible) {
			this.responsesVisible = responsesVisible;
		}

		@Override
		public int hashCode() {
			return Objects.hash(round, roundEndTimestamp, visible, solutionVisible, responsesEnabled, responsesVisible);
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (!super.equals(o)) {
				return false;
			}
			final State state = (State) o;

			return round == state.round &&
					visible == state.visible &&
					solutionVisible == state.solutionVisible &&
					responsesEnabled == state.responsesEnabled &&
					responsesVisible == state.responsesVisible &&
					Objects.equals(roundEndTimestamp, state.roundEndTimestamp);
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("round", round)
					.append("roundEndTimestamp", roundEndTimestamp)
					.append("visible", visible)
					.append("solutionVisible", solutionVisible)
					.append("responsesEnabled", responsesEnabled)
					.append("responsesVisible", responsesVisible)
					.toString();
		}
	}

	private String roomId;
	private String subject;
	private String body;
	private Format format;
	private Set<String> groups;
	private boolean abstentionsAllowed;
	private State state;
	private Date timestamp;
	private Map<String, Map<String, ?>> extensions;
	private Map<String, String> attachments;

	@JsonView({View.Persistence.class, View.Public.class})
	public String getRoomId() {
		return roomId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setRoomId(final String roomId) {
		this.roomId = roomId;
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
	public Format getFormat() {
		return format;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setFormat(final Format format) {
		this.format = format;
	}

	@JsonView(View.Public.class)
	public Set<String> getGroups() {
		if (groups == null) {
			groups = new HashSet<>();
		}

		return groups;
	}

	/* Content groups are persisted in the Room */
	@JsonView(View.Public.class)
	public void setGroups(final Set<String> groups) {
		this.groups = groups;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public State getState() {
		return state != null ? state : (state = new State());
	}

	public void resetState() {
		this.state = new State();
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setState(final State state) {
		this.state = state;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Date getTimestamp() {
		return timestamp;
	}

	@JsonView(View.Persistence.class)
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Map<String, Map<String, ?>> getExtensions() {
		return extensions;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setExtensions(Map<String, Map<String, ?>> extensions) {
		this.extensions = extensions;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Map<String, String> getAttachments() {
		return attachments;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAttachments(final Map<String, String> attachments) {
		this.attachments = attachments;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isAbstentionsAllowed() {
		return abstentionsAllowed;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAbstentionsAllowed(final boolean abstentionsAllowed) {
		this.abstentionsAllowed = abstentionsAllowed;
	}

	@JsonView(View.Persistence.class)
	@Override
	public Class<? extends Entity> getType() {
		return Content.class;
	}

	/**
	 * {@inheritDoc}
	 *
	 * The following fields of <tt>LogEntry</tt> are excluded from equality checks:
	 * {@link #state}, {@link #extensions}, {@link #attachments}.
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!super.equals(o)) {
			return false;
		}
		final Content content = (Content) o;

		return Objects.equals(roomId, content.roomId) &&
				Objects.equals(subject, content.subject) &&
				Objects.equals(body, content.body) &&
				format == content.format &&
				Objects.equals(groups, content.groups) &&
				Objects.equals(timestamp, content.timestamp);
	}

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("roomId", roomId)
				.append("subject", subject)
				.append("body", body)
				.append("format", format)
				.append("groups", groups)
				.append("abstentionsAllowed", abstentionsAllowed)
				.append("state", state)
				.append("timestamp", timestamp)
				.append("attachments", attachments);
	}
}
