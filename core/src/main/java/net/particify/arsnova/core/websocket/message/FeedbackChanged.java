package net.particify.arsnova.core.websocket.message;

public class FeedbackChanged extends WebSocketMessage<FeedbackChangedPayload> {
  public FeedbackChanged() {
    super(FeedbackChanged.class.getSimpleName());
  }
}
