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

public class Statistics {

	private int answers;
	private int questions;
	private int openSessions;
	private int closedSessions;
	private int activeUsers;
	private int loggedinUsers;
	private int interposedQuestions;

	public int getAnswers() {
		return answers;
	}

	public void setAnswers(final int answers) {
		this.answers = answers;
	}

	public int getQuestions() {
		return questions;
	}

	public void setQuestions(final int questions) {
		this.questions = questions;
	}

	public int getOpenSessions() {
		return openSessions;
	}

	public void setOpenSessions(final int openSessions) {
		this.openSessions = openSessions;
	}

	public int getClosedSessions() {
		return closedSessions;
	}

	public void setClosedSessions(final int closedSessions) {
		this.closedSessions = closedSessions;
	}

	public int getActiveUsers() {
		return activeUsers;
	}

	public void setActiveUsers(final int activeUsers) {
		this.activeUsers = activeUsers;
	}

	public int getLoggedinUsers() {
		return loggedinUsers;
	}

	public void setLoggedinUsers(final int loggedinUsers) {
		this.loggedinUsers = loggedinUsers;
	}

	public int getInterposedQuestions() {
		return interposedQuestions;
	}

	public void setInterposedQuestions(int interposedQuestions) {
		this.interposedQuestions = interposedQuestions;
	}

	@Override
	public int hashCode() {
		return (this.getClass().getName()
				+ activeUsers
				+ answers
				+ closedSessions
				+ openSessions
				+ questions
				+ loggedinUsers
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
