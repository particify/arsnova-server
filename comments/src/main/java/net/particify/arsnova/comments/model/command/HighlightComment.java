package net.particify.arsnova.comments.model.command;

public class HighlightComment extends WebSocketCommand<HighlightCommentPayload> {
  public HighlightComment() {
    super(HighlightComment.class.getSimpleName());
  }

  public HighlightComment(HighlightCommentPayload p) {
    super(DeleteComment.class.getSimpleName());
    this.payload = p;
  }
}
