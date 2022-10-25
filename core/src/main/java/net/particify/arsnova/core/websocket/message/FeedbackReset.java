package net.particify.arsnova.core.websocket.message;

public class FeedbackReset extends WebSocketMessage<WebSocketPayload> {
  public FeedbackReset() {
    super(FeedbackReset.class.getSimpleName());
  }
}
