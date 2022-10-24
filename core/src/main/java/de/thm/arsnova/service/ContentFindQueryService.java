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
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.FindQuery;

@Service
public class ContentFindQueryService implements FindQueryService<Content> {
  private ContentService contentService;
  private ContentGroupService contentGroupService;

  public ContentFindQueryService(final ContentService contentService,
      final ContentGroupService contentGroupService) {
    this.contentService = contentService;
    this.contentGroupService = contentGroupService;
  }

  @Override
  public Set<String> resolveQuery(final FindQuery<Content> findQuery) {
    final Set<String> contentIds = new HashSet<>();

    if (findQuery.getExternalFilters().get("notInContentGroupOfRoomId") instanceof String) {
      final String roomId = (String) findQuery.getExternalFilters().get("notInContentGroupOfRoomId");
      final Set<String> idsWithGroup = contentGroupService.getByRoomId(roomId).stream()
          .flatMap(cg -> cg.getContentIds().stream()).collect(Collectors.toSet());
      final Set<String> idsWithoutGroup = contentService.getByRoomId(roomId).stream()
          .map(Content::getId).filter(id -> !idsWithGroup.contains(id)).collect(Collectors.toSet());
      contentIds.addAll(idsWithoutGroup);
    }

    if (findQuery.getProperties().getRoomId() != null) {
      final List<Content> contentList = contentService.getByRoomId(findQuery.getProperties().getRoomId());
      for (final Content c : contentList) {
        contentIds.add(c.getId());
      }
    }

    return contentIds;
  }
}
