/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2021 The ARSnova Team and Contributors
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

import de.thm.arsnova.entities.DbUser;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.User;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;

/**
 * The functionality the user service should provide.
 */
public interface IUserService {
	User getCurrentUser();

	boolean isAdmin(String username);

	boolean isBannedFromLogin(String addr);

	void increaseFailedLoginCount(String addr);

	User getUser2SocketId(UUID socketId);

	void putUser2SocketId(UUID socketId, User user);

	void removeUser2SocketId(UUID socketId);

	Set<Map.Entry<UUID, User>> socketId2User();

	boolean isUserInSession(User user, String keyword);

	Set<User> getUsersInSession(String keyword);

	String getSessionForUser(String username);

	void addUserToSessionBySocketId(UUID socketId, String keyword);

	void removeUserFromSessionBySocketId(UUID socketId);

	void removeUserFromMaps(User user);

	int loggedInUsers();

	DbUser getDbUser(String username);

	DbUser createDbUser(String username, String password);

	DbUser updateDbUser(DbUser dbUser);

	DbUser deleteDbUser(String username);

	void initiatePasswordReset(String username);

	boolean resetPassword(DbUser dbUser, String key, String password);

	void deleteUserContent(LoggedIn user);

	void anonymizeUser(LoggedIn username);

	void impersonateUser(String username, Collection<? extends GrantedAuthority> authorities);

	LoggedIn getLoggedInFromUser(User user);
}
