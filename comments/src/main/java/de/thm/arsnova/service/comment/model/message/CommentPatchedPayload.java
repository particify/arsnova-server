package de.thm.arsnova.service.comment.model.message;

import de.thm.arsnova.service.comment.model.Comment;

import java.util.Map;

public class CommentPatchedPayload implements WebSocketPayload {
    private String id;
    private Map<String, Object> changes;

    public CommentPatchedPayload(
            final String id,
            final Map<String, Object> changes
    ) {
        this.id = id;
        this.changes = changes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getChanges() {
        return changes;
    }

    public void setChanges(Map<String, Object> changes) {
        this.changes = changes;
    }
}
