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
package de.thm.arsnova.entities.migration.v2;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * This class represents a list of motdkeys for a user.
 */
@ApiModel(value = "motdlist", description = "the motdlist to save the messages a user has confirmed to be read")
public class MotdList implements Entity {
	private String id;
	private String rev;
	private String motdkeys;
	private String username;

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

	@ApiModelProperty(required = true, value = "the motdkeylist")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getMotdkeys() {
		return motdkeys;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setMotdkeys(String motds) {
		motdkeys = motds;
	}

	@ApiModelProperty(required = true, value = "the username")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getUsername() {
		return username;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setUsername(final String u) {
		username = u;
	}
}
