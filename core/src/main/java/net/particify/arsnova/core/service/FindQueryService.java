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

import java.util.Set;

import net.particify.arsnova.core.model.Entity;
import net.particify.arsnova.core.model.FindQuery;

/**
 * Provides a method to find entity IDs by {@link FindQuery}.
 *
 * @param <E> Entity type
 * @author Daniel Gerhardt
 */
public interface FindQueryService<E extends Entity> {
  Set<String> resolveQuery(FindQuery<E> findQuery);
}
