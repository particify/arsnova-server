/*
 * Copyright (C) 2012 THM webMedia
 *
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.services;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.thm.arsnova.entities.User;

public interface IUserService {
	User getCurrentUser();

	User getUser2SocketId(UUID socketId);

	void putUser2SocketId(UUID socketId, User user);

	void removeUser2SocketId(UUID socketId);

	Set<Map.Entry<UUID, User>> socketId2User();

	boolean isUserInSession(User user, String keyword);

	Set<User> getUsersInSession(String keyword);

	void addCurrentUserToSessionMap(String keyword);

	String getSessionForUser(String username);

	void addUserToSessionBySocketId(UUID socketId, String keyword);

	void removeUserFromSessionBySocketId(UUID socketId);

	int getUsersInSessionCount(String keyword);

	void removeUserFromMaps(User user);

	int loggedInUsers();
}
