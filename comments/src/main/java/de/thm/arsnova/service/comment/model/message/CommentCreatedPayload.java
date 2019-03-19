package de.thm.arsnova.service.comment.model.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.arsnova.service.comment.model.Comment;

import java.util.Date;
import java.util.Objects;

public class CommentCreatedPayload implements WebSocketPayload {
    private String id;
    private String subject;
    private String body;
    private Date timestamp;

    public CommentCreatedPayload() {}

    public CommentCreatedPayload(Comment c) {
        if (c != null) {
            id = c.getId();
            subject = c.getSubject();
            body = c.getBody();
        }
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
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

    @JsonProperty("timestamp")
    public Date getTimestamp() {
        return timestamp;
    }

    @JsonProperty("timestamp")
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "CommentCreatedPayload{" +
                "subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentCreatedPayload that = (CommentCreatedPayload) o;
        return Objects.equals(subject, that.subject) &&
                Objects.equals(body, that.body) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {

        return Objects.hash(subject, body, timestamp);
    }
}
