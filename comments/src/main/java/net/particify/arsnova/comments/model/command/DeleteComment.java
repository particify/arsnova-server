package net.particify.arsnova.comments.model.command;

public class DeleteComment extends WebSocketCommand<DeleteCommentPayload> {
  public DeleteComment() {
    super(DeleteComment.class.getSimpleName());
  }

  public DeleteComment(DeleteCommentPayload p) {
    super(DeleteComment.class.getSimpleName());
    this.payload = p;
  }
}
