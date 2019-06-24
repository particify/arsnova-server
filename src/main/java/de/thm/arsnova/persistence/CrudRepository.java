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

package de.thm.arsnova.persistence;

/**
 * This is a temporary extension to {@link org.springframework.data.repository.CrudRepository} which simplifies the
 * migration to Spring Data 2.0.
 *
 * {@inheritDoc}
 *
 * @author Daniel Gerhardt
 */
public interface CrudRepository<T, ID> extends org.springframework.data.repository.CrudRepository<T, ID> {
	/**
	 *
	 * @param id The entity's Id
	 * @return The retrieved entity or null
	 * @deprecated Use {@link #findById(Object)} instead.
	 */
	@Deprecated
	T findOne(final ID id);
}
