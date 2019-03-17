package de.thm.arsnova.service.comment.message;

public class CommentCreated extends WebSocketMessage<CommentCreatedPayload> {
    public CommentCreated() {
        super(CommentCreated.class.getSimpleName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentCreated that = (CommentCreated) o;
        return this.getPayload().equals(that.getPayload());
    }
}
