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
import org.springframework.context.ApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

import java.util.Optional;

public class StateChangeEvent<E extends Entity, T> extends ApplicationEvent implements ResolvableTypeProvider {
	private final E entity;
	private final String stateName;
	private final T newValue;
	private final T oldValue;

	public StateChangeEvent(final Object source, final E entity, final String stateName,
			final T newValue, final T oldValue) {
		super(source);
		this.entity = entity;
		this.stateName = stateName;
		this.newValue = newValue;
		this.oldValue = oldValue;
	}

	public E getEntity() {
		return entity;
	}

	public String getStateName() {
		return stateName;
	}

	public T getNewValue() {
		return newValue;
	}

	public Optional<T> getOldValue() {
		return Optional.ofNullable(oldValue);
	}

	@Override
	public ResolvableType getResolvableType() {
		return ResolvableType.forClassWithGenerics(getClass(), entity.getClass(), newValue.getClass());
	}
}
