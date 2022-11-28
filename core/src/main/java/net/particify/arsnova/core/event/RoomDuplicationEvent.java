package net.particify.arsnova.core.event;

import java.io.Serial;
import org.springframework.context.ApplicationEvent;

import net.particify.arsnova.core.model.Room;

public class RoomDuplicationEvent extends ApplicationEvent {
  @Serial
  private static final long serialVersionUID = 1L;

  private final transient Room originalRoom;
  private final transient Room duplicateRoom;

  public RoomDuplicationEvent(final Object source, final Room originalRoom, final Room duplicateRoom) {
    super(source);
    this.originalRoom = originalRoom;
    this.duplicateRoom = duplicateRoom;
  }

  public Room getOriginalRoom() {
    return originalRoom;
  }

  public Room getDuplicateRoom() {
    return duplicateRoom;
  }
}
