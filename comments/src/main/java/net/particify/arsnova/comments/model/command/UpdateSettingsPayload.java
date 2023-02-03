package net.particify.arsnova.comments.model.command;

import java.util.Objects;

import net.particify.arsnova.comments.model.Settings;
import net.particify.arsnova.comments.model.WebSocketPayload;

public class UpdateSettingsPayload implements WebSocketPayload {
  private String roomId;
  private Boolean directSend;
  private Boolean fileUploadEnabled;
  private Boolean readonly;
  private Boolean disabled;

  public UpdateSettingsPayload() {
  }

  public UpdateSettingsPayload(Settings settings) {
    roomId = settings.getRoomId();
    directSend = settings.getDirectSend();
    fileUploadEnabled = settings.isFileUploadEnabled();
    readonly = settings.isReadonly();
    disabled = settings.isDisabled();
  }

  public UpdateSettingsPayload(String roomId, Boolean directSend) {
    this.roomId = roomId;
    this.directSend = directSend;
  }

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }

  public Boolean getDirectSend() {
    return directSend;
  }

  public void setDirectSend(Boolean directSend) {
    this.directSend = directSend;
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
    return "UpdateSettingsPayload{" +
        "roomId='" + roomId + '\'' +
        ", directSend=" + directSend +
        ", fileUploadEnabled=" + fileUploadEnabled +
        ", readonly=" + readonly +
        ", disabled=" + disabled +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final UpdateSettingsPayload that = (UpdateSettingsPayload) o;
    return
        Objects.equals(roomId, that.roomId)
        && Objects.equals(directSend, that.directSend)
        && Objects.equals(fileUploadEnabled, that.fileUploadEnabled)
        && Objects.equals(readonly, that.readonly)
        && Objects.equals(disabled, that.disabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId, directSend, fileUploadEnabled, readonly, disabled);
  }
}
