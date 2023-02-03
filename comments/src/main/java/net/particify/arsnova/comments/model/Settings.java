package net.particify.arsnova.comments.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Objects;

@Entity
public class Settings {
  @Id
  private String roomId;
  private boolean directSend = true;
  private boolean fileUploadEnabled;
  private boolean readonly;
  private boolean disabled;

  public Settings() {
  }

  public boolean getDirectSend() {
    return directSend;
  }

  public void setDirectSend(boolean directSend) {
    this.directSend = directSend;
  }

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }

  public boolean isFileUploadEnabled() {
    return fileUploadEnabled;
  }

  public void setFileUploadEnabled(final boolean fileUploadEnabled) {
    this.fileUploadEnabled = fileUploadEnabled;
  }

  public boolean isReadonly() {
    return readonly;
  }

  public void setReadonly(boolean readonly) {
    this.readonly = readonly;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
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
