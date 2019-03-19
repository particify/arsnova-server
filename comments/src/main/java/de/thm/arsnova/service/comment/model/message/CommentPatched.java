package de.thm.arsnova.service.comment.model.message;

public class CommentPatched extends WebSocketMessage<CommentPatchedPayload> {
    public CommentPatched() {
        super(CommentPatched.class.getSimpleName());
    }

    public CommentPatched(CommentPatchedPayload p) {
        super(CommentPatched.class.getSimpleName());
        this.payload = p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentPatched that = (CommentPatched) o;
        return this.getPayload().equals(that.getPayload());
    }
}
