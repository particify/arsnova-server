package de.thm.arsnova.service.comment.model.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.arsnova.service.comment.model.WebSocketPayload;

import java.util.Map;
import java.util.Objects;

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


    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
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

    @Override
    public String toString() {
        return "PatchCommentPayload{" +
                "id='" + id + '\'' +
                ", changes=" + changes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatchCommentPayload that = (PatchCommentPayload) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(changes, that.changes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, changes);
    }
}
