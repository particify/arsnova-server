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

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Answer {

	private String _id;
	private String _rev;
	private String type;
	private String sessionId;
	private String questionId;
	private String answerText;
	private String answerSubject;
	private String questionVariant;
	private int questionValue;
	private int piRound;
	private String user;
	private long timestamp;
	private int answerCount = 1;
	private boolean abstention;
	private int abstentionCount;

	public Answer() {
		this.type = "skill_question_answer";
	}

	public final String get_id() {
		return _id;
	}

	public final void set_id(String _id) {
		this._id = _id;
	}

	public final String get_rev() {
		return _rev;
	}

	public final void set_rev(final String _rev) {
		this._rev = _rev;
	}

	public final String getType() {
		return type;
	}

	public final void setType(final String type) {
		this.type = type;
	}

	public final String getSessionId() {
		return sessionId;
	}

	public final void setSessionId(final String sessionId) {
		this.sessionId = sessionId;
	}

	public final String getQuestionId() {
		return questionId;
	}

	public final void setQuestionId(final String questionId) {
		this.questionId = questionId;
	}

	public final String getAnswerText() {
		return answerText;
	}

	public final void setAnswerText(final String answerText) {
		this.answerText = answerText;
	}

	public final String getAnswerSubject() {
		return answerSubject;
	}

	public final void setAnswerSubject(final String answerSubject) {
		this.answerSubject = answerSubject;
	}

	public int getPiRound() {
		return piRound;
	}

	public void setPiRound(int piRound) {
		this.piRound = piRound;
	}

	/* TODO: use JsonViews instead of JsonIgnore when supported by Spring (4.1)
	 * http://wiki.fasterxml.com/JacksonJsonViews
	 * https://jira.spring.io/browse/SPR-7156 */
	@JsonIgnore
	public final String getUser() {
		return user;
	}

	public final void setUser(final String user) {
		this.user = user;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public final int getAnswerCount() {
		return answerCount;
	}

	public final void setAnswerCount(final int answerCount) {
		this.answerCount = answerCount;
	}

	public boolean isAbstention() {
		return abstention;
	}

	public void setAbstention(boolean abstention) {
		this.abstention = abstention;
	}

	public int getAbstentionCount() {
		return abstentionCount;
	}

	public void setAbstentionCount(int abstentionCount) {
		this.abstentionCount = abstentionCount;
	}

	public String getQuestionVariant() {
		return questionVariant;
	}

	public void setQuestionVariant(String questionVariant) {
		this.questionVariant = questionVariant;
	}

	public int getQuestionValue() {
		return questionValue;
	}

	public void setQuestionValue(int questionValue) {
		this.questionValue = questionValue;
	}

	@Override
	public final String toString() {
		return "Answer type:'" + type + "'"
				+ ", session: " + sessionId
				+ ", question: " + questionId
				+ ", subject: " + answerSubject
				+ ", answerCount: " + answerCount
				+ ", answer: " + answerText
				+ ", user: " + user;
	}

}
