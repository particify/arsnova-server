package net.particify.arsnova.core.websocket.message;

import java.util.Objects;

public class WebSocketMessage<P extends WebSocketPayload> {
  private String type;

  private P payload;

  public WebSocketMessage(final String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public P getPayload() {
    return payload;
  }

  public void setPayload(final P payload) {
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
    final FeedbackChanged that = (FeedbackChanged) o;
    return this.type.equals(that.getType())
        && this.payload.equals(that.getPayload());
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, payload);
  }
}
