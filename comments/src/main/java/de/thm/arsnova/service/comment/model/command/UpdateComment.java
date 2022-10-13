package de.thm.arsnova.service.comment.model.command;

public class UpdateComment extends WebSocketCommand<UpdateCommentPayload> {
  public UpdateComment() {
    super(UpdateComment.class.getSimpleName());
  }
  public UpdateComment(UpdateCommentPayload p) {
    super(UpdateComment.class.getSimpleName());
    this.payload = p;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UpdateComment that = (UpdateComment) o;
    return this.getPayload().equals(that.getPayload());
  }
}
