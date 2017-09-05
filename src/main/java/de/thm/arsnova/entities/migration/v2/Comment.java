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

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.Entity;
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.serialization.View;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A question the user is asking the teacher. Also known as comment, feedback or audience question.
 */
@ApiModel(value = "comment", description = "the comment entity")
public class Comment implements Entity {
	private String id;
	private String rev;
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

	@ApiModelProperty(required = true, value = "is read")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isRead() {
		return read;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setRead(boolean read) {
		this.read = read;
	}

	@ApiModelProperty(required = true, value = "the subject")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getSubject() {
		return subject;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSubject(String subject) {
		this.subject = subject;
	}

	@ApiModelProperty(required = true, value = "the Text")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getText() {
		return text;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setText(String text) {
		this.text = text;
	}

	@ApiModelProperty(required = true, value = "ID of the session, the comment is assigned to")
	@JsonView(View.Persistence.class)
	public String getSessionId() {
		return sessionId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@ApiModelProperty(required = true, value = "creation date timestamp")
	@JsonView({View.Persistence.class, View.Public.class})
	public long getTimestamp() {
		return timestamp;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@JsonView(View.Persistence.class)
	public String getCreator() {
		return creator;
	}

	@JsonView(View.Persistence.class)
	public void setCreator(String creator) {
		this.creator = creator;
	}

	public boolean isCreator(UserAuthentication user) {
		return user.getUsername().equals(creator);
	}
}
