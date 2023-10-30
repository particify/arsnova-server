package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;

import net.particify.arsnova.core.model.serialization.View;

public class ContentTemplate extends Entity {
  @JsonIgnoreProperties({"state"})
  @JsonUnwrapped
  private Content content;

  public ContentTemplate() {

  }

  public ContentTemplate(final Content content) {
    content.setId(null);
    content.setRevision(null);
    content.setRoomId(null);
    content.setState(null);
    this.content = content;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public Content getContent() {
    return content;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setContent(final Content content) {
    this.content = content;
  }
}
