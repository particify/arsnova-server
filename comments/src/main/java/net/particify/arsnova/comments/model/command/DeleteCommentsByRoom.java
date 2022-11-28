package net.particify.arsnova.comments.model.command;

public class DeleteCommentsByRoom extends WebSocketCommand<DeleteCommentsByRoomPayload> {
  public DeleteCommentsByRoom() {
    super(DeleteCommentsByRoom.class.getSimpleName());
  }

  public DeleteCommentsByRoom(DeleteCommentsByRoomPayload p) {
    super(DeleteCommentsByRoom.class.getSimpleName());
    this.payload = p;
  }
}

