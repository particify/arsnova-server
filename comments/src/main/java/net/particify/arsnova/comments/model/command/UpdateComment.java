package net.particify.arsnova.comments.model.command;

public class UpdateComment extends WebSocketCommand<UpdateCommentPayload> {
  public UpdateComment() {
    super(UpdateComment.class.getSimpleName());
  }
  public UpdateComment(UpdateCommentPayload p) {
    super(UpdateComment.class.getSimpleName());
    this.payload = p;
  }
}
