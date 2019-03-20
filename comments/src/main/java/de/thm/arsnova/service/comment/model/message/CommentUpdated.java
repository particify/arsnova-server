package de.thm.arsnova.service.comment.model.message;

public class CommentUpdated extends WebSocketMessage<CommentUpdatedPayload> {
    public CommentUpdated() {
        super(UpdateComment.class.getSimpleName());
    }

    public CommentUpdated(CommentUpdatedPayload p) {
        super(UpdateComment.class.getSimpleName());
        this.payload = p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentUpdated that = (CommentUpdated) o;
        return this.getPayload().equals(that.getPayload());
    }
}
