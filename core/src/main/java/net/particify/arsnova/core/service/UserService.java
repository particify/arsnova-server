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

package net.particify.arsnova.core.service;

import java.util.List;

import net.particify.arsnova.core.model.UserProfile;

/**
 * The functionality the user service should provide.
 */
public interface UserService extends EntityService<UserProfile> {
  boolean isAdmin(String loginId, UserProfile.AuthProvider authProvider);

  UserProfile getByAuthProviderAndLoginId(UserProfile.AuthProvider authProvider, String loginId);

  List<UserProfile> getByLoginId(String loginId);

  String getIdByUsername(String username);

  List<UserProfile> getAllByMail(String mail);

  UserProfile create(String username, String password);

  UserProfile createAnonymizedGuestUser();

  boolean activateAccount(String id, String key, String clientAddress);

  void activateAccount(String id);

  void initiatePasswordReset(String id);

  boolean resetPassword(String id, String key, String password);

  UserProfile resetActivation(String id, String clientAddress);

  String generateGuestId();
}
