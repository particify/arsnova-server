package net.particify.arsnova.comments.model.command;

public class PatchComment extends WebSocketCommand<PatchCommentPayload> {
  public PatchComment() {
    super(PatchComment.class.getSimpleName());
  }

  public PatchComment(PatchCommentPayload p) {
    super(PatchComment.class.getSimpleName());
    this.payload = p;
  }
}
