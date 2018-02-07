package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserProfile implements Entity {
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

		public String getRoomId() {
			return roomId;
		}

		public void setRoomId(String roomId) {
			this.roomId = roomId;
		}

		public Date getLastVisit() {
			return lastVisit;
		}

		public void setLastVisit(Date lastVisit) {
			this.lastVisit = lastVisit;
		}
	}

	private String id;
	private String rev;
	private Date creationTimestamp;
	private Date updateTimestamp;
	private AuthProvider authProvider;
	private String loginId;
	private Date lastLoginTimestamp;
	private Account account;
	private List<RoomHistoryEntry> roomHistory = new ArrayList<>();
	private Set<String> acknowledgedMotds = new HashSet<>();
	private Map<String, Map<String, ?>> extensions;

	public UserProfile() {

	}

	public UserProfile(final AuthProvider authProvider, final String loginId) {
		this.authProvider = authProvider;
		this.loginId = loginId;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getId() {
		return id;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public void setId(final String id) {
		this.id = id;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getRevision() {
		return rev;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public void setRevision(final String rev) {
		this.rev = rev;
	}

	@Override
	@JsonView(View.Persistence.class)
	public Date getCreationTimestamp() {
		return creationTimestamp;
	}

	@Override
	@JsonView(View.Persistence.class)
	public void setCreationTimestamp(final Date creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}

	@Override
	@JsonView(View.Persistence.class)
	public Date getUpdateTimestamp() {
		return updateTimestamp;
	}

	@Override
	@JsonView(View.Persistence.class)
	public void setUpdateTimestamp(final Date updateTimestamp) {
		this.updateTimestamp = updateTimestamp;
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
	public List<RoomHistoryEntry> getRoomHistory() {
		return roomHistory;
	}

	@JsonView(View.Persistence.class)
	public void setRoomHistory(final List<RoomHistoryEntry> roomHistory) {
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
}
