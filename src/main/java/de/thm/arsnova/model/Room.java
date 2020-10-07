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
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.View;

public class Room extends Entity {
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
		private boolean questionsEnabled = true;
		private boolean slidesEnabled = true;
		private boolean commentsEnabled = true;
		private boolean flashcardsEnabled = true;
		private boolean quickSurveyEnabled = true;
		private boolean quickFeedbackEnabled = true;
		private boolean scoreEnabled = true;
		private boolean multipleRoundsEnabled = true;
		private boolean timerEnabled = true;
		private boolean feedbackLocked = true;

		@JsonView({View.Persistence.class, View.Public.class})
		public boolean isQuestionsEnabled() {
			return questionsEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setQuestionsEnabled(final boolean questionsEnabled) {
			this.questionsEnabled = questionsEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public boolean isSlidesEnabled() {
			return slidesEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setSlidesEnabled(final boolean slidesEnabled) {
			this.slidesEnabled = slidesEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public boolean isCommentsEnabled() {
			return commentsEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setCommentsEnabled(final boolean commentsEnabled) {
			this.commentsEnabled = commentsEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public boolean isFlashcardsEnabled() {
			return flashcardsEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setFlashcardsEnabled(final boolean flashcardsEnabled) {
			this.flashcardsEnabled = flashcardsEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public boolean isQuickSurveyEnabled() {
			return quickSurveyEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setQuickSurveyEnabled(final boolean quickSurveyEnabled) {
			this.quickSurveyEnabled = quickSurveyEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public boolean isQuickFeedbackEnabled() {
			return quickFeedbackEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setQuickFeedbackEnabled(final boolean quickFeedbackEnabled) {
			this.quickFeedbackEnabled = quickFeedbackEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public boolean isScoreEnabled() {
			return scoreEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setScoreEnabled(final boolean scoreEnabled) {
			this.scoreEnabled = scoreEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public boolean isMultipleRoundsEnabled() {
			return multipleRoundsEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setMultipleRoundsEnabled(final boolean multipleRoundsEnabled) {
			this.multipleRoundsEnabled = multipleRoundsEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public boolean isTimerEnabled() {
			return timerEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setTimerEnabled(final boolean timerEnabled) {
			this.timerEnabled = timerEnabled;
		}

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
			return Objects.hash(
					questionsEnabled, slidesEnabled, commentsEnabled, flashcardsEnabled,
					quickSurveyEnabled, quickFeedbackEnabled, scoreEnabled, multipleRoundsEnabled,
					timerEnabled, feedbackLocked);
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

			return questionsEnabled == settings.questionsEnabled
					&& slidesEnabled == settings.slidesEnabled
					&& commentsEnabled == settings.commentsEnabled
					&& flashcardsEnabled == settings.flashcardsEnabled
					&& quickSurveyEnabled == settings.quickSurveyEnabled
					&& quickFeedbackEnabled == settings.quickFeedbackEnabled
					&& scoreEnabled == settings.scoreEnabled
					&& multipleRoundsEnabled == settings.multipleRoundsEnabled
					&& timerEnabled == settings.timerEnabled
					&& feedbackLocked == settings.feedbackLocked;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("questionsEnabled", questionsEnabled)
					.append("slidesEnabled", slidesEnabled)
					.append("commentsEnabled", commentsEnabled)
					.append("flashcardsEnabled", flashcardsEnabled)
					.append("quickSurveyEnabled", quickSurveyEnabled)
					.append("quickFeedbackEnabled", quickFeedbackEnabled)
					.append("scoreEnabled", scoreEnabled)
					.append("multipleRoundsEnabled", multipleRoundsEnabled)
					.append("timerEnabled", timerEnabled)
					.append("feedbackLocked", feedbackLocked)
					.toString();
		}
	}

	public static class Author {
		private String name;
		private String mail;
		private String organizationName;
		private String organizationLogo;
		private String organizationUnit;

		@JsonView({View.Persistence.class, View.Public.class})
		public String getName() {
			return name;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setName(final String name) {
			this.name = name;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public String getMail() {
			return mail;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setMail(final String mail) {
			this.mail = mail;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public String getOrganizationName() {
			return organizationName;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setOrganizationName(final String organizationName) {
			this.organizationName = organizationName;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public String getOrganizationLogo() {
			return organizationLogo;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setOrganizationLogo(final String organizationLogo) {
			this.organizationLogo = organizationLogo;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public String getOrganizationUnit() {
			return organizationUnit;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setOrganizationUnit(final String organizationUnit) {
			this.organizationUnit = organizationUnit;
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, mail, organizationName, organizationLogo, organizationUnit);
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			final Author author = (Author) o;

			return Objects.equals(name, author.name)
					&& Objects.equals(mail, author.mail)
					&& Objects.equals(organizationName, author.organizationName)
					&& Objects.equals(organizationLogo, author.organizationLogo)
					&& Objects.equals(organizationUnit, author.organizationUnit);
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("name", name)
					.append("mail", mail)
					.append("organizationName", organizationName)
					.append("organizationLogo", organizationLogo)
					.append("organizationUnit", organizationUnit)
					.toString();
		}
	}

	public static class PoolProperties {
		private String category;
		private String level;
		private String license;

		@JsonView({View.Persistence.class, View.Public.class})
		public String getCategory() {
			return category;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setCategory(final String category) {
			this.category = category;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public String getLevel() {
			return level;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setLevel(final String level) {
			this.level = level;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public String getLicense() {
			return license;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setLicense(final String license) {
			this.license = license;
		}

		@Override
		public int hashCode() {
			return Objects.hash(category, level, license);
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			final PoolProperties that = (PoolProperties) o;

			return Objects.equals(category, that.category)
					&& Objects.equals(level, that.level)
					&& Objects.equals(license, that.license);
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("category", category)
					.append("level", level)
					.append("license", license)
					.toString();
		}
	}

	@NotEmpty
	private String shortId;

	@NotEmpty
	private String ownerId;

	@NotBlank
	private String name;

	@NotBlank
	private String abbreviation;

	private String description;
	private String renderedDescription;
	private boolean closed;
	private Set<Moderator> moderators;
	private boolean moderatorsInitialized;
	private Settings settings;
	private Author author;
	private PoolProperties poolProperties;
	private Map<String, Map<String, Object>> extensions;
	private Map<String, String> attachments;
	private RoomStatistics statistics;

	{
		final TextRenderingOptions options = new TextRenderingOptions();
		options.setMarkdownFeatureset(TextRenderingOptions.MarkdownFeatureset.EXTENDED);
		this.addRenderingMapping(
				this::getDescription,
				this::setRenderedDescription,
				options);
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
	public String getAbbreviation() {
		return abbreviation;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAbbreviation(final String abbreviation) {
		this.abbreviation = abbreviation;
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

	@JsonView({View.Persistence.class, View.Public.class})
	public Author getAuthor() {
		return author;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAuthor(final Author author) {
		this.author = author;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public PoolProperties getPoolProperties() {
		return poolProperties;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPoolProperties(final PoolProperties poolProperties) {
		this.poolProperties = poolProperties;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Map<String, Map<String, Object>> getExtensions() {
		return extensions;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setExtensions(final Map<String, Map<String, Object>> extensions) {
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
	 * {@link #settings}, {@link #author}, {@link #poolProperties}, {@link #extensions}, {@link #attachments},
	 * {@link #statistics}.
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
				&& Objects.equals(abbreviation, room.abbreviation)
				&& Objects.equals(description, room.description);
	}

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("shortId", shortId)
				.append("ownerId", ownerId)
				.append("name", name)
				.append("abbreviation", abbreviation)
				.append("description", description)
				.append("closed", closed)
				.append("moderatorsInitialized", moderatorsInitialized)
				.append("moderators", moderators)
				.append("settings", settings)
				.append("author", author)
				.append("poolProperties", poolProperties)
				.append("attachments", attachments)
				.append("statistics", statistics);
	}
}
