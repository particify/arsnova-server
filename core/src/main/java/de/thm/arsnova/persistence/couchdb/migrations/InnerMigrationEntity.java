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
import java.util.HashMap;
import java.util.Map;

/**
 * This abstract class should be implemented by inner classes of
 * {@link MigrationEntity}s. It allows those models to be reduced to properties
 * relevant for the migration. Properties which are not defined are still
 * included for serialization, so they can be written back to the database
 * untouched.
 *
 * @author Daniel Gerhardt
 */
public abstract class InnerMigrationEntity {
  private Map<String, Object> properties = new HashMap<>();

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
