package de.thm.arsnova.event;

public class RoomAccessRevokedEvent extends RoomAccessEvent {
	public RoomAccessRevokedEvent() {
	}

	public RoomAccessRevokedEvent(
			final String version,
			final String rev,
			final String roomId,
			final String userId
	) {
		super(version, rev, roomId, userId);
	}
}
