package net.particify.arsnova.comments.model.event;

public class CommentDeleted extends WebSocketEvent<CommentDeletedPayload> {
  public CommentDeleted() {
    super(CommentDeleted.class.getSimpleName());
  }

  public CommentDeleted(CommentDeletedPayload p, String roomId) {
    super(CommentDeleted.class.getSimpleName(), roomId);
    this.payload = p;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CommentDeleted that = (CommentDeleted) o;
    if (this.getRoomId() != that.getRoomId());
    return (this.getPayload().equals(that.getPayload()));
  }
}
