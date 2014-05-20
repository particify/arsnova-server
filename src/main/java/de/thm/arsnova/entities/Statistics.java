package de.thm.arsnova.entities;

public class Statistics {

	private int answers;
	private int questions;
	private int openSessions;
	private int closedSessions;
	private int activeUsers;

	public int getAnswers() {
		return answers;
	}
	public void setAnswers(int answers) {
		this.answers = answers;
	}

	public int getQuestions() {
		return questions;
	}
	public void setQuestions(int questions) {
		this.questions = questions;
	}

	public int getOpenSessions() {
		return openSessions;
	}
	public void setOpenSessions(int openSessions) {
		this.openSessions = openSessions;
	}

	public int getClosedSessions() {
		return closedSessions;
	}
	public void setClosedSessions(int closedSessions) {
		this.closedSessions = closedSessions;
	}

	public int getActiveUsers() {
		return activeUsers;
	}
	public void setActiveUsers(int activeUsers) {
		this.activeUsers = activeUsers;
	}

	@Override
	public int hashCode() {
		return (this.getClass().getName()
				+ this.activeUsers
				+ this.answers
				+ this.closedSessions
				+ this.openSessions
				+ this.questions).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj instanceof Statistics) {
			Statistics other = (Statistics) obj;
			return this.hashCode() == other.hashCode();
		}

		return false;
	}
}
