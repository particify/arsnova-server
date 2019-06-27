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

package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Date;
import java.util.Objects;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.View;

/**
 * Used as base for classes that represent persistent data with an unique ID.
 *
 * @author Daniel Gerhardt
 */
public abstract class Entity {
	protected String id;
	protected String rev;
	protected Date creationTimestamp;
	protected Date updateTimestamp;
	private boolean internal;

	@JsonView({View.Persistence.class, View.Public.class})
	public String getId() {
		return id;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setId(final String id) {
		this.id = id;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getRevision() {
		return rev;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setRevision(final String rev) {
		this.rev = rev;
	}

	@JsonView(View.Persistence.class)
	public Date getCreationTimestamp() {
		return creationTimestamp;
	}

	@JsonView(View.Persistence.class)
	public void setCreationTimestamp(final Date creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}

	@JsonView(View.Persistence.class)
	public Date getUpdateTimestamp() {
		return updateTimestamp;
	}

	@JsonView(View.Persistence.class)
	public void setUpdateTimestamp(final Date updateTimestamp) {
		this.updateTimestamp = updateTimestamp;
	}

	@JsonView(View.Persistence.class)
	public Class<? extends Entity> getType() {
		return getClass();
	}

	public boolean isInternal() {
		return internal;
	}

	public void setInternal(final boolean internal) {
		this.internal = internal;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, rev, creationTimestamp, updateTimestamp);
	}

	/**
	 * Use this helper method when overriding {@link #hashCode()}.
	 *
	 * @param init The result of <tt>super.hashCode()</tt>
	 * @param additionalFields Fields introduced by the subclass which should be included in the hash code generation
	 *
	 * @see java.util.Arrays#hashCode(Object[])
	 */
	protected int hashCode(final int init, final Object... additionalFields) {
		int result = init;
		if (additionalFields == null) {
			return result;
		}
		for (final Object element : additionalFields) {
			result = 31 * result + (element == null ? 0 : element.hashCode());
		}

		return result;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final Entity entity = (Entity) o;

		return Objects.equals(id, entity.id)
				&& Objects.equals(rev, entity.rev)
				&& Objects.equals(creationTimestamp, entity.creationTimestamp)
				&& Objects.equals(updateTimestamp, entity.updateTimestamp);
	}

	@Override
	public String toString() {
		return buildToString().toString();
	}

	/**
	 * Use this helper method to adjust the output of {@link #toString()}.
	 * Override this method instead of <tt>toString()</tt> and call <tt>super.buildToString()</tt>.
	 * Additional fields can be added to the String by calling
	 * {@link org.springframework.core.style.ToStringCreator#append} on the <tt>ToStringCreator</tt>.
	 */
	protected ToStringCreator buildToString() {
		final ToStringCreator toStringCreator = new ToStringCreator(this);
		toStringCreator
				.append("id", id)
				.append("revision", rev)
				.append("creationTimestamp", creationTimestamp)
				.append("updateTimestamp", updateTimestamp);

		return toStringCreator;
	}
}
