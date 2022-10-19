package de.thm.arsnova.service.comment.model.command;

public class HighlightComment extends WebSocketCommand<HighlightCommentPayload> {
    public HighlightComment() {
        super(HighlightComment.class.getSimpleName());
    }

    public HighlightComment(HighlightCommentPayload p) {
        super(DeleteComment.class.getSimpleName());
        this.payload = p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HighlightComment that = (HighlightComment) o;
        return this.getPayload().equals(that.getPayload());
    }
}
