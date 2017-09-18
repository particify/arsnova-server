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
package de.thm.arsnova.services;

import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.migration.v2.Room;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.UUID;

/**
 * This service is used to assign and check for a specific role.
 */
@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserRoomServiceImpl implements UserRoomService, Serializable {
	private static final long serialVersionUID = 1L;

	private UserAuthentication user;
	private Room room;
	private UUID socketId;
	private Role role;

	@Override
	public void setUser(final UserAuthentication u) {
		user = u;
		user.setRole(role);
	}

	@Override
	public UserAuthentication getUser() {
		return user;
	}

	@Override
	public void setRoom(final Room room) {
		this.room = room;
	}

	@Override
	public Room getRoom() {
		return room;
	}

	@Override
	public void setSocketId(final UUID sId) {
		socketId = sId;
	}

	@Override
	public UUID getSocketId() {
		return socketId;
	}

	@Override
	public boolean inRoom() {
		return isAuthenticated()
				&& getRoom() != null;
	}

	@Override
	public boolean isAuthenticated() {
		return getUser() != null
				&& getRole() != null;
	}

	@Override
	public void setRole(final Role r) {
		role = r;
		if (user != null) {
			user.setRole(role);
		}
	}

	@Override
	public Role getRole() {
		return role;
	}
}
