package net.particify.arsnova.comments.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;

import net.particify.arsnova.comments.model.command.CreateComment;

public class WebSocketMessage<P extends WebSocketPayload> implements Serializable {
  protected String type;

  protected P payload;

  public WebSocketMessage(String type) {
    this.type = type;
  }

  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  @JsonProperty("payload")
  public P getPayload() {
    return payload;
  }

  @JsonProperty("payload")
  public void setPayload(P payload) {
    this.payload = payload;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final WebSocketMessage<?> that = (WebSocketMessage<?>) o;

    return Objects.equals(type, that.type) && Objects.equals(payload, that.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, payload);
  }

  @Override
  public String toString() {
    return "WebSocketMessage{" +
        "type='" + type + '\'' +
        ", payload=" + payload.toString() +
        '}';
  }
}
