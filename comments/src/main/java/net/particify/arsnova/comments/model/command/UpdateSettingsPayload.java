package net.particify.arsnova.comments.model.command;

import java.util.Objects;
import java.util.UUID;

import net.particify.arsnova.comments.model.Settings;
import net.particify.arsnova.comments.model.WebSocketPayload;

public class UpdateSettingsPayload implements WebSocketPayload {
  private UUID roomId;
  private boolean directSend;
  private boolean fileUploadEnabled;
  private boolean readonly;
  private boolean disabled;

  public UpdateSettingsPayload() {
  }

  public UpdateSettingsPayload(Settings settings) {
    roomId = settings.getRoomId();
    directSend = settings.getDirectSend();
    fileUploadEnabled = settings.isFileUploadEnabled();
    readonly = settings.isReadonly();
    disabled = settings.isDisabled();
  }

  public UpdateSettingsPayload(UUID roomId, boolean directSend) {
    this.roomId = roomId;
    this.directSend = directSend;
  }

  public UUID getRoomId() {
    return roomId;
  }

  public void setRoomId(UUID roomId) {
    this.roomId = roomId;
  }

  public boolean getDirectSend() {
    return directSend;
  }

  public void setDirectSend(boolean directSend) {
    this.directSend = directSend;
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
