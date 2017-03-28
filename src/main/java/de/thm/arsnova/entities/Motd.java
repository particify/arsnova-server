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
package de.thm.arsnova.entities;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * This class represents a message of the day.
 */
@ApiModel(value = "motd", description = "the message of the day entity")
public class Motd {

  private String motdkey; //ID
  private Date startdate;
  private Date enddate;
  private String title;
  private String text;
	private String audience;
	private String sessionId;
	private String sessionkey;
	private String _id;
	private String _rev;

  @ApiModelProperty(required = true, value = "the identification string")
  public String getMotdkey() {
    return motdkey;
  }

  public void setMotdkey(final String key) {
    motdkey = key;
  }

	@ApiModelProperty(required = true, value = "startdate for showing this message (timestamp format)")
  public Date getStartdate() {
    return startdate;
  }

  public void setStartdate(final Date timestamp) {
    startdate = timestamp;
  }

  @ApiModelProperty(required = true, value = "enddate for showing this message (timestamp format)")
  public Date getEnddate() {
    return enddate;
  }

  public void setEnddate(final Date timestamp) {
    enddate = timestamp;
  }

  @ApiModelProperty(required = true, value = "tite of the message")
  public String getTitle() {
    return title;
  }

  public void setTitle(final String ttitle) {
    title = ttitle;
  }

  @ApiModelProperty(required = true, value = "text of the message")
  public String getText() {
    return text;
  }

  public void setText(final String ttext) {
    text = ttext;
  }

	@ApiModelProperty(required = true, value = "defines the target audience for this motd (one of the following: 'student', 'tutor', 'loggedIn', 'all')")
	public String getAudience() {
		return audience;
	}

	public void setAudience(String a) {
		audience = a;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@ApiModelProperty(required = true, value = "when audience equals session, the sessionkey referes to the session the messages belong to")
	public String getSessionkey() {
		return sessionkey;
	}

	public void setSessionkey(String a) {
		sessionkey = a;
	}

  @ApiModelProperty(required = true, value = "the couchDB ID")
	public String get_id() {
		return _id;
	}

  public void set_id(final String id) {
    _id = id;
  }

	public void set_rev(final String rev) {
		_rev = rev;
	}

	public String get_rev() {
		return _rev;
	}

	@Override
	public int hashCode() {
		// See http://stackoverflow.com/a/113600
		final int prim = 37;

		int result = 42;
		return prim * result + this.motdkey.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !obj.getClass().equals(this.getClass())) {
			return false;
		}
		Motd other = (Motd) obj;
		return this.getMotdkey().equals(other.getMotdkey());
	}
}
