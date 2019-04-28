package de.thm.arsnova.service.comment.model.event;

import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.WebSocketPayload;

import java.util.Objects;

public class CommentHighlightedPayload implements WebSocketPayload {
    private String id;
    private Boolean light;

    public CommentHighlightedPayload() {
    }

    public CommentHighlightedPayload(Comment c, Boolean light) {
        if (c != null) {
            id = c.getId();
            light = light;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getLight() {
        return light;
    }

    public void setLight(Boolean light) {
        this.light = light;
    }

    @Override
    public String toString() {
        return "CommentHighlightedPayload{" +
                "id='" + id + '\'' +
                ", light=" + light +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentHighlightedPayload that = (CommentHighlightedPayload) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(light, that.light);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, light);
    }
}
