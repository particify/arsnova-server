package de.thm.arsnova.service.comment.model.command;

import de.thm.arsnova.service.comment.model.WebSocketPayload;

public class DeleteCommentPayload implements WebSocketPayload {
    private String id;

    public DeleteCommentPayload() {
    }

    public DeleteCommentPayload(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
