package de.thm.arsnova.service.comment.model.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.arsnova.service.comment.model.WebSocketPayload;

import java.util.Map;

public class PatchCommentPayload implements WebSocketPayload {
    private String id;
    private Map<String, Object> changes;

    public PatchCommentPayload() {
    }

    public PatchCommentPayload(
            final String id,
            final Map<String, Object> changes
    ) {
        this.id = id;
        this.changes = changes;
    }


    @JsonProperty("roomId")
    public String getId() {
        return id;
    }

    @JsonProperty("roomId")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("changes")
    public Map<String, Object> getChanges() {
        return changes;
    }

    @JsonProperty("changes")
    public void setChanges(Map<String, Object> changes) {
        this.changes = changes;
    }
}
