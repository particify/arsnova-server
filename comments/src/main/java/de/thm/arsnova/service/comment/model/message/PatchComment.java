package de.thm.arsnova.service.comment.model.message;

public class PatchComment extends WebSocketMessage<PatchCommentPayload> {
    public PatchComment() {
        super(PatchComment.class.getSimpleName());
    }

    public PatchComment(PatchCommentPayload p) {
        super(PatchComment.class.getSimpleName());
        this.payload = p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatchComment that = (PatchComment) o;
        return this.getPayload().equals(that.getPayload());
    }
}
