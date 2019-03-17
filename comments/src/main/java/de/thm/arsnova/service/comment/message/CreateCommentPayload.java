package de.thm.arsnova.service.comment.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class CreateCommentPayload implements WebSocketPayload {
    private String creatorId;
    private String roomId;
    private String subject;
    private String body;

    @JsonProperty("creatorId")
    public String getCreatorId() {
        return creatorId;
    }

    @JsonProperty("creatorId")
    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    @JsonProperty("roomId")
    public String getRoomId() {
        return roomId;
    }

    @JsonProperty("roomId")
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    @JsonProperty("subject")
    public String getSubject() {
        return subject;
    }

    @JsonProperty("subject")
    public void setSubject(String subject) {
        this.subject = subject;
    }

    @JsonProperty("body")
    public String getBody() {
        return body;
    }

    @JsonProperty("body")
    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "CreateCommentPayload{" +
                "creatorId='" + creatorId + '\'' +
                ", roomId='" + roomId + '\'' +
                ", subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateCommentPayload that = (CreateCommentPayload) o;
        return Objects.equals(creatorId, that.creatorId) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {

        return Objects.hash(creatorId, subject, body);
    }
}
