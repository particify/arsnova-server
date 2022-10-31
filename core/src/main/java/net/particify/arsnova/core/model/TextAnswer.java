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

package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Date;
import javax.validation.constraints.Size;
import org.springframework.core.style.ToStringCreator;

import net.particify.arsnova.core.model.serialization.View;

public class TextAnswer extends Answer {
  private String subject;

  // Validation: null is allowed for abstentions
  @Size(min = 1, max = 500)
  private String body;

  private boolean read;
  private boolean hidden;

  public TextAnswer() {

  }

  public TextAnswer(final Content content, final String creatorId) {
    super(content, creatorId);
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getSubject() {
    return null;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setSubject(final String subject) {
    this.subject = subject;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getBody() {
    if (subject != null && !subject.isBlank()) {
      return subject + ": " + body;
    } else {
      return body;
    }
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setBody(final String body) {
    this.body = body;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public boolean isRead() {
    return read;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setRead(final boolean read) {
    this.read = read;
  }


  @JsonView(View.Persistence.class)
  public boolean isHidden() {
    return hidden;
  }

  @JsonView(View.Persistence.class)
  public void setHidden(final boolean hidden) {
    this.hidden = hidden;
  }

  @Override
  @JsonView({View.Persistence.class, View.Public.class})
  public Date getCreationTimestamp() {
    return creationTimestamp;
  }

  @Override
  public boolean isAbstention() {
    return body == null;
  }

  @Override
  protected ToStringCreator buildToString() {
    return super.buildToString()
        .append("subject", subject)
        .append("body", body)
        .append("read", read)
        .append("hidden", hidden);
  }
}
