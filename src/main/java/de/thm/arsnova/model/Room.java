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

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.View;

public class Room extends Entity implements RoomIdAware {
	public static class Moderator {
		public enum Role {
			EDITING_MODERATOR,
			EXECUTIVE_MODERATOR
		}

		@NotEmpty
		private String userId;

		private Set<Role> roles;

		@JsonView({View.Persistence.class, View.Public.class})
		public String getUserId() {
			return userId;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setUserId(final String userId) {
			this.userId = userId;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public Set<Role> getRoles() {
			if (roles == null) {
				roles = new HashSet<>();
			}

			return roles;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setRoles(final Set<Role> roles) {
			this.roles = roles;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("userId", userId)
					.append("roles", roles)
					.toString();
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			final Moderator moderator = (Moderator) o;
			return Objects.equals(userId, moderator.userId)
					&& Objects.equals(roles, moderator.roles);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, roles);
		}
	}

	public static class Settings {
		private boolean feedbackLocked = true;

		@JsonView({View.Persistence.class, View.Public.class})
		public boolean isFeedbackLocked() {
			return feedbackLocked;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setFeedbackLocked(final boolean feedbackLocked) {
			this.feedbackLocked = feedbackLocked;
		}

		@Override
		public int hashCode() {
			return Objects.hash(feedbackLocked);
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			final Settings settings = (Settings) o;

			return feedbackLocked == settings.feedbackLocked;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("feedbackLocked", feedbackLocked)
					.toString();
		}
	}

	@JsonView(View.Persistence.class)
	public static class ImportMetadata {
		private String source;
		private Date timestamp;
		private String jobId;

		public String getSource() {
			return source;
		}

		public void setSource(final String source) {
			this.source = source;
		}

		public Date getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(final Date timestamp) {
			this.timestamp = timestamp;
		}

		public String getJobId() {
			return jobId;
		}

		public void setJobId(final String jobId) {
			this.jobId = jobId;
		}
	}

	@NotEmpty
	private String shortId;

	@NotEmpty
	private String ownerId;

	@NotBlank
	private String name;

	private String description;
	private String renderedDescription;
	private boolean closed;
	private boolean template;
	private String password;
	private String lmsCourseId;
	private Set<Moderator> moderators;
	private boolean moderatorsInitialized;
	private Date scheduledDeletion;

	@JsonMerge
	private Settings settings;

	private ImportMetadata importMetadata;
	private Map<String, Map<String, Object>> extensions;
	private RoomStatistics statistics;

	{
		final TextRenderingOptions options = new TextRenderingOptions();
		options.setMarkdownFeatureset(TextRenderingOptions.MarkdownFeatureset.EXTENDED);
		this.addRenderingMapping(
				this::getDescription,
				this::setRenderedDescription,
				options);
	}

	public Room() {

	}

	/**
	 * Copying constructor which adopts most of the original room's properties
	 * which are not used to store relations to other data.
	 */
	public Room(final Room room) {
		this.name = room.name;
		this.description = room.description;
		this.closed = room.closed;
		this.settings = room.settings;
		this.extensions = room.extensions;
	}

	@Override
	public String getRoomId() {
		return id;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getShortId() {
		return shortId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setShortId(final String shortId) {
		this.shortId = shortId;
	}

	@JsonView({View.Persistence.class, View.Admin.class})
	public String getOwnerId() {
		return ownerId;
	}

	@JsonView(View.Persistence.class)
	public void setOwnerId(final String ownerId) {
		this.ownerId = ownerId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getName() {
		return name;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setName(final String name) {
		this.name = name;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getDescription() {
		return description;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setDescription(final String description) {
		this.description = description;
	}

	@JsonView(View.Public.class)
	public String getRenderedDescription() {
		return renderedDescription;
	}

	public void setRenderedDescription(final String renderedDescription) {
		this.renderedDescription = renderedDescription;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isClosed() {
		return closed;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setClosed(final boolean closed) {
		this.closed = closed;
	}

	@JsonView(View.Persistence.class)
	public boolean isTemplate() {
		return template;
	}

	@JsonView(View.Persistence.class)
	public void setTemplate(final boolean template) {
		this.template = template;
	}

	@JsonView(View.Persistence.class)
	public String getPassword() {
		return password;
	}

	@JsonView(View.Persistence.class)
	public void setPassword(final String password) {
		this.password = password == null || password.isBlank() ? null : password;
	}

	@JsonView(View.Public.class)
	public boolean isPasswordProtected() {
		return this.password != null;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getLmsCourseId() {
		return lmsCourseId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setLmsCourseId(final String lmsCourseId) {
		this.lmsCourseId = lmsCourseId;
	}

	@JsonView({View.Persistence.class, View.Admin.class})
	public Set<Moderator> getModerators() {
		if (moderators == null) {
			moderators = new HashSet<>();
		}

		return moderators;
	}

	@JsonView(View.Persistence.class)
	public void setModerators(final Set<Moderator> moderators) {
		this.moderators = moderators;
		moderatorsInitialized = true;
	}

	public boolean isModeratorsInitialized() {
		return moderatorsInitialized;
	}

	@JsonView(View.Persistence.class)
	public Date getScheduledDeletion() {
		return scheduledDeletion;
	}

	@JsonView(View.Persistence.class)
	public void setScheduledDeletion(final Date scheduledDeletion) {
		this.scheduledDeletion = scheduledDeletion;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Settings getSettings() {
		if (settings == null) {
			settings = new Settings();
		}

		return settings;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSettings(final Settings settings) {
		this.settings = settings;
	}

	@JsonView(View.Persistence.class)
	public ImportMetadata getImportMetadata() {
		return importMetadata;
	}

	@JsonView(View.Persistence.class)
	public void setImportMetadata(final ImportMetadata importMetadata) {
		this.importMetadata = importMetadata;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Map<String, Map<String, Object>> getExtensions() {
		return extensions;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setExtensions(final Map<String, Map<String, Object>> extensions) {
		this.extensions = extensions;
	}

	@JsonView(View.Public.class)
	public RoomStatistics getStatistics() {
		return statistics;
	}

	public void setStatistics(final RoomStatistics statistics) {
		this.statistics = statistics;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The following fields of <tt>Room</tt> are excluded from equality checks:
	 * {@link #settings}, {@link #extensions}, {@link #statistics}.
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
		final Room room = (Room) o;

		return closed == room.closed
				&& Objects.equals(shortId, room.shortId)
				&& Objects.equals(ownerId, room.ownerId)
				&& Objects.equals(name, room.name)
				&& Objects.equals(description, room.description);
	}

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("shortId", shortId)
				.append("ownerId", ownerId)
				.append("name", name)
				.append("description", description)
				.append("closed", closed)
				.append("passwordProtected", isPasswordProtected())
				.append("moderatorsInitialized", moderatorsInitialized)
				.append("moderators", moderators)
				.append("settings", settings)
				.append("statistics", statistics);
	}
}
