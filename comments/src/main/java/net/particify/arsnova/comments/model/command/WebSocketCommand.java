package net.particify.arsnova.comments.model.command;

import net.particify.arsnova.comments.model.WebSocketMessage;
import net.particify.arsnova.comments.model.WebSocketPayload;

public class WebSocketCommand<P extends WebSocketPayload> extends WebSocketMessage<P> {
  public WebSocketCommand(String type) {
    super(type);
  }
}
