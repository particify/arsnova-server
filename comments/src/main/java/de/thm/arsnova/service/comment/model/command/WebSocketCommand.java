package de.thm.arsnova.service.comment.model.command;

import de.thm.arsnova.service.comment.model.WebSocketMessage;
import de.thm.arsnova.service.comment.model.WebSocketPayload;

public class WebSocketCommand<P extends WebSocketPayload> extends WebSocketMessage<P> {
  public WebSocketCommand(String type) {
    super(type);
  }
}
