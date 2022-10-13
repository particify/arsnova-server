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

package de.thm.arsnova.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.FindQuery;
import de.thm.arsnova.model.UserProfile;

@Service
public class UserFindQueryService implements FindQueryService<UserProfile> {
  private UserService userService;

  public UserFindQueryService(final UserService userService) {
    this.userService = userService;
  }

  @Override
  public Set<String> resolveQuery(final FindQuery<UserProfile> findQuery) {
    final Set<String> userIds = new HashSet<>();
    if (findQuery.getProperties().getLoginId() != null) {
      final List<UserProfile> userList = userService.getByLoginId(findQuery.getProperties().getLoginId());
      for (final UserProfile user : userList) {
        userIds.add(user.getId());
      }
    }

    return userIds;
  }
}
