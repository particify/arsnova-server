package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Room implements Entity {
	public class ContentGroup {
		private List<String> contentIds;
		private boolean autoSort;

		@JsonView({View.Persistence.class, View.Public.class})
		public List<String> getContentIds() {
			return contentIds;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setContentIds(final List<String> contentIds) {
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
	}

	public class Settings {
		private boolean questionsEnabled = true;
		private boolean slidesEnabled = true;
		private boolean commentsEnabled = true;
		private boolean flashcardsEnabled = true;
		private boolean livevoteEnabled = true;
		private boolean scoreEnabled = true;
		private boolean multipleRoundsEnabled = true;
		private boolean timerEnabled = true;
		private boolean feedbackLocked = false;

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
		public boolean isLivevoteEnabled() {
			return livevoteEnabled;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setLivevoteEnabled(boolean livevoteEnabled) {
			this.livevoteEnabled = livevoteEnabled;
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
	}

	public class Author {
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
	}

	public class PoolProperties {
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
	}

	private String id;
	private String rev;
	private Date creationTimestamp;
	private Date updateTimestamp;
	private String shortId;
	private String ownerId;
	private String name;
	private String abbreviation;
	private String description;
	private boolean closed;
	private Map<String, ContentGroup> contentGroups;
	private Settings settings;
	private Author author;
	private PoolProperties poolProperties;
	private Map<String, Map<String, ?>> extensions;
	private Map<String, String> attachments;
	private RoomStatistics statistics;

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getId() {
		return id;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public void setId(final String id) {
		this.id = id;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getRevision() {
		return rev;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public void setRevision(final String rev) {
		this.rev = rev;
	}

	@Override
	@JsonView(View.Persistence.class)
	public Date getCreationTimestamp() {
		return creationTimestamp;
	}

	@Override
	@JsonView(View.Persistence.class)
	public void setCreationTimestamp(final Date creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}

	@Override
	@JsonView(View.Persistence.class)
	public Date getUpdateTimestamp() {
		return updateTimestamp;
	}

	@Override
	@JsonView(View.Persistence.class)
	public void setUpdateTimestamp(final Date updateTimestamp) {
		this.updateTimestamp = updateTimestamp;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getShortId() {
		return shortId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setShortId(final String shortId) {
		this.shortId = shortId;
	}

	@JsonView(View.Persistence.class)
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

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isClosed() {
		return closed;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setClosed(final boolean closed) {
		this.closed = closed;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Map<String, ContentGroup> getContentGroups() {
		return contentGroups;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setContentGroups(final Map<String, ContentGroup> contentGroups) {
		this.contentGroups = contentGroups;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Settings getSettings() {
		return settings;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSettings(Settings settings) {
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
	public Map<String, Map<String, ?>> getExtensions() {
		return extensions;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setExtensions(final Map<String, Map<String, ?>> extensions) {
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
}
