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

package net.particify.arsnova.core.persistence;

import java.time.Instant;
import java.util.List;

import net.particify.arsnova.core.model.UserProfile;

public interface UserRepository extends CrudRepository<UserProfile, String> {
  UserProfile findByAuthProviderAndLoginId(UserProfile.AuthProvider authProvider, String loginId);

  List<UserProfile> findByLoginId(String loginId);

  List<UserProfile> findAllByMail(String mail);

  int deleteNonActivatedUsers(long lastActivityBefore);

  List<UserProfile> findAllByLastActivityTimestamp(Instant lastActivityTimestamp, int limit);

  int countByLastActivityTimestamp(Instant lastActivityTimestamp);
}
