package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserProfile implements Entity {
	public class Account {
		private String password;
		private String activationKey;
		private String passwordResetKey;
		private long passwordResetTime;

		@JsonView(View.Persistence.class)
		public String getPassword() {
			return password;
		}

		@JsonView(View.Persistence.class)
		public void setPassword(String password) {
			this.password = password;
		}

		@JsonView(View.Persistence.class)
		public String getActivationKey() {
			return activationKey;
		}

		@JsonView(View.Persistence.class)
		public void setActivationKey(String activationKey) {
			this.activationKey = activationKey;
		}

		@JsonView(View.Persistence.class)
		public String getPasswordResetKey() {
			return passwordResetKey;
		}

		@JsonView(View.Persistence.class)
		public void setPasswordResetKey(String passwordResetKey) {
			this.passwordResetKey = passwordResetKey;
		}

		@JsonView(View.Persistence.class)
		public long getPasswordResetTime() {
			return passwordResetTime;
		}

		@JsonView(View.Persistence.class)
		public void setPasswordResetTime(long passwordResetTime) {
			this.passwordResetTime = passwordResetTime;
		}
	}

	public class SessionHistoryEntry {
		private String sessionId;
		private long lastVisit;

		public SessionHistoryEntry() {

		}

		public SessionHistoryEntry(String sessionId, long lastVisit) {
			this.sessionId = sessionId;
			this.lastVisit = lastVisit;
		}

		public String getSessionId() {
			return sessionId;
		}

		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}

		public long getLastVisit() {
			return lastVisit;
		}

		public void setLastVisit(long lastVisit) {
			this.lastVisit = lastVisit;
		}
	}

	private String id;
	private String rev;
	private String authProvider;
	private String loginId;
	private long creation;
	private long lastLogin;
	private Account account;
	private List<SessionHistoryEntry> sessionHistory = new ArrayList<>();
	private Set<String> acknowledgedMotds = new HashSet<>();

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getId() {
		return id;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public void setId(String id) {
		this.id = id;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getRevision() {
		return rev;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public void setRevision(String rev) {
		this.rev = rev;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getAuthProvider() {
		return authProvider;
	}

	@JsonView(View.Persistence.class)
	public void setAuthProvider(String authProvider) {
		this.authProvider = authProvider;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getLoginId() {
		return loginId;
	}

	@JsonView(View.Persistence.class)
	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public long getCreation() {
		return creation;
	}

	@JsonView(View.Persistence.class)
	public void setCreation(long creation) {
		this.creation = creation;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public long getLastLogin() {
		return lastLogin;
	}

	@JsonView(View.Persistence.class)
	public void setLastLogin(long lastLogin) {
		this.lastLogin = lastLogin;
	}

	@JsonView(View.Persistence.class)
	public Account getAccount() {
		return account;
	}

	@JsonView(View.Persistence.class)
	public void setAccount(Account account) {
		this.account = account;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public List<SessionHistoryEntry> getSessionHistory() {
		return sessionHistory;
	}

	@JsonView(View.Persistence.class)
	public void setSessionHistory(List<SessionHistoryEntry> sessionHistory) {
		this.sessionHistory = sessionHistory;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Set<String> getAcknowledgedMotds() {
		return acknowledgedMotds;
	}

	@JsonView(View.Persistence.class)
	public void setAcknowledgedMotds(Set<String> acknowledgedMotds) {
		this.acknowledgedMotds = acknowledgedMotds;
	}
}
