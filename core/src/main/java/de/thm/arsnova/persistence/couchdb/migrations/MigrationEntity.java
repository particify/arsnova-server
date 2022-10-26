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

package de.thm.arsnova.persistence.couchdb.migrations;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.thm.arsnova.model.Entity;
import de.thm.arsnova.model.serialization.View;

/**
 * This abstract class should be implemented by models for {@link Entity}s used
 * in {@link Migration}s. It allows those models to be reduced to properties
 * relevant for the migration. Properties which are not defined are still
 * included for serialization, so they can be written back to the database
 * untouched.
 *
 * @author Daniel Gerhardt
 */
public abstract class MigrationEntity {
	private Map<String, Object> properties = new HashMap<>();
	private String id;
	private String revision;
	private Date creationTimestamp;
	private Date updateTimestamp;

	@JsonProperty("_id")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonView(View.Persistence.class)
	public String getId() {
		return id;
	}

	@JsonProperty("_id")
	@JsonView(View.Persistence.class)
	public void setId(final String id) {
		this.id = id;
	}

	@JsonProperty("_rev")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonView(View.Persistence.class)
	public String getRevision() {
		return revision;
	}

	@JsonProperty("_rev")
	@JsonView(View.Persistence.class)
	public void setRevision(final String revision) {
		this.revision = revision;
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

	public <T> T getProperty(final String key, final Class<T> clazz) {
		return (T) properties.get(key);
	}

	@JsonAnySetter
	public void setProperty(final String key, final Object value) {
		properties.put(key, value);
	}

	@JsonAnyGetter
	public Map<String, Object> getProperties() {
		return properties;
	}

	public void removeProperty(final String key) {
		properties.remove(key);
	}
}
