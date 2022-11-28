package net.particify.arsnova.comments.model.command;

import java.io.Serializable;

public class ImportComment extends WebSocketCommand<ImportCommentPayload> implements Serializable {
  public ImportComment() {
    super(ImportComment.class.getSimpleName());
  }

  public ImportComment(ImportCommentPayload p) {
    super(ImportComment.class.getSimpleName());
    this.payload = p;
  }
}
