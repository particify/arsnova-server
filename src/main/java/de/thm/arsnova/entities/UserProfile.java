package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class UserProfile extends Entity {
	public enum AuthProvider {
		NONE,
		UNKNOWN,
		ARSNOVA,
		ARSNOVA_GUEST,
		LDAP,
		CAS,
		GOOGLE,
		FACEBOOK,
		TWITTER
	}

	public static class Account {
		private String password;
		private String activationKey;
		private String passwordResetKey;
		private Date passwordResetTime;

		@JsonView(View.Persistence.class)
		public String getPassword() {
			return password;
		}

		@JsonView(View.Persistence.class)
		public void setPassword(final String password) {
			this.password = password;
		}

		@JsonView(View.Persistence.class)
		public String getActivationKey() {
			return activationKey;
		}

		@JsonView(View.Persistence.class)
		public void setActivationKey(final String activationKey) {
			this.activationKey = activationKey;
		}

		@JsonView(View.Persistence.class)
		public String getPasswordResetKey() {
			return passwordResetKey;
		}

		@JsonView(View.Persistence.class)
		public void setPasswordResetKey(final String passwordResetKey) {
			this.passwordResetKey = passwordResetKey;
		}

		@JsonView(View.Persistence.class)
		public Date getPasswordResetTime() {
			return passwordResetTime;
		}

		@JsonView(View.Persistence.class)
		public void setPasswordResetTime(final Date passwordResetTime) {
			this.passwordResetTime = passwordResetTime;
		}
	}

	public static class RoomHistoryEntry {
		private String roomId;
		private Date lastVisit;

		public RoomHistoryEntry() {

		}

		public RoomHistoryEntry(String roomId, Date lastVisit) {
			this.roomId = roomId;
			this.lastVisit = lastVisit;
		}

		@JsonView(View.Persistence.class)
		public String getRoomId() {
			return roomId;
		}

		@JsonView(View.Persistence.class)
		public void setRoomId(String roomId) {
			this.roomId = roomId;
		}

		@JsonView(View.Persistence.class)
		public Date getLastVisit() {
			return lastVisit;
		}

		@JsonView(View.Persistence.class)
		public void setLastVisit(Date lastVisit) {
			this.lastVisit = lastVisit;
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			final RoomHistoryEntry that = (RoomHistoryEntry) o;

			return Objects.equals(roomId, that.roomId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(roomId);
		}
	}

	private AuthProvider authProvider;
	private String loginId;
	private Date lastLoginTimestamp;
	private Account account;
	/* TODO: Review - is a Map more appropriate?
	 * pro List: can be ordered by date
	 * pro Map (roomId -> RoomHistoryEntry): easier to access for updating lastVisit
	 * -> Map but custom serialization to array? */
	private Set<RoomHistoryEntry> roomHistory = new HashSet<>();
	private Set<String> acknowledgedMotds = new HashSet<>();
	private Map<String, Map<String, ?>> extensions;

	public UserProfile() {

	}

	public UserProfile(final AuthProvider authProvider, final String loginId) {
		this.authProvider = authProvider;
		this.loginId = loginId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public AuthProvider getAuthProvider() {
		return authProvider;
	}

	@JsonView(View.Persistence.class)
	public void setAuthProvider(final AuthProvider authProvider) {
		this.authProvider = authProvider;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getLoginId() {
		return loginId;
	}

	@JsonView(View.Persistence.class)
	public void setLoginId(final String loginId) {
		this.loginId = loginId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Date getLastLoginTimestamp() {
		return lastLoginTimestamp;
	}

	@JsonView(View.Persistence.class)
	public void setLastLoginTimestamp(final Date lastLoginTimestamp) {
		this.lastLoginTimestamp = lastLoginTimestamp;
	}

	@JsonView(View.Persistence.class)
	public Account getAccount() {
		return account;
	}

	@JsonView(View.Persistence.class)
	public void setAccount(final Account account) {
		this.account = account;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Set<RoomHistoryEntry> getRoomHistory() {
		return roomHistory;
	}

	@JsonView(View.Persistence.class)
	public void setRoomHistory(final Set<RoomHistoryEntry> roomHistory) {
		this.roomHistory = roomHistory;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Set<String> getAcknowledgedMotds() {
		return acknowledgedMotds;
	}

	@JsonView(View.Persistence.class)
	public void setAcknowledgedMotds(final Set<String> acknowledgedMotds) {
		this.acknowledgedMotds = acknowledgedMotds;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Map<String, Map<String, ?>> getExtensions() {
		return extensions;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setExtensions(Map<String, Map<String, ?>> extensions) {
		this.extensions = extensions;
	}

	/**
	 * {@inheritDoc}
	 *
	 * The following fields of <tt>UserProfile</tt> are excluded from equality checks:
	 * {@link #account}, {@link #roomHistory}, {@link #acknowledgedMotds}, {@link #extensions}.
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!super.equals(o)) {
			return false;
		}
		final UserProfile that = (UserProfile) o;

		return authProvider == that.authProvider &&
				Objects.equals(loginId, that.loginId) &&
				Objects.equals(lastLoginTimestamp, that.lastLoginTimestamp);
	}
}
