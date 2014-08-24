package de.thm.arsnova.entities;

public class Statistics {

	private int answers;
	private int questions;
	private int openSessions;
	private int closedSessions;
	private int activeUsers;
	private int loggedinUsers;

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

	public int getAverageAnswersPerQuestion() {
		if (getQuestions() > 0) {
			return getAnswers() / getQuestions();
		}
		return 0;
	}
	public void setAverageAnswersPerQuestion(final int value) {
		// NOP
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
