package net.particify.arsnova.comments.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Objects;

@Entity
public class Settings {
  @Id
  private String roomId;
  private Boolean directSend;
  private Boolean fileUploadEnabled;
  private Boolean readonly;
  private Boolean disabled;

  public Settings() {
  }

  public Boolean getDirectSend() {
    return directSend;
  }

  public void setDirectSend(Boolean directSend) {
    this.directSend = directSend;
  }

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }

  public Boolean isFileUploadEnabled() {
    return fileUploadEnabled;
  }

  public void setFileUploadEnabled(final Boolean fileUploadEnabled) {
    this.fileUploadEnabled = fileUploadEnabled;
  }

  public Boolean isReadonly() {
    return readonly;
  }

  public void setReadonly(Boolean readonly) {
    this.readonly = readonly;
  }

  public Boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(Boolean disabled) {
    this.disabled = disabled;
  }

  @Override
  public String toString() {
    return "Settings{" +
        "roomId='" + roomId + '\'' +
        ", directSend=" + directSend +
        ", fileUploadEnabled=" + fileUploadEnabled +
        ", readonly=" + readonly +
        ", disabled" + disabled +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Settings settings = (Settings) o;
    return
        Objects.equals(roomId, settings.roomId)
        && Objects.equals(directSend, settings.directSend)
        && Objects.equals(fileUploadEnabled, settings.fileUploadEnabled)
        && Objects.equals(readonly, settings.readonly)
        && Objects.equals(disabled, settings.disabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId, directSend, fileUploadEnabled, readonly, disabled);
  }
}
