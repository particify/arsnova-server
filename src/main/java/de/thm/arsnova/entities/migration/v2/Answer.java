/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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
package de.thm.arsnova.entities.migration.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.Entity;
import de.thm.arsnova.entities.serialization.View;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Both a regular (single choice, evaluation, etc.) as well as a freetext answer.
 *
 * This class has additional fields to transport generated answer statistics.
 */
@ApiModel(value = "Answer", description = "the answer entity")
public class Answer implements Entity {
	private String id;
	private String rev;
	private String type;
	private String sessionId;
	private String questionId;
	private String answerText;
	private String answerTextRaw;
	private String answerSubject;
	private boolean successfulFreeTextAnswer;
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

	@ApiModelProperty(required = true, value = "the couchDB ID")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getId() {
		return id;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setId(final String id) {
		this.id = id;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setRevision(final String rev) {
		this.rev = rev;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getRevision() {
		return rev;
	}

	@ApiModelProperty(required = true, value = "ID of the session, the answer is assigned to")
	@JsonView({View.Persistence.class, View.Public.class})
	public final String getSessionId() {
		return sessionId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setSessionId(final String sessionId) {
		this.sessionId = sessionId;
	}

	@ApiModelProperty(required = true, value = "used to display question id")
	@JsonView({View.Persistence.class, View.Public.class})
	public final String getQuestionId() {
		return questionId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setQuestionId(final String questionId) {
		this.questionId = questionId;
	}

	@ApiModelProperty(required = true, value = "the answer text")
	@JsonView({View.Persistence.class, View.Public.class})
	public final String getAnswerText() {
		return answerText;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setAnswerText(final String answerText) {
		this.answerText = answerText;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final String getAnswerTextRaw() {
		return this.answerTextRaw;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setAnswerTextRaw(final String answerTextRaw) {
		this.answerTextRaw = answerTextRaw;
	}

	@ApiModelProperty(required = true, value = "the answer subject")
	@JsonView({View.Persistence.class, View.Public.class})
	public final String getAnswerSubject() {
		return answerSubject;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setAnswerSubject(final String answerSubject) {
		this.answerSubject = answerSubject;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final boolean isSuccessfulFreeTextAnswer() {
		return this.successfulFreeTextAnswer;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setSuccessfulFreeTextAnswer(final boolean successfulFreeTextAnswer) {
		this.successfulFreeTextAnswer = successfulFreeTextAnswer;
	}

	@ApiModelProperty(required = true, value = "the peer instruction round nr.")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getPiRound() {
		return piRound;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPiRound(int piRound) {
		this.piRound = piRound;
	}

	@ApiModelProperty(required = true, value = "the user")
	@JsonView(View.Persistence.class)
	public final String getUser() {
		return user;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setUser(final String user) {
		this.user = user;
	}

	@ApiModelProperty(required = true, value = "the answer image")
	@JsonView(View.Persistence.class)
	public String getAnswerImage() {
		return answerImage;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAnswerImage(String answerImage) {
		this.answerImage = answerImage;
	}

	@ApiModelProperty(required = true, value = "the answer thumbnail")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getAnswerThumbnailImage() {
		return answerThumbnailImage;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAnswerThumbnailImage(String answerThumbnailImage) {
		this.answerThumbnailImage = answerThumbnailImage;
	}

	@ApiModelProperty(required = true, value = "the creation date timestamp")
	@JsonView({View.Persistence.class, View.Public.class})
	public long getTimestamp() {
		return timestamp;
	}

	@JsonView(View.Persistence.class)
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@ApiModelProperty(required = true, value = "displays whether the answer is read")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isRead() {
		return read;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setRead(boolean read) {
		this.read = read;
	}

	@ApiModelProperty(required = true, value = "the number of answers given. used for statistics")
	@JsonView(View.Public.class)
	public final int getAnswerCount() {
		return answerCount;
	}

	public final void setAnswerCount(final int answerCount) {
		this.answerCount = answerCount;
	}

	@ApiModelProperty(required = true, value = "the abstention")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isAbstention() {
		return abstention;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAbstention(boolean abstention) {
		this.abstention = abstention;
	}

	@ApiModelProperty(required = true, value = "the number of abstentions given. used for statistics")
	@JsonView(View.Public.class)
	public int getAbstentionCount() {
		return abstentionCount;
	}

	public void setAbstentionCount(int abstentionCount) {
		this.abstentionCount = abstentionCount;
	}

	@ApiModelProperty(required = true, value = "either lecture or preparation")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getQuestionVariant() {
		return questionVariant;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setQuestionVariant(String questionVariant) {
		this.questionVariant = questionVariant;
	}

	@ApiModelProperty(required = true, value = "used to display question value")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getQuestionValue() {
		return questionValue;
	}

	@JsonView({View.Persistence.class, View.Public.class})
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
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((rev == null) ? 0 : rev.hashCode());
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
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Answer other = (Answer) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (rev == null) {
			if (other.rev != null) {
				return false;
			}
		} else if (!rev.equals(other.rev)) {
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
