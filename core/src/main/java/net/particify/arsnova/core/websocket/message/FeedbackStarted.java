package net.particify.arsnova.core.websocket.message;

public class FeedbackStarted extends WebSocketMessage<WebSocketPayload> {
  public FeedbackStarted() {
    super(FeedbackStarted.class.getSimpleName());
  }
}
