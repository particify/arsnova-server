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

import java.util.Map;

import de.thm.arsnova.model.Entity;

public abstract class AfterUpdateEvent<E extends Entity> extends CrudEvent<E> {
  private final E oldEntity;
  private final Map<String, Object> changes;

  public AfterUpdateEvent(final Object source, final E entity, final E oldEntity,
      final Map<String, Object> changes) {
    super(source, entity);
    this.oldEntity = oldEntity;
    this.changes = changes;
  }

  public E getOldEntity() {
    return oldEntity;
  }

  public Map<String, Object> getChanges() {
    return changes;
  }

  public Map<String, Object> getRequestedChanges() {
    return changes;
  }
}
