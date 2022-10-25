package net.particify.arsnova.core.websocket.message;

public class FeedbackStopped extends WebSocketMessage<WebSocketPayload> {
  public FeedbackStopped() {
    super(FeedbackStopped.class.getSimpleName());
  }
}
