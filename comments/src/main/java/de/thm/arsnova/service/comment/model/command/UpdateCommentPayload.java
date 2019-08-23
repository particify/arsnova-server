package de.thm.arsnova.service.comment.model.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.WebSocketPayload;

import java.util.Objects;

public class UpdateCommentPayload implements WebSocketPayload {
    private String id;
    private String body;
    private boolean read;
    private boolean favorite;
    private int correct;

    public UpdateCommentPayload() {
    }

    public UpdateCommentPayload(Comment c) {
        this.id = c.getId();
        this.body = c.getBody();
        this.read = c.isRead();
        this.favorite = c.isFavorite();
        this.correct = c.getCorrect();
    }

    public UpdateCommentPayload(String id, String body, boolean read, boolean favorite, int correct) {
        this.id = id;
        this.body = body;
        this.read = read;
        this.favorite = favorite;
        this.correct = correct;
    }

    @JsonProperty("roomId")
    public String getId() {
        return id;
    }

    @JsonProperty("roomId")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("body")
    public String getBody() {
        return body;
    }

    @JsonProperty("body")
    public void setBody(String body) {
        this.body = body;
    }

    @JsonProperty("read")
    public boolean isRead() {
        return read;
    }

    @JsonProperty("read")
    public void setRead(boolean read) {
        this.read = read;
    }

    @JsonProperty("favorite")
    public boolean isFavorite() {
        return favorite;
    }

    @JsonProperty("favorite")
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    @JsonProperty("correct")
    public int getCorrect() {
        return correct;
    }

    @JsonProperty("correct")
    public void setCorrect(int correct) {
        this.correct = correct;
    }

    @Override
    public String toString() {
        return "UpdateCommentPayload{" +
                "id='" + id + '\'' +
                ", body='" + body + '\'' +
                ", read=" + read +
                ", favorite=" + favorite +
                ", correct=" + correct +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateCommentPayload that = (UpdateCommentPayload) o;
        return read == that.read &&
                favorite == that.favorite &&
                correct == that.correct &&
                Objects.equals(id, that.id) &&
                Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, body, read, favorite, correct);
    }
}
