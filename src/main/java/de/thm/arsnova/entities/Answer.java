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

/**
 * Both a regular (single choice, evaluation, etc.) as well as a freetext answer.
 *
 * This class has additional fields to transport generated answer statistics.
 */
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
	private boolean read;
	private int answerCount = 1;
	private boolean abstention;
	private int abstentionCount;
	@JsonIgnore
	private String answerImage;
	private String answerThumbnailImage;

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

	@JsonIgnore
	public String getAnswerImage() {
		return answerImage;
	}

	public void setAnswerImage(String answerImage) {
		this.answerImage = answerImage;
	}

	public String getAnswerThumbnailImage() {
		return answerThumbnailImage;
	}

	public void setAnswerThumbnailImage(String answerThumbnailImage) {
		this.answerThumbnailImage = answerThumbnailImage;
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

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
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

	@Override
	public int hashCode() {
		// auto generated!
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_id == null) ? 0 : _id.hashCode());
		result = prime * result + ((_rev == null) ? 0 : _rev.hashCode());
		result = prime * result + ((answerSubject == null) ? 0 : answerSubject.hashCode());
		result = prime * result + ((answerText == null) ? 0 : answerText.hashCode());
		result = prime * result + piRound;
		result = prime * result + ((questionId == null) ? 0 : questionId.hashCode());
		result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		result = prime * result + ((user == null) ? 0 : user.hashCode());
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
		Answer other = (Answer) obj;
		if (_id == null) {
			if (other._id != null) {
				return false;
			}
		} else if (!_id.equals(other._id)) {
			return false;
		}
		if (_rev == null) {
			if (other._rev != null) {
				return false;
			}
		} else if (!_rev.equals(other._rev)) {
			return false;
		}
		if (answerSubject == null) {
			if (other.answerSubject != null) {
				return false;
			}
		} else if (!answerSubject.equals(other.answerSubject)) {
			return false;
		}
		if (answerText == null) {
			if (other.answerText != null) {
				return false;
			}
		} else if (!answerText.equals(other.answerText)) {
			return false;
		}
		if (piRound != other.piRound) {
			return false;
		}
		if (questionId == null) {
			if (other.questionId != null) {
				return false;
			}
		} else if (!questionId.equals(other.questionId)) {
			return false;
		}
		if (sessionId == null) {
			if (other.sessionId != null) {
				return false;
			}
		} else if (!sessionId.equals(other.sessionId)) {
			return false;
		}
		if (timestamp != other.timestamp) {
			return false;
		}
		if (user == null) {
			if (other.user != null) {
				return false;
			}
		} else if (!user.equals(other.user)) {
			return false;
		}
		return true;
	}
}
