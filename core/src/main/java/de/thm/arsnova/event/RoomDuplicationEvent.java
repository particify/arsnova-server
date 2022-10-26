package de.thm.arsnova.event;

import org.springframework.context.ApplicationEvent;

import de.thm.arsnova.model.Room;

public class RoomDuplicationEvent extends ApplicationEvent {
	private Room originalRoom;
	private Room duplicateRoom;

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
