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

public class Authorize {
	private String user;
	private String socketid;

	@JsonView(View.Public.class)
	public final String getUser() {
		return user;
	}

	public final void setUser(final String user) {
		this.user = user;
	}

	@JsonView(View.Public.class)
	public final String getSocketid() {
		return socketid;
	}

	public final void setSocketid(final String socketid) {
		this.socketid = socketid;
	}

	@Override
	public final String toString() {
		return "user: " + user + ", socketid: " + socketid;

	}
}
