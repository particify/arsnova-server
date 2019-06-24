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

import java.io.IOException;
import java.util.List;

import de.thm.arsnova.model.Comment;
import de.thm.arsnova.model.migration.v2.CommentReadingCount;

public interface CommentService extends EntityService<Comment> {
	int count(String roomId);

	CommentReadingCount countRead(String roomId, String username);

	List<Comment> getByRoomId(String roomId, int offset, int limit);

	Comment getAndMarkRead(String commentId) throws IOException;

	void deleteByRoomId(String roomId);
}
