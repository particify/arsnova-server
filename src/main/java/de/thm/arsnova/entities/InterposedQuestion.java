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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A question the user is asking the teacher. Also known as feedback or audience question.
 */
@ApiModel(value = "audiencequestion", description = "the interposed question entity")
public class InterposedQuestion {

	private String _id;
	private String _rev;
	private String type;
	private String subject;
	private String text;
	/* FIXME sessionId actually is used to hold the sessionKey.
	 * This really needs to be changed because it leads to a lot
	 * of confusion. It can not be easily changed without a lot of
	 * refactoring since the client application depends on the
	 * current naming */
	private String sessionId;
	private long timestamp;
	private boolean read;
	private String creator;

	@ApiModelProperty(required = true, value = "the couchDB ID")
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}

	@ApiModelProperty(required = true, value = "the couchDB revision Nr.")
	public String get_rev() {
		return _rev;
	}
	public void set_rev(String _rev) {
		this._rev = _rev;
	}

	@ApiModelProperty(required = true, value = "is read")
	public boolean isRead() {
		return read;
	}
	public void setRead(boolean read) {
		this.read = read;
	}

	@ApiModelProperty(required = true, value = "used to display the type")
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

	@ApiModelProperty(required = true, value = "the Subject")
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}

	@ApiModelProperty(required = true, value = "the Text")
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}

	@ApiModelProperty(required = true, value = "ID of the session, the question is assigned to")
	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@ApiModelProperty(required = true, value = "creation date timestamp")
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/* TODO: use JsonViews instead of JsonIgnore when supported by Spring (4.1)
	 * http://wiki.fasterxml.com/JacksonJsonViews
	 * https://jira.spring.io/browse/SPR-7156 */
	@JsonIgnore
	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public boolean isCreator(User user) {
		return user.getUsername().equals(creator);
	}
}
