package de.thm.arsnova.service.comment.model.message;

import java.io.Serializable;

public class CreateComment extends WebSocketMessage<CreateCommentPayload> implements Serializable {
    public CreateComment() {
        super(CreateComment.class.getSimpleName());
    }

    public CreateComment(CreateCommentPayload p) {
        super(CreateComment.class.getSimpleName());
        this.payload = p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateComment that = (CreateComment) o;
        return this.getPayload().equals(that.getPayload());
    }
}
