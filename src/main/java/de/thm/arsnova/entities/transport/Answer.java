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
package de.thm.arsnova.entities.transport;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.User;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Answer {

	private String answerSubject;

	private String answerText;
	
	private String answerImage;

	private boolean abstention;

	public String getAnswerText() {
		return answerText;
	}

	public void setAnswerText(String answerText) {
		this.answerText = answerText;
	}

	public String getAnswerSubject() {
		return answerSubject;
	}

	public void setAnswerSubject(String answerSubject) {
		this.answerSubject = answerSubject;
	}

	public boolean isAbstention() {
		return abstention;
	}

	public void setAbstention(boolean abstention) {
		this.abstention = abstention;
	}

	public de.thm.arsnova.entities.Answer generateAnswerEntity(final User user, final Question question) {
		// rewrite all fields so that no manipulated data gets written
		// only answerText, answerSubject, and abstention are allowed
		de.thm.arsnova.entities.Answer theAnswer = new de.thm.arsnova.entities.Answer();
		theAnswer.setAnswerSubject(this.getAnswerSubject());
		theAnswer.setAnswerText(this.getAnswerText());
		theAnswer.setSessionId(question.getSessionId());
		theAnswer.setUser(user.getUsername());
		theAnswer.setQuestionId(question.get_id());
		theAnswer.setTimestamp(new Date().getTime());
		theAnswer.setQuestionVariant(question.getQuestionVariant());
		theAnswer.setAbstention(this.isAbstention());
		// calculate learning progress value after all properties are set
		theAnswer.setQuestionValue(question.calculateValue(theAnswer));
		theAnswer.setAnswerImage(this.getAnswerImage());

		if ("freetext".equals(question.getQuestionType())) {
			theAnswer.setPiRound(0);
		} else {
			theAnswer.setPiRound(question.getPiRound());
		}

		return theAnswer;
	}
	
	public String getAnswerImage() {
		return answerImage;
	}
	
	public void setAnswerImage(String answerImage) {
		this.answerImage = answerImage;
	}
}
