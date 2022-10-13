package de.thm.arsnova.websocket.message;

import de.thm.arsnova.model.TextAnswer;

public class TextAnswerCreated extends WebSocketMessage<TextAnswerCreated.TextAnswerCreatedPayload> {
  public TextAnswerCreated(final TextAnswer textAnswer) {
    super(TextAnswerCreated.class.getSimpleName());
    this.setPayload(new TextAnswerCreatedPayload(textAnswer));
  }

  public class TextAnswerCreatedPayload implements WebSocketPayload {
    private String id;
    private String body;

    public TextAnswerCreatedPayload(final TextAnswer textAnswer) {
      id = textAnswer.getId();
      body = textAnswer.getBody();
    }

    public String getId() {
      return id;
    }

    public String getBody() {
      return body;
    }
  }
}
