package net.particify.arsnova.core.event;

import java.util.Objects;
import org.springframework.core.style.ToStringCreator;

public abstract class RoomAccessBaseEvent {
  private String version;
  private String rev;
  private String roomId;

  public RoomAccessBaseEvent() {
  }

  public RoomAccessBaseEvent(final String version, final String rev, final String roomId) {
    this.version = version;
    this.rev = rev;
    this.roomId = roomId;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public String getRev() {
    return rev;
  }

  public void setRev(final String rev) {
    this.rev = rev;
  }

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(final String roomId) {
    this.roomId = roomId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final RoomAccessBaseEvent that = (RoomAccessBaseEvent) o;
    return Objects.equals(version, that.version)
        && Objects.equals(rev, that.rev)
        && Objects.equals(roomId, that.roomId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, rev, roomId);
  }

  /**
   * Use this helper method to adjust the output of {@link #toString()}.
   * Override this method instead of <tt>toString()</tt> and call <tt>super.buildToString()</tt>.
   * Additional fields can be added to the String by calling
   * {@link org.springframework.core.style.ToStringCreator#append} on the <tt>ToStringCreator</tt>.
   */
  protected ToStringCreator buildToString() {
    final ToStringCreator toStringCreator = new ToStringCreator(this);
    toStringCreator
        .append("version", version)
        .append("rev", rev)
        .append("roomId", roomId);

    return toStringCreator;
  }

  @Override
  public String toString() {
    return buildToString().toString();
  }
}
