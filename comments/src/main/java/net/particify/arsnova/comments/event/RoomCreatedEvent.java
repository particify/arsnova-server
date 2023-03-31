package net.particify.arsnova.comments.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
class RoomCreatedEvent {
  private UUID id;
  private UUID tenantId;

  public UUID getId() {
    return id;
  }

  public void setId(final UUID id) {
    this.id = id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(final UUID tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public String toString() {
    return "RoomCreatedEvent{" +
      "id='" + id + '\'' +
      ", tenantId='" + tenantId + '\'' +
      '}';
  }
}
