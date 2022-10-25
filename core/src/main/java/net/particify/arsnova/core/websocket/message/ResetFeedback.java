package net.particify.arsnova.core.websocket.message;

public class ResetFeedback extends WebSocketMessage<ResetFeedbackPayload> {
  public ResetFeedback() {
    super(ResetFeedback.class.getSimpleName());
  }
}
