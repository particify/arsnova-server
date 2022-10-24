package de.thm.arsnova.websocket.message;

public class ResetFeedback extends WebSocketMessage<ResetFeedbackPayload> {
  public ResetFeedback() {
    super(ResetFeedback.class.getSimpleName());
  }
}
