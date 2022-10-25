package net.particify.arsnova.core.websocket.message;

public class FeedbackChanged extends WebSocketMessage<FeedbackChangedPayload> {
  public FeedbackChanged() {
    super(FeedbackChanged.class.getSimpleName());
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
    return this.getPayload().equals(that.getPayload());
  }
}
