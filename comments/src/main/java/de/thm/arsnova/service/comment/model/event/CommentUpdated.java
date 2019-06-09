package de.thm.arsnova.service.comment.model.event;

import de.thm.arsnova.service.comment.model.command.UpdateComment;

public class CommentUpdated extends WebSocketEvent<CommentUpdatedPayload> {
    public CommentUpdated() {
        super(UpdateComment.class.getSimpleName());
    }

    public CommentUpdated(CommentUpdatedPayload p, String id) {
        super(CommentUpdated.class.getSimpleName(), id);
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
