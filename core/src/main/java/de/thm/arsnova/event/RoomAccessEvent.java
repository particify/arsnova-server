package de.thm.arsnova.event;

import java.util.Objects;
import org.springframework.core.style.ToStringCreator;

public abstract class RoomAccessEvent extends RoomAccessBaseEvent {
	private String userId;

	public RoomAccessEvent() {
	}

	public RoomAccessEvent(
			final String version,
			final String rev,
			final String roomId,
			final String userId
	) {
		super(version, rev, roomId);
		this.userId = userId;
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
		if (!super.equals(o)) {
			return false;
		}
		final RoomAccessEvent that = (RoomAccessEvent) o;
		return Objects.equals(userId, that.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), userId);
	}

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("userId", userId);
	}
}
