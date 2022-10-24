package de.thm.arsnova.event;

import java.util.List;
import java.util.Objects;
import org.springframework.core.style.ToStringCreator;

public class RoomAccessSyncEvent extends RoomAccessBaseEvent {
  public static class RoomAccessEntry {
    private String userId;
    private String role;

    public RoomAccessEntry() {
    }

    public RoomAccessEntry(final String userId, final String role) {
      this.userId = userId;
      this.role = role;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(final String userId) {
      this.userId = userId;
    }

    public String getRole() {
      return role;
    }

    public void setRole(final String role) {
      this.role = role;
    }

    @Override
    public String toString() {
      return new ToStringCreator(this)
          .append("userId", userId)
          .append("role", role)
          .toString();
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final RoomAccessEntry that = (RoomAccessEntry) o;
      return Objects.equals(userId, that.userId)
          && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
      return Objects.hash(userId, role);
    }
  }

  private List<RoomAccessEntry> access;

  public RoomAccessSyncEvent() {
  }

  public RoomAccessSyncEvent(
      final String version,
      final String rev,
      final String roomId,
      final List<RoomAccessEntry> acccess) {
    super(version, rev, roomId);
    this.access = acccess;
  }

  public List<RoomAccessEntry> getAccess() {
    return access;
  }

  public void setAccess(final List<RoomAccessEntry> access) {
    this.access = access;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final RoomAccessSyncEvent that = (RoomAccessSyncEvent) o;
    return Objects.equals(access, that.access);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), access);
  }

  @Override
  protected ToStringCreator buildToString() {
    return super.buildToString()
        .append("role", access);
  }
}
