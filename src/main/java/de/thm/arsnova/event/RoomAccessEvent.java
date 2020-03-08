package de.thm.arsnova.event;

import java.util.Objects;
import org.springframework.core.style.ToStringCreator;

public abstract class RoomAccessEvent {
	private String version;
	private String roomId;
	private String userId;

	public RoomAccessEvent() {
	}

	public RoomAccessEvent(
			final String version,
			final String roomId,
			final String userId
	) {
		this.version = version;
		this.roomId = roomId;
		this.userId = userId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(final String version) {
		this.version = version;
	}

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(final String roomId) {
		this.roomId = roomId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(final String userId) {
		this.userId = userId;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final RoomAccessEvent that = (RoomAccessEvent) o;
		return Objects.equals(version, that.version)
				&& Objects.equals(roomId, that.roomId)
				&& Objects.equals(userId, that.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(version, roomId, userId);
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
				.append("roomId", roomId)
				.append("userId", userId);

		return toStringCreator;
	}

	@Override
	public String toString() {
		return buildToString().toString();
	}
}
