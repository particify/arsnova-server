package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.time.LocalDateTime;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.security.RoomRole;

public class AccessToken extends Entity implements RoomIdAware {
	private String roomId;
	private RoomRole role;
	private String token;
	private LocalDateTime expirationDate;
	private String userId;

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getRoomId() {
		return roomId;
	}

	@JsonView(View.Persistence.class)
	public void setRoomId(final String roomId) {
		this.roomId = roomId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public RoomRole getRole() {
		return role;
	}

	@JsonView(View.Persistence.class)
	public void setRole(final RoomRole role) {
		this.role = role;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getToken() {
		return token;
	}

	@JsonView(View.Persistence.class)
	public void setToken(final String token) {
		this.token = token;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public LocalDateTime getExpirationDate() {
		return expirationDate;
	}

	@JsonView(View.Persistence.class)
	public void setExpirationDate(final LocalDateTime expirationDate) {
		this.expirationDate = expirationDate;
	}

	@JsonView(View.Persistence.class)
	public String getUserId() {
		return userId;
	}

	@JsonView(View.Persistence.class)
	public void setUserId(final String userId) {
		this.userId = userId;
	}

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("roomId", roomId)
				.append("role", role)
				.append("expirationDate", expirationDate)
				.append("userId", userId);
	}
}
