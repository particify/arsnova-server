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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.View;

public class MigrationState extends Entity {
  public static class Migration {
    private String id;
    private Date start;
    private int step;
    private Object state;

    public Migration() {

    }

    public Migration(final String id, final Date start) {
      this.id = id;
      this.start = start;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public String getId() {
      return id;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public Date getStart() {
      return start;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public int getStep() {
      return step;
    }

    @JsonView(View.Persistence.class)
    public void setStep(final int step) {
      this.step = step;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public Object getState() {
      return state;
    }

    @JsonView(View.Persistence.class)
    public void setState(final Object state) {
      this.state = state;
    }

    @Override
    public String toString() {
      return new ToStringCreator(this)
          .append("id", id)
          .append("start", start)
          .append("step", step)
          .append("state", state)
          .toString();
    }
  }

  public static final String ID = "MigrationState";
  private Migration active;
  private List<String> completed = new ArrayList<>();

  {
    id = ID;
  }

  @Override
  @JsonView(View.Persistence.class)
  public String getId() {
    return ID;
  }

  @Override
  @JsonView(View.Persistence.class)
  public void setId(final String id) {
    if (!id.equals(this.ID)) {
      throw new IllegalArgumentException("ID of this entity must not be changed.");
    }
  }

  @Override
  @JsonView({View.Persistence.class, View.Public.class})
  public String getRevision() {
    return rev;
  }

  @Override
  @JsonView(View.Persistence.class)
  public void setRevision(final String rev) {
    this.rev = rev;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public Migration getActive() {
    return active;
  }

  @JsonView(View.Persistence.class)
  public void setActive(final Migration active) {
    this.active = active;
  }

  public void setActive(final String id, final Date start) {
    this.setActive(new Migration(id, start));
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public List<String> getCompleted() {
    return completed;
  }

  @JsonView(View.Persistence.class)
  public void setCompleted(final List<String> completed) {
    this.completed = completed;
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * All fields of <tt>MigrationState</tt> are included in equality checks.
   * </p>
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!super.equals(o)) {
      return false;
    }
    final MigrationState that = (MigrationState) o;

    return Objects.equals(active, that.active)
        && Objects.equals(completed, that.completed);
  }

  @Override
  protected ToStringCreator buildToString() {
    return super.buildToString()
        .append("active", active)
        .append("completed", completed);
  }
}
