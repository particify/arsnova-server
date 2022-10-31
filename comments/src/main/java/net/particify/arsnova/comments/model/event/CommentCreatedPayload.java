package net.particify.arsnova.comments.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.Objects;

import net.particify.arsnova.comments.model.Comment;
import net.particify.arsnova.comments.model.WebSocketPayload;

public class CommentCreatedPayload implements WebSocketPayload {
  private String id;
  private String body;
  private String tag;
  private Date timestamp;
  private String answer;
  private boolean favorite;
  private int correct;
  private boolean read;

  public CommentCreatedPayload() {}

  public CommentCreatedPayload(Comment c) {
    if (c != null) {
      id = c.getId();
      body = c.getBody();
      tag = c.getTag();
      answer = c.getAnswer();
      favorite = c.isFavorite();
      correct = c.getCorrect();
      read = c.isRead();
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

  @JsonProperty("body")
  public String getBody() {
    return body;
  }

  @JsonProperty("body")
  public void setBody(String body) {
    this.body = body;
  }

  @JsonProperty("tag")
  public String getTag() {
    return tag;
  }

  @JsonProperty("tag")
  public void setTag(String tag) {
    this.tag = tag;
  }

  @JsonProperty("timestamp")
  public Date getTimestamp() {
    return timestamp;
  }

  @JsonProperty("timestamp")
  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  @JsonProperty("answer")
  public String getAnswer() {
    return answer;
  }

  @JsonProperty("answer")
  public void setAnswer(final String answer) {
    this.answer = answer;
  }

  @JsonProperty("favorite")
  public boolean isFavorite() {
    return favorite;
  }

  @JsonProperty("favorite")
  public void setFavorite(final boolean favorite) {
    this.favorite = favorite;
  }

  @JsonProperty("correct")
  public int getCorrect() {
    return correct;
  }

  @JsonProperty("correct")
  public void setCorrect(final int correct) {
    this.correct = correct;
  }

  @JsonProperty("read")
  public boolean isRead() {
    return read;
  }

  @JsonProperty("read")
  public void setRead(final boolean read) {
    this.read = read;
  }

  @Override
  public String toString() {
    return "CommentCreatedPayload{" +
        "id='" + id + '\'' +
        ", body='" + body + '\'' +
        ", tag='" + tag + '\'' +
        ", timestamp=" + timestamp +
        ", answer='" + answer + '\'' +
        ", favorite=" + favorite +
        ", correct=" + correct +
        ", read=" + read +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final CommentCreatedPayload that = (CommentCreatedPayload) o;
    return favorite == that.favorite &&
        correct == that.correct &&
        read == that.read &&
        Objects.equals(id, that.id) &&
        Objects.equals(body, that.body) &&
        Objects.equals(tag, that.tag) &&
        Objects.equals(timestamp, that.timestamp) &&
        Objects.equals(answer, that.answer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, body, tag, timestamp, answer, favorite, correct, read);
  }
}
