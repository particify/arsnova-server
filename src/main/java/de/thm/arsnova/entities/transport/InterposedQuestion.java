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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * A question a student is asking. Also known as feedback or audience question.
 */
@ApiModel(value = "audiencequestion/{questionId}", description = "the Interposed Question API")
public class InterposedQuestion {

	private String id;
	private String subject;
	private String text;
	private long timestamp;
	private boolean read;

	public static List<InterposedQuestion> fromList(List<de.thm.arsnova.entities.InterposedQuestion> questions) {
		ArrayList<InterposedQuestion> interposedQuestions = new ArrayList<InterposedQuestion>();
		for (de.thm.arsnova.entities.InterposedQuestion question : questions) {
			interposedQuestions.add(new InterposedQuestion(question));
		}
		return interposedQuestions;
	}

	public InterposedQuestion(de.thm.arsnova.entities.InterposedQuestion question) {
		this.id = question.get_id();
		this.subject = question.getSubject();
		this.text = question.getText();
		this.timestamp = question.getTimestamp();
		this.read = question.isRead();
	}

	public InterposedQuestion() { }

	@ApiModelProperty(required = true, value = "used to display Id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@ApiModelProperty(required = true, value = "used to display Subject")
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	@ApiModelProperty(required = true, value = "used to display Text")
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@ApiModelProperty(required = true, value = "used to display Timetamp")
	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@ApiModelProperty(required = true, value = "is read")
	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}
}
