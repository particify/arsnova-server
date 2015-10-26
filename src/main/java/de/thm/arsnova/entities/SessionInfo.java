/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
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
package de.thm.arsnova.entities;

import java.util.ArrayList;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Summary information of a specific session. For example, this is used to display list entries of 'my sessions' as well
 * as 'my visited sessions'.
 */
@ApiModel(value = "session/import", description = "the session info entity")
public class SessionInfo {

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
	private int numInterposed;
	private int numUnredInterposed;
	private int numUnanswered;

	public SessionInfo(Session session) {
		this.name = session.getName();
		this.shortName = session.getShortName();
		this.keyword = session.getKeyword();
		this.active = session.isActive();
		this.courseType = session.getCourseType();
		this.creationTime = session.getCreationTime();
		this.sessionType = session.getSessionType();
		this.ppLevel = session.getPpLevel();
		this.ppSubject = session.getPpSubject();
	}

	public SessionInfo() {}

	public static List<SessionInfo> fromSessionList(List<Session> sessions) {
		List<SessionInfo> infos = new ArrayList<SessionInfo>();
		for (Session s : sessions) {
			infos.add(new SessionInfo(s));
		}
		return infos;
	}

	@ApiModelProperty(required = true, value = "the name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ApiModelProperty(required = true, value = "the short name")
	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	@ApiModelProperty(required = true, value = "the keyword")
	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	@ApiModelProperty(required = true, value = "true for active")
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@ApiModelProperty(required = true, value = "the source the course comes from (example: moodle)")
	public String getCourseType() {
		return courseType;
	}

	public void setCourseType(String courseType) {
		this.courseType = courseType;
	}

	@ApiModelProperty(required = true, value = "the session type")
	public String getSessionType() {
		return sessionType;
	}

	public void setSessionType(String sessionType) {
		this.sessionType = sessionType;
	}

	@ApiModelProperty(required = true, value = "used to display level")
	public String getPpLevel() {
		return ppLevel;
	}

	public void setPpLevel(String ppLevel) {
		this.ppLevel = ppLevel;
	}

	@ApiModelProperty(required = true, value = "the public pool subject")
	public String getPpSubject() {
		return ppSubject;
	}

	public void setPpSubject(String ppSubject) {
		this.ppSubject = ppSubject;
	}

	@ApiModelProperty(required = true, value = "the number of questions")
	public int getNumQuestions() {
		return numQuestions;
	}

	public void setNumQuestions(int numQuestions) {
		this.numQuestions = numQuestions;
	}

	@ApiModelProperty(required = true, value = "the number of answers")
	public int getNumAnswers() {
		return numAnswers;
	}

	public void setNumAnswers(int numAnswers) {
		this.numAnswers = numAnswers;
	}

	@ApiModelProperty(required = true, value = "used to display interposed number")
	public int getNumInterposed() {
		return numInterposed;
	}

	public void setNumInterposed(int numInterposed) {
		this.numInterposed = numInterposed;
	}

	@ApiModelProperty(required = true, value = "the number of unanswered questions")
	public int getNumUnanswered() {
		return numUnanswered;
	}

	public void setNumUnanswered(int numUnanswered) {
		this.numUnanswered = numUnanswered;
	}

	@ApiModelProperty(required = true, value = "the creation timestamp")
	public long getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	@ApiModelProperty(required = true, value = "the number of unread interposed questions")
	public int getNumUnredInterposed() {
		return numUnredInterposed;
	}

	public void setNumUnredInterposed(int numUnredInterposed) {
		this.numUnredInterposed = numUnredInterposed;
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
	public boolean equals(Object obj) {
		// auto generated!
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) {
			return false;
		}
		SessionInfo other = (SessionInfo) obj;
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
