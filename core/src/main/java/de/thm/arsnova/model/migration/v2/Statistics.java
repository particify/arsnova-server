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

import de.thm.arsnova.model.serialization.View;

/**
 * Collection of several statistics about ARSnova.
 */
@ApiModel(value = "statistics", description = "the statistic entity")
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
	private int flashcards;

	public Statistics() {

	}

	public Statistics(final de.thm.arsnova.model.Statistics statistics) {
		this.answers = statistics.getAnswer().getTotalCount();
		this.lectureQuestions = statistics.getContent().getTotalCount();
		this.openSessions = statistics.getRoom().getTotalCount() - statistics.getRoom().getClosed();
		this.closedSessions = statistics.getRoom().getClosed();
		this.activeUsers = statistics.getUserProfile().getTotalCount();
		this.interposedQuestions = statistics.getComment().getTotalCount();
	}

	@ApiModelProperty(required = true, value = "the number of answers")
	@JsonView(View.Public.class)
	public int getAnswers() {
		return answers;
	}

	public void setAnswers(final int answers) {
		this.answers = answers;
	}

	@ApiModelProperty(required = true, value = "the number of lecture questions")
	@JsonView(View.Public.class)
	public int getLectureQuestions() {
		return lectureQuestions;
	}

	public void setLectureQuestions(final int questions) {
		this.lectureQuestions = questions;
	}

	@ApiModelProperty(required = true, value = "the number of prepartion uestions")
	@JsonView(View.Public.class)
	public int getPreparationQuestions() {
		return preparationQuestions;
	}

	public void setPreparationQuestions(final int questions) {
		this.preparationQuestions = questions;
	}

	@ApiModelProperty(required = true, value = "the total number of questions")
	@JsonView(View.Public.class)
	public int getQuestions() {
		return getLectureQuestions() + getPreparationQuestions();
	}

	@ApiModelProperty(required = true, value = "the number of open sessions")
	@JsonView(View.Public.class)
	public int getOpenSessions() {
		return openSessions;
	}

	public void setOpenSessions(final int openSessions) {
		this.openSessions = openSessions;
	}

	@ApiModelProperty(required = true, value = "the number of closed Sessions")
	@JsonView(View.Public.class)
	public int getClosedSessions() {
		return closedSessions;
	}

	public void setClosedSessions(final int closedSessions) {
		this.closedSessions = closedSessions;
	}

	@ApiModelProperty(required = true, value = "the total number of Sessions")
	@JsonView(View.Public.class)
	public int getSessions() {
		return getOpenSessions() + getClosedSessions();
	}

	@ApiModelProperty(required = true, value = "used to display Active Users")
	@JsonView(View.Public.class)
	public int getActiveUsers() {
		return activeUsers;
	}

	public void setActiveUsers(final int activeUsers) {
		this.activeUsers = activeUsers;
	}

	@ApiModelProperty(required = true, value = "the number of users that are logged")
	@JsonView(View.Public.class)
	public int getLoggedinUsers() {
		return loggedinUsers;
	}

	public void setLoggedinUsers(final int loggedinUsers) {
		this.loggedinUsers = loggedinUsers;
	}

	@ApiModelProperty(required = true, value = "the number of interposed Questions")
	@JsonView(View.Public.class)
	public int getInterposedQuestions() {
		return interposedQuestions;
	}

	public void setInterposedQuestions(final int interposedQuestions) {
		this.interposedQuestions = interposedQuestions;
	}

	@ApiModelProperty(required = true, value = "the number of flashcards")
	@JsonView(View.Public.class)
	public int getFlashcards() {
		return flashcards;
	}

	public void setFlashcards(final int flashcards) {
		this.flashcards = flashcards;
	}

	@ApiModelProperty(required = true, value = "the number of creators")
	@JsonView(View.Public.class)
	public int getCreators() {
		return creators;
	}

	public void setCreators(final int creators) {
		this.creators = creators;
	}

	@ApiModelProperty(required = true, value = "the number of concept Questions")
	@JsonView(View.Public.class)
	public int getConceptQuestions() {
		return conceptQuestions;
	}

	public void setConceptQuestions(final int conceptQuestions) {
		this.conceptQuestions = conceptQuestions;
	}

	@ApiModelProperty(required = true, value = "the number of active Students")
	@JsonView(View.Public.class)
	public int getActiveStudents() {
		return activeStudents;
	}

	public void setActiveStudents(final int activeStudents) {
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
