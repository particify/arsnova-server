package net.particify.arsnova.comments.model.event;

import java.util.UUID;

public class CommentHighlighted extends WebSocketEvent<CommentHighlightedPayload> {
  public CommentHighlighted() {
    super(CommentHighlighted.class.getSimpleName());
  }

  public CommentHighlighted(CommentHighlightedPayload p, UUID id) {
    super(CommentHighlighted.class.getSimpleName(), id);
    this.payload = p;
  }
}
