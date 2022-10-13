package de.thm.arsnova.service.comment.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.WebSocketPayload;

import java.util.Objects;

public class CommentUpdatedPayload implements WebSocketPayload {
  private String id;
  private String body;
  private boolean read;
  private boolean favorite;
  private int correct;
  private boolean ack;
  private String answer;

  public CommentUpdatedPayload() {
  }

  public CommentUpdatedPayload(Comment c) {
    this.id = c.getId();
    this.body = c.getBody();
    this.read = c.isRead();
    this.favorite = c.isFavorite();
    this.correct = c.getCorrect();
    this.ack = c.isAck();
    this.answer = c.getAnswer();
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

  @JsonProperty("ack")
  public boolean isAck() {
    return ack;
  }

  @JsonProperty("ack")
  public void setAck(boolean ack) {
    this.ack = ack;
  }

  @JsonProperty("answer")
  public String getAnswer() {
    return answer;
  }

  @JsonProperty("answer")
  public void setAnswer(final String answer) {
    this.answer = answer;
  }

  @Override
  public String toString() {
    return "CommentUpdatedPayload{" +
        "id='" + id + '\'' +
        ", body='" + body + '\'' +
        ", read=" + read +
        ", favorite=" + favorite +
        ", correct=" + correct +
        ", ack=" + ack +
        ", answer='" + answer + '\'' +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final CommentUpdatedPayload that = (CommentUpdatedPayload) o;
    return read == that.read &&
        favorite == that.favorite &&
        correct == that.correct &&
        ack == that.ack &&
        Objects.equals(id, that.id) &&
        Objects.equals(body, that.body) &&
        Objects.equals(answer, that.answer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, body, read, favorite, correct, ack, answer);
  }
}
