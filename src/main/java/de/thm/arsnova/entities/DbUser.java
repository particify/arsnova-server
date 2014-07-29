package de.thm.arsnova.entities;

public class DbUser {
	private String id;
	private String rev;
	private String username;
	private String password;
	private String activationKey;
	private String passwordResetKey;
	private long passwordResetTime;
	private long creation;
	private long lastLogin;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/* CouchDB deserialization */
	public void set_id(String id) {
		this.id = id;
	}

	public String getRev() {
		return rev;
	}

	public void setRev(String rev) {
		this.rev = rev;
	}

	/* CouchDB deserialization */
	public void set_rev(String rev) {
		this.rev = rev;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getActivationKey() {
		return activationKey;
	}

	public void setActivationKey(String activationKey) {
		this.activationKey = activationKey;
	}

	public String getPasswordResetKey() {
		return passwordResetKey;
	}

	public void setPasswordResetKey(String passwordResetKey) {
		this.passwordResetKey = passwordResetKey;
	}

	public long getPasswordResetTime() {
		return passwordResetTime;
	}

	public void setPasswordResetTime(long passwordResetTime) {
		this.passwordResetTime = passwordResetTime;
	}

	public long getCreation() {
		return creation;
	}

	public void setCreation(long creation) {
		this.creation = creation;
	}

	public long getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(long lastLogin) {
		this.lastLogin = lastLogin;
	}

	/* CouchDB deserialization */
	public void setType(String type) {
		/* no op */
	}
}
