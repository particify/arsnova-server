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

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;

import de.thm.arsnova.model.serialization.View;

/**
 * Summary information of a specific Room. For example, this is used to display list entries of a user's Rooms as well
 * as a user's Room History (Visited Rooms).
 */
@ApiModel(value = "RoomInfo", description = "Room (Session) Info entity")
public class RoomInfo {

	private String name;
	private String shortName;
	private String keyword;
	private boolean active;
	private String courseType;
	private long creationTime;
	private String sessionType;
	private String ppLevel;
	private String ppSubject;

	private int numQuestions;
	private int numAnswers;
	private int numComments;
	private int numUnreadComments;
	private int numUnanswered;

	public RoomInfo(final Room room) {
		this.name = room.getName();
		this.shortName = room.getShortName();
		this.keyword = room.getKeyword();
		this.active = room.isActive();
		this.courseType = room.getCourseType();
		this.creationTime = room.getCreationTime();
		this.sessionType = room.getSessionType();
		this.ppLevel = room.getPpLevel();
		this.ppSubject = room.getPpSubject();
	}

	public RoomInfo() {

	}

	public static List<RoomInfo> fromSessionList(final List<Room> sessions) {
		final List<RoomInfo> infos = new ArrayList<>();
		for (final Room s : sessions) {
			infos.add(new RoomInfo(s));
		}
		return infos;
	}

	@ApiModelProperty(required = true, value = "the name")
	@JsonView(View.Public.class)
	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@ApiModelProperty(required = true, value = "the short name")
	@JsonView(View.Public.class)
	public String getShortName() {
		return shortName;
	}

	public void setShortName(final String shortName) {
		this.shortName = shortName;
	}

	@ApiModelProperty(required = true, value = "the keyword")
	@JsonView(View.Public.class)
	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(final String keyword) {
		this.keyword = keyword;
	}

	@ApiModelProperty(required = true, value = "true for active")
	@JsonView(View.Public.class)
	public boolean isActive() {
		return active;
	}

	public void setActive(final boolean active) {
		this.active = active;
	}

	@ApiModelProperty(required = true, value = "the source the course comes from (example: moodle)")
	@JsonView(View.Public.class)
	public String getCourseType() {
		return courseType;
	}

	public void setCourseType(final String courseType) {
		this.courseType = courseType;
	}

	@ApiModelProperty(required = true, value = "the session type")
	@JsonView(View.Public.class)
	public String getSessionType() {
		return sessionType;
	}

	public void setSessionType(final String sessionType) {
		this.sessionType = sessionType;
	}

	@ApiModelProperty(required = true, value = "used to display level")
	@JsonView(View.Public.class)
	public String getPpLevel() {
		return ppLevel;
	}

	public void setPpLevel(final String ppLevel) {
		this.ppLevel = ppLevel;
	}

	@ApiModelProperty(required = true, value = "the public pool subject")
	@JsonView(View.Public.class)
	public String getPpSubject() {
		return ppSubject;
	}

	public void setPpSubject(final String ppSubject) {
		this.ppSubject = ppSubject;
	}

	@ApiModelProperty(required = true, value = "the number of questions")
	@JsonView(View.Public.class)
	public int getNumQuestions() {
		return numQuestions;
	}

	public void setNumQuestions(final int numQuestions) {
		this.numQuestions = numQuestions;
	}

	@ApiModelProperty(required = true, value = "the number of answers")
	@JsonView(View.Public.class)
	public int getNumAnswers() {
		return numAnswers;
	}

	public void setNumAnswers(final int numAnswers) {
		this.numAnswers = numAnswers;
	}

	/* Still named "Interposed" instead of "Comments" here for compatibilty reasons. */
	@ApiModelProperty(required = true, value = "used to display comment number")
	@JsonView(View.Public.class)
	public int getNumInterposed() {
		return numComments;
	}

	/* Still named "Interposed" instead of "Comments" here for compatibilty reasons. */
	public void setNumInterposed(final int numComments) {
		this.numComments = numComments;
	}

	@ApiModelProperty(required = true, value = "the number of unanswered questions")
	@JsonView(View.Public.class)
	public int getNumUnanswered() {
		return numUnanswered;
	}

	public void setNumUnanswered(final int numUnanswered) {
		this.numUnanswered = numUnanswered;
	}

	@ApiModelProperty(required = true, value = "the creation timestamp")
	@JsonView(View.Public.class)
	public long getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(final long creationTime) {
		this.creationTime = creationTime;
	}

	/* Still named "Interposed" instead of "Comments" here for compatibilty reasons. */
	@ApiModelProperty(required = true, value = "the number of unread comments")
	@JsonView(View.Public.class)
	public int getNumUnredInterposed() {
		return numUnreadComments;
	}

	/* Still named "Interposed" instead of "Comments" here for compatibilty reasons. */
	public void setNumUnredInterposed(final int numUnreadComments) {
		this.numUnreadComments = numUnreadComments;
	}

	@Override
	public int hashCode() {
		// auto generated!
		final int prime = 31;
		int result = 1;
		result = prime * result + ((keyword == null) ? 0 : keyword.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		// auto generated!
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final RoomInfo other = (RoomInfo) obj;
		if (keyword == null) {
			if (other.keyword != null) {
				return false;
			}
		} else if (!keyword.equals(other.keyword)) {
			return false;
		}
		return true;
	}
}
