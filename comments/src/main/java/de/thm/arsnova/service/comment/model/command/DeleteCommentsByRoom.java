package de.thm.arsnova.service.comment.model.command;

public class DeleteCommentsByRoom extends WebSocketCommand<DeleteCommentsByRoomPayload> {
    public DeleteCommentsByRoom() {
        super(DeleteCommentsByRoom.class.getSimpleName());
    }

    public DeleteCommentsByRoom(DeleteCommentsByRoomPayload p) {
        super(DeleteCommentsByRoom.class.getSimpleName());
        this.payload = p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeleteCommentsByRoom that = (DeleteCommentsByRoom) o;
        return this.getPayload().equals(that.getPayload());
    }
}

