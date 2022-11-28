package net.particify.arsnova.comments.model.event;

public class CommentHighlighted extends WebSocketEvent<CommentHighlightedPayload> {
  public CommentHighlighted() {
    super(CommentHighlighted.class.getSimpleName());
  }

  public CommentHighlighted(CommentHighlightedPayload p, String id) {
    super(CommentHighlighted.class.getSimpleName(), id);
    this.payload = p;
  }
}
