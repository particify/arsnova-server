package net.particify.arsnova.comments.model.command;

import java.io.Serializable;

public class CreateComment extends WebSocketCommand<CreateCommentPayload> implements Serializable {
  public CreateComment() {
    super(CreateComment.class.getSimpleName());
  }

  public CreateComment(CreateCommentPayload p) {
    super(CreateComment.class.getSimpleName());
    this.payload = p;
  }
}
