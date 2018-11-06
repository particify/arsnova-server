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
package de.thm.arsnova.service;

import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.security.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The functionality the user service should provide.
 */
public interface UserService extends EntityService<UserProfile> {
	UserProfile getCurrentUserProfile();

	User getCurrentUser();

	de.thm.arsnova.model.ClientAuthentication getCurrentClientAuthentication();

	boolean isAdmin(String username);

	boolean isBannedFromLogin(String addr);

	void increaseFailedLoginCount(String addr);

	String getUserIdToSocketId(UUID socketId);

	void putUserIdToSocketId(UUID socketId, String userId);

	void removeUserToSocketId(UUID socketId);

	Set<Map.Entry<UUID, String>> getSocketIdToUserId();

	boolean isUserInRoom(String userId, String roomId);

	Set<String> getUsersByRoomId(String roomId);

	String getRoomIdByUserId(String userId);

	void addUserToRoomBySocketId(UUID socketId, String roomId);

	void removeUserFromRoomBySocketId(UUID socketId);

	void removeUserIdFromMaps(String userId);

	int loggedInUsers();

	void authenticate(UsernamePasswordAuthenticationToken token, UserProfile.AuthProvider authProvider);

	User loadUser(UserProfile.AuthProvider authProvider, String loginId,
			Collection<GrantedAuthority> grantedAuthorities, boolean autoCreate) throws UsernameNotFoundException;

	User loadUser(String userId, Collection<GrantedAuthority> grantedAuthorities);

	UserProfile getByAuthProviderAndLoginId(UserProfile.AuthProvider authProvider, String loginId);

	UserProfile getByUsername(String username);

	UserProfile create(String username, String password);

	UserProfile update(UserProfile userProfile);

	UserProfile deleteByUsername(String username);

	void addRoomToHistory(UserProfile userProfile, Room room);

	void initiatePasswordReset(String username);

	boolean resetPassword(UserProfile userProfile, String key, String password);
}
