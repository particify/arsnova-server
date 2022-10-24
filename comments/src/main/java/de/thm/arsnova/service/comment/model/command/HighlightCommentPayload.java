package de.thm.arsnova.service.comment.model.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.arsnova.service.comment.model.WebSocketPayload;

import java.util.Objects;

public class HighlightCommentPayload implements WebSocketPayload {
  private String id;
  private Boolean lights;

  public HighlightCommentPayload() {
  }

  public HighlightCommentPayload(String id) {
    this.id = id;
  }

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  @JsonProperty("lights")
  public Boolean getLights() {
    return lights;
  }

  @JsonProperty("lights")
  public void setLights(Boolean lights) {
    this.lights = lights;
  }

  @Override
  public String toString() {
    return "HighlightCommentPayload{" +
        "id='" + id + '\'' +
        ", lights=" + lights +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HighlightCommentPayload that = (HighlightCommentPayload) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(lights, that.lights);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, lights);
  }
}
