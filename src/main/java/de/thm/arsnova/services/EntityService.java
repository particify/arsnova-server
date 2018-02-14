/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team
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
package de.thm.arsnova.services;

import de.thm.arsnova.entities.Entity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Provides CRUD operations for entities independently from the underlying persistence implementation.
 *
 * @param <T> Entity type
 * @author Daniel Gerhardt
 */
public interface EntityService<T extends Entity> {
	@PreAuthorize("hasPermission(#id, #this.this.getTypeName(), 'read')")
	T get(String id);

	@PreAuthorize("hasPermission(#entity, 'create')")
	T create(T entity);

	@PreAuthorize("hasPermission(#oldEntity, 'update')")
	T update(T oldEntity, T newEntity);

	T patch(T entity, Map<String, Object> changes) throws IOException;

	@PreAuthorize("hasPermission(#entity, 'update')")
	T patch(T entity, Map<String, Object> changes, Function<T, ? extends Object> propertyGetter)
			throws IOException;

	Iterable<T> patch(Collection<T> entities, Map<String, Object> changes) throws IOException;

	@PreFilter(value = "hasPermission(filterObject, 'update')", filterTarget = "entities")
	Iterable<T> patch(Collection<T> entities, Map<String, Object> changes, Function<T, ? extends Object> propertyGetter)
			throws IOException;

	@PreAuthorize("hasPermission(#entity, 'delete')")
	void delete(T entity);
}
