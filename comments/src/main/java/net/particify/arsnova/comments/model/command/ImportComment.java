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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ImportComment that = (ImportComment) o;
    return this.getPayload().equals(that.getPayload());
  }
}
