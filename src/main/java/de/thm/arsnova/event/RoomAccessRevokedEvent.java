package de.thm.arsnova.event;

public class RoomAccessRevokedEvent extends RoomAccessEvent {
	public RoomAccessRevokedEvent() {
	}

	public RoomAccessRevokedEvent(
			final String version,
			final String roomId,
			final String userId
	) {
		super(version, roomId, userId);
	}
}
