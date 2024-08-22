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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Set;

import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.ContentGroupTemplate;
import net.particify.arsnova.core.model.ContentTemplate;
import net.particify.arsnova.core.model.WordContent;

/**
 * The functionality the question service should provide.
 */
public interface ContentService extends EntityService<Content> {
  List<Content> getByRoomId(String roomId);

  int countByRoomId(String roomId);

  List<Integer> getCorrectChoiceIndexes(String contentId);

  Set<String> getCorrectTerms(String contentId);

  byte[] exportToCsv(List<String> contentIds, String charset) throws JsonProcessingException;

  byte[] exportToTsv(List<String> contentIds, String charset) throws JsonProcessingException;

  void addToBannedKeywords(WordContent wordContent, String keyword);

  void clearBannedKeywords(WordContent wordContent);

  List<Content> createFromTemplates(
      String roomId,
      ContentGroupTemplate contentGroupTemplate,
      List<ContentTemplate> templates);

  void start(String contentId);

  void stop(String contentId);

  void startRound(String contentId, int round);
}
