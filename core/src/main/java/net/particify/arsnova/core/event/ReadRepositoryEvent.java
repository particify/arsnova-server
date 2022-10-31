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

public class ReadRepositoryEvent extends RepositoryEvent {
  private final boolean reduced;
  private final boolean multiple;
  private final boolean partial;

  public ReadRepositoryEvent(final Object source, final String typeName) {
    super(source, typeName);
    this.multiple = false;
    this.partial = false;
    this.reduced = false;
  }

  public ReadRepositoryEvent(
      final Object source,
      final String typeName,
      final boolean multiple,
      final boolean partial,
      final boolean reduced) {
    super(source, typeName);
    this.multiple = multiple;
    this.partial = partial;
    this.reduced = reduced;
  }

  public boolean isReduced() {
    return reduced;
  }

  public boolean isMultiple() {
    return multiple && !isReduced();
  }

  public boolean isPartial() {
    return partial && !isReduced();
  }
}
