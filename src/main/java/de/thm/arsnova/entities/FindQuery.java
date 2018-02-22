/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;
import org.springframework.core.style.ToStringCreator;

import java.util.Map;

public class FindQuery<E extends Entity> {
	private E properties;
	private Map<String, Object> externalFilters;

	public E getProperties() {
		return properties;
	}

	@JsonView(View.Public.class)
	public void setProperties(final E properties) {
		this.properties = properties;
	}

	public Map<String, Object> getExternalFilters() {
		return externalFilters;
	}

	@JsonView(View.Public.class)
	public void setExternalFilters(final Map<String, Object> externalFilters) {
		this.externalFilters = externalFilters;
	}

	@Override
	public String toString() {
		return new ToStringCreator(getClass())
				.append("properties", properties)
				.append("externalFilters", externalFilters)
				.toString();
	}
}
