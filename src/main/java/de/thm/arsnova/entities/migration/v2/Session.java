/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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
package de.thm.arsnova.entities.migration.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.Entity;
import de.thm.arsnova.entities.ScoreOptions;
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.serialization.View;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Represents an ARSnova session.
 */
@ApiModel(value = "session", description = "the session entity")
public class Session implements Entity {
	private String id;
	private String rev;
	private String name;
	private String shortName;
	private String keyword;
	private String creator;
	private boolean active;
	private long lastOwnerActivity;
	private String courseType;
	private String courseId;
	private long creationTime;
	private ScoreOptions learningProgressOptions = new ScoreOptions();
	private SessionFeature features = new SessionFeature();

	private String ppAuthorName;
	private String ppAuthorMail;
	private String ppUniversity;
	private String ppLogo;
	private String ppSubject;
	private String ppLicense;
	private String ppDescription;
	private String ppFaculty;
	private String ppLevel;
	private String sessionType;
	private boolean feedbackLock;
	private boolean flipFlashcards;

	@JsonView({View.Persistence.class, View.Public.class})
	public String getId() {
		return id;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setId(final String id) {
		this.id = id;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getRevision() {
		return rev;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setRevision(final String rev) {
		this.rev = rev;
	}

	@ApiModelProperty(required = true, value = "the name")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getName() {
		return name;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setName(final String name) {
		this.name = name;
	}

	@ApiModelProperty(required = true, value = "the short name")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getShortName() {
		return shortName;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setShortName(final String shortName) {
		this.shortName = shortName;
	}

	@ApiModelProperty(required = true, value = "the keyword")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getKeyword() {
		return keyword;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setKeyword(final String keyword) {
		this.keyword = keyword;
	}

	@ApiModelProperty(required = true, value = "the session creator")
	@JsonView(View.Persistence.class)
	public String getCreator() {
		return creator;
	}

	@JsonView(View.Persistence.class)
	public void setCreator(final String creator) {
		this.creator = creator;
	}

	@ApiModelProperty(required = true, value = "true for active session")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isActive() {
		return active;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setActive(final boolean active) {
		this.active = active;
	}

	@ApiModelProperty(required = true, value = "timestamp from the last activity of the owner")
	@JsonView(View.Persistence.class)
	public long getLastOwnerActivity() {
		return lastOwnerActivity;
	}

	@JsonView(View.Persistence.class)
	public void setLastOwnerActivity(final long lastOwnerActivity) {
		this.lastOwnerActivity = lastOwnerActivity;
	}

	public boolean isCreator(final UserAuthentication user) {
		return user.getUsername().equals(creator);
	}

	@ApiModelProperty(required = true, value = "the source the course comes from (example: moodle)")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getCourseType() {
		return courseType;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setCourseType(final String courseType) {
		this.courseType = courseType;
	}

	@ApiModelProperty(required = true, value = "the course ID")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getCourseId() {
		return courseId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setCourseId(final String courseId) {
		this.courseId = courseId;
	}

	@JsonIgnore
	public boolean isCourseSession() {
		return getCourseId() != null && !getCourseId().isEmpty();
	}

	@ApiModelProperty(required = true, value = "creation timestamp")
	@JsonView({View.Persistence.class, View.Public.class})
	public long getCreationTime() {
		return creationTime;
	}

	@JsonView(View.Persistence.class)
	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	@ApiModelProperty(required = true, value = "the score options")
	@JsonView({View.Persistence.class, View.Public.class})
	public ScoreOptions getLearningProgressOptions() {
		return learningProgressOptions;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setLearningProgressOptions(ScoreOptions learningProgressOptions) {
		this.learningProgressOptions = learningProgressOptions;
	}

	@ApiModelProperty(required = true, value = "the enabled features (e.g. feedback, interposed, learning Progress, lecture)")
	@JsonView({View.Persistence.class, View.Public.class})
	public SessionFeature getFeatures() {
		return features;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setFeatures(SessionFeature features) {
		this.features = features;
	}

	@ApiModelProperty(required = true, value = "the public pool author name")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getPpAuthorName() {
		return ppAuthorName;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPpAuthorName(final String ppAuthorName) {
		this.ppAuthorName = ppAuthorName;
	}

	@ApiModelProperty(required = true, value = "the public pool author email")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getPpAuthorMail() {
		return ppAuthorMail;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPpAuthorMail(final String ppAuthorMail) {
		this.ppAuthorMail = ppAuthorMail;
	}

	@ApiModelProperty(required = true, value = "the public pool university")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getPpUniversity() {
		return ppUniversity;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPpUniversity(final String ppUniversity) {
		this.ppUniversity = ppUniversity;
	}

	@ApiModelProperty(required = true, value = "the public pool logo")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getPpLogo() {
		return ppLogo;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPpLogo(final String ppLogo) {
		this.ppLogo = ppLogo;
	}

	@ApiModelProperty(required = true, value = "used to display subject")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getPpSubject() {
		return ppSubject;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPpSubject(final String ppSubject) {
		this.ppSubject = ppSubject;
	}

	@ApiModelProperty(required = true, value = "the public pool license")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getPpLicense() {
		return ppLicense;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPpLicense(final String ppLicense) {
		this.ppLicense = ppLicense;
	}

	@ApiModelProperty(required = true, value = "the public pool description")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getPpDescription() {
		return ppDescription;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPpDescription(final String ppDescription) {
		this.ppDescription = ppDescription;
	}

	@ApiModelProperty(required = true, value = "the public pool faculty")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getPpFaculty() {
		return ppFaculty;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPpFaculty(final String ppFaculty) {
		this.ppFaculty = ppFaculty;
	}

	@ApiModelProperty(required = true, value = "the public pool level")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getPpLevel() {
		return ppLevel;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPpLevel(final String ppLevel) {
		this.ppLevel = ppLevel;
	}

	@ApiModelProperty(required = true, value = "the session type")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getSessionType() {
		return sessionType;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSessionType(final String sessionType) {
		this.sessionType = sessionType;
	}

	@ApiModelProperty(required = true, value = "the feedback lock status")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean getFeedbackLock() {
		return feedbackLock;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setFeedbackLock(Boolean lock) {
		this.feedbackLock = lock;
	}

	@ApiModelProperty(required = true, value = "the flashcard flip condition")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean getFlipFlashcards() {
		return flipFlashcards;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setFlipFlashcards(Boolean flip) {
		this.flipFlashcards = flip;
	}

	@Override
	public String toString() {
		return "Session [keyword=" + keyword + ", type=" + getType() + ", creator=" + creator + "]";
	}

	@Override
	public int hashCode() {
		// See http://stackoverflow.com/a/113600
		final int theAnswer = 42;
		final int theOthers = 37;

		return theOthers * theAnswer + this.keyword.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !obj.getClass().equals(this.getClass())) {
			return false;
		}
		Session other = (Session) obj;
		return this.keyword.equals(other.keyword);
	}

}
