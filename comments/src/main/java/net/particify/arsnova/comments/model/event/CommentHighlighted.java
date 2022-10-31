package net.particify.arsnova.comments.model.event;

public class CommentHighlighted extends WebSocketEvent<CommentHighlightedPayload> {
  public CommentHighlighted() {
    super(CommentHighlighted.class.getSimpleName());
  }

  public CommentHighlighted(CommentHighlightedPayload p, String id) {
    super(CommentHighlighted.class.getSimpleName(), id);
    this.payload = p;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CommentHighlighted that = (CommentHighlighted) o;
    return this.getPayload().equals(that.getPayload());
  }
}
