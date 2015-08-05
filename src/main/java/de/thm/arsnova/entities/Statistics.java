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

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Collection of several statistics about ARSnova.
 */
@ApiModel(value = "statistics", description = "the Statistic API")
public class Statistics {

	private int answers;
	private int lectureQuestions;
	private int preparationQuestions;
	private int openSessions;
	private int closedSessions;
	private int creators;
	private int activeUsers;
	private int activeStudents;
	private int loggedinUsers;
	private int interposedQuestions;
	private int conceptQuestions;

	@ApiModelProperty(required = true, notes = "used to display Answers")
	public int getAnswers() {
		return answers;
	}

	public void setAnswers(final int answers) {
		this.answers = answers;
	}

	@ApiModelProperty(required = true, notes = "used to display Lecture Questions")
	public int getLectureQuestions() {
		return lectureQuestions;
	}

	public void setLectureQuestions(final int questions) {
		this.lectureQuestions = questions;
	}

	@ApiModelProperty(required = true, notes = "used to display prepartion Questions")
	public int getPreparationQuestions() {
		return preparationQuestions;
	}

	public void setPreparationQuestions(final int questions) {
		this.preparationQuestions = questions;
	}

	@ApiModelProperty(required = true, notes = "used to display Questions")
	public int getQuestions() {
		return getLectureQuestions() + getPreparationQuestions();
	}

	@ApiModelProperty(required = true, notes = "used to display open Sessions")
	public int getOpenSessions() {
		return openSessions;
	}

	public void setOpenSessions(final int openSessions) {
		this.openSessions = openSessions;
	}

	@ApiModelProperty(required = true, notes = "used to display closed Sessions")
	public int getClosedSessions() {
		return closedSessions;
	}

	public void setClosedSessions(final int closedSessions) {
		this.closedSessions = closedSessions;
	}

	@ApiModelProperty(required = true, notes = "used to display Sessions")
	public int getSessions() {
		return getOpenSessions() + getClosedSessions();
	}

	@ApiModelProperty(required = true, notes = "used to display Active Users")
	public int getActiveUsers() {
		return activeUsers;
	}

	public void setActiveUsers(final int activeUsers) {
		this.activeUsers = activeUsers;
	}

	@ApiModelProperty(required = true, notes = "used to display logged in Users")
	public int getLoggedinUsers() {
		return loggedinUsers;
	}

	public void setLoggedinUsers(final int loggedinUsers) {
		this.loggedinUsers = loggedinUsers;
	}

	@ApiModelProperty(required = true, notes = "used to display interposed Questions")
	public int getInterposedQuestions() {
		return interposedQuestions;
	}

	public void setInterposedQuestions(int interposedQuestions) {
		this.interposedQuestions = interposedQuestions;
	}

	@ApiModelProperty(required = true, notes = "used to display creators")
	public int getCreators() {
		return creators;
	}

	public void setCreators(int creators) {
		this.creators = creators;
	}

	@ApiModelProperty(required = true, notes = "used to display concept Questions")
	public int getConceptQuestions() {
		return conceptQuestions;
	}

	public void setConceptQuestions(int conceptQuestions) {
		this.conceptQuestions = conceptQuestions;
	}

	@ApiModelProperty(required = true, notes = "used to display active Students")
	public int getActiveStudents() {
		return activeStudents;
	}

	public void setActiveStudents(int activeStudents) {
		this.activeStudents = activeStudents;
	}

	@Override
	public int hashCode() {
		return (this.getClass().getName()
				+ activeUsers
				+ answers
				+ closedSessions
				+ openSessions
				+ lectureQuestions
				+ preparationQuestions
				+ interposedQuestions
				+ loggedinUsers
				+ conceptQuestions
				).hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj instanceof Statistics) {
			final Statistics other = (Statistics) obj;
			return hashCode() == other.hashCode();
		}
		return false;
	}
}
