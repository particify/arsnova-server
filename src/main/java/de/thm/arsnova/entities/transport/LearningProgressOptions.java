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

public class LearningProgressOptions {

	private String sessionKeyword;

	private String type;

	private String questionVariant;

	public String getSessionKeyword() {
		return sessionKeyword;
	}

	public void setSessionKeyword(String sessionKeyword) {
		this.sessionKeyword = sessionKeyword;
	}

	public String getType() {
		return type;
	}

	public void setType(String learningProgressType) {
		this.type = learningProgressType;
	}

	public String getQuestionVariant() {
		return questionVariant;
	}

	public void setQuestionVariant(String questionVariant) {
		this.questionVariant = questionVariant;
	}

	public de.thm.arsnova.entities.LearningProgressOptions toEntity() {
		de.thm.arsnova.entities.LearningProgressOptions entity = new de.thm.arsnova.entities.LearningProgressOptions();
		entity.setType(this.getType());
		entity.setQuestionVariant(this.getQuestionVariant());
		return entity;
	}
}
