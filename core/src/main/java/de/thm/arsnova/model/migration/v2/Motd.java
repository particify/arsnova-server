/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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

package de.thm.arsnova.model.migration.v2;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Date;

import de.thm.arsnova.model.serialization.View;

/**
 * Represents a Message of the Day.
 */
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

	@JsonView({View.Persistence.class, View.Public.class})
	public String getMotdkey() {
		return motdkey;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setMotdkey(final String key) {
		motdkey = key;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Date getStartdate() {
		return startdate;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setStartdate(final Date timestamp) {
		startdate = timestamp;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Date getEnddate() {
		return enddate;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setEnddate(final Date timestamp) {
		enddate = timestamp;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getTitle() {
		return title;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setTitle(final String title) {
		this.title = title;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getText() {
		return text;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setText(final String ttext) {
		text = ttext;
	}

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

	@JsonView({View.Persistence.class, View.Public.class})
	public String getSessionkey() {
		return sessionkey;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSessionkey(final String a) {
		sessionkey = a;
	}

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

		final int result = 42;
		return prim * result + this.motdkey.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !obj.getClass().equals(this.getClass())) {
			return false;
		}
		final Motd other = (Motd) obj;
		return this.getMotdkey().equals(other.getMotdkey());
	}
}
