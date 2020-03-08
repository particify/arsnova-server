package de.thm.arsnova.event;

import java.util.Objects;
import org.springframework.core.style.ToStringCreator;

public class RoomAccessGrantedEvent extends RoomAccessEvent {
	private String role;

	public RoomAccessGrantedEvent() {
	}

	public RoomAccessGrantedEvent(
			final String version,
			final String roomId,
			final String userId,
			final String role
	) {
		super(version, roomId, userId);
		this.role = role;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
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
		final RoomAccessGrantedEvent that = (RoomAccessGrantedEvent) o;
		return Objects.equals(role, that.role);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), role);
	}

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("role", role);
	}
}
