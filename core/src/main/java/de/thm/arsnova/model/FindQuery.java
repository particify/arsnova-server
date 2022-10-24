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
import java.util.Map;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.View;

public class FindQuery<E extends Entity> {
  enum LogicalOperator {
    AND,
    OR
  }

  private LogicalOperator operator = LogicalOperator.AND;
  private E properties;
  private Map<String, Object> externalFilters;

  public LogicalOperator getOperator() {
    return operator;
  }

  @JsonView(View.Public.class)
  public void setOperator(final LogicalOperator operator) {
    this.operator = operator;
  }

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
        .append("operator", operator)
        .append("properties", properties)
        .append("externalFilters", externalFilters)
        .toString();
  }
}
