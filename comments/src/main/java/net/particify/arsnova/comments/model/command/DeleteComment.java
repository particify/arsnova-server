package net.particify.arsnova.comments.model.command;

public class DeleteComment extends WebSocketCommand<DeleteCommentPayload> {
  public DeleteComment() {
    super(DeleteComment.class.getSimpleName());
  }

  public DeleteComment(DeleteCommentPayload p) {
    super(DeleteComment.class.getSimpleName());
    this.payload = p;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DeleteComment that = (DeleteComment) o;
    return this.getPayload().equals(that.getPayload());
  }
}
