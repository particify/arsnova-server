/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * This class represents a message of the day.
 */
@ApiModel(value = "motd", description = "the message of the day entity")
public class Motd implements Entity {

	private String motdkey; //ID
	private Date startdate;
	private Date enddate;
	private String title;
	private String text;
	private String audience;
	private String sessionId;
	private String sessionkey;
	private String id;
	private String rev;

	@ApiModelProperty(required = true, value = "the identification string")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getMotdkey() {
		return motdkey;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setMotdkey(final String key) {
		motdkey = key;
	}

	@ApiModelProperty(required = true, value = "startdate for showing this message (timestamp format)")
	@JsonView({View.Persistence.class, View.Public.class})
	public Date getStartdate() {
		return startdate;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setStartdate(final Date timestamp) {
		startdate = timestamp;
	}

	@ApiModelProperty(required = true, value = "enddate for showing this message (timestamp format)")
	@JsonView({View.Persistence.class, View.Public.class})
	public Date getEnddate() {
		return enddate;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setEnddate(final Date timestamp) {
		enddate = timestamp;
	}

	@ApiModelProperty(required = true, value = "tite of the message")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getTitle() {
		return title;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setTitle(final String ttitle) {
		title = ttitle;
	}

	@ApiModelProperty(required = true, value = "text of the message")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getText() {
		return text;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setText(final String ttext) {
		text = ttext;
	}

	@ApiModelProperty(required = true, value = "defines the target audience for this motd (one of the following: 'student', 'tutor', 'loggedIn', 'all')")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getAudience() {
		return audience;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAudience(final String a) {
		audience = a;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getSessionId() {
		return sessionId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSessionId(final String sessionId) {
		this.sessionId = sessionId;
	}

	@ApiModelProperty(required = true, value = "when audience equals session, the sessionkey referes to the session the messages belong to")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getSessionkey() {
		return sessionkey;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSessionkey(final String a) {
		sessionkey = a;
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

	@Override
	public int hashCode() {
		// See http://stackoverflow.com/a/113600
		final int prim = 37;

		int result = 42;
		return prim * result + this.motdkey.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !obj.getClass().equals(this.getClass())) {
			return false;
		}
		Motd other = (Motd) obj;
		return this.getMotdkey().equals(other.getMotdkey());
	}
}
