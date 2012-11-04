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
	public void setAnswers(int ansers) {
		this.answers = ansers;
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
}
