/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2016 The ARSnova Team
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

/**
 * This class represents a list of motdkeys for a user.
 */
@ApiModel(value = "motdlist", description = "the motdlist to save the messages a user has confirmed to be read")
public class MotdList {

  private String motdkeys;
	private String username;
	private String _id;
	private String _rev;

	@ApiModelProperty(required = true, value = "the motdkeylist")
	public String getMotdkeys() {
		return motdkeys;
	}

	public void setMotdkeys(String motds) {
		motdkeys = motds;
	}

  @ApiModelProperty(required = true, value = "the username")
  public String getUsername() {
    return username;
  }

  public void setUsername(final String u) {
		username = u;
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
}
