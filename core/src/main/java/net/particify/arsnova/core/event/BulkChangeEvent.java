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

package net.particify.arsnova.core.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

import net.particify.arsnova.core.model.Entity;

public class BulkChangeEvent<E extends Entity> extends ApplicationEvent implements ResolvableTypeProvider {
  private Iterable<E> entities;
  private Class<E> clazz;

  public BulkChangeEvent(final Object source, final Class<E> clazz, final Iterable<E> entities) {
    super(source);
    this.clazz = clazz;
    this.entities = entities;
  }

  public Iterable<E> getEntities() {
    return entities;
  }

  public void setEntities(final Iterable<E> entities) {
    this.entities = entities;
  }

  @Override
  public ResolvableType getResolvableType() {
    return ResolvableType.forClassWithGenerics(getClass(), clazz);
  }
}
