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

package de.thm.arsnova.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.thm.arsnova.event.AfterCreationEvent;
import de.thm.arsnova.event.AfterDeletionEvent;
import de.thm.arsnova.model.Comment;
import de.thm.arsnova.model.Room;

/**
 * This class is used to evict caches based on events. The events carry all necessary information to clear the
 * caches, e.g, for a specific session.
 */
@Component
public class CacheBusterImpl implements CacheBuster {
	@CacheEvict(value = "statistics", allEntries = true)
	@EventListener
	public void handleAfterCommentCreation(final AfterCreationEvent<Comment> event) {
		/* Implementation provided by caching aspect. */
	}

	@CacheEvict(value = "statistics", allEntries = true)
	@EventListener
	public void handleAfterCommentDeletion(final AfterDeletionEvent<Comment> event) {
		/* Implementation provided by caching aspect. */
	}

	@CacheEvict(value = "statistics", allEntries = true)
	@EventListener
	public void handleAfterRoomCreation(final AfterCreationEvent<Room> event) {
		/* Implementation provided by caching aspect. */
	}

	@CacheEvict(value = "statistics", allEntries = true)
	@EventListener
	public void handleAfterRoomDeletion(final AfterDeletionEvent<Room> event) {
		/* Implementation provided by caching aspect. */
	}
}
