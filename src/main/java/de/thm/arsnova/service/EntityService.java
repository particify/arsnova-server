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
import java.util.Map;
import java.util.function.Function;

import de.thm.arsnova.model.Entity;

/**
 * Provides CRUD operations for entities independently from the underlying persistence implementation.
 *
 * @param <T> Entity type
 * @author Daniel Gerhardt
 */
public interface EntityService<T extends Entity> {
	T get(String id);

	T get(String id, boolean internal);

	List<T> get(Iterable<String> ids);

	T create(T entity);

	List<T> create(List<T> entities);

	T update(T entity);

	T update(T entity, Class<?> view);

	T update(T oldEntity, T newEntity, Class<?> view);

	T patch(T entity, Map<String, Object> changes) throws IOException;

	T patch(T entity, Map<String, Object> changes, Class<?> view) throws IOException;

	T patch(T entity, Map<String, Object> changes, Function<T, ? extends Object> propertyGetter) throws IOException;

	T patch(T entity, Map<String, Object> changes, Function<T, ? extends Object> propertyGetter, Class<?> view)
			throws IOException;

	List<T> patch(Iterable<T> entities, Map<String, Object> changes) throws IOException;

	List<T> patch(Iterable<T> entities, Map<String, Object> changes, Class<?> view) throws IOException;

	List<T> patch(
			Iterable<T> entities,
			Map<String, Object> changes,
			Function<T, ? extends Object> propertyGetter) throws IOException;

	List<T> patch(
			Iterable<T> entities,
			Map<String, Object> changes,
			Function<T, ? extends Object> propertyGetter,
			Class<?> view) throws IOException;

	void delete(T entity);

	void delete(Iterable<T> entities);
}
