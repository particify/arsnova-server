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
package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

import java.util.Map;
import java.util.function.Function;

public class BeforePatchEvent<E extends Entity> extends BeforeUpdateEvent<E> {
	private final Function<E, ? extends Object> propertyGetter;
	private final Map<String, Object> changes;

	public BeforePatchEvent(final Object source, final E entity, final Function<E, ? extends Object> propertyGetter,
			final Map<String, Object> changes) {
		super(source, entity);
		this.propertyGetter = propertyGetter;
		this.changes = changes;
	}

	public Function<E, ? extends Object> getPropertyGetter() {
		return propertyGetter;
	}

	public Map<String, Object> getChanges() {
		return changes;
	}
}
