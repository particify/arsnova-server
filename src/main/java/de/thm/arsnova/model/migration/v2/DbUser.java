/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.model.migration.v2;

import com.fasterxml.jackson.annotation.JsonView;

import de.thm.arsnova.model.serialization.View;

/**
 * A user account for ARSnova's own registration and login process.
 */
public class DbUser implements Entity {
	private String id;
	private String rev;
	private String username;
	private String password;
	private String activationKey;
	private String passwordResetKey;
	private long passwordResetTime;
	private long creation;
	private long lastLogin;

	@JsonView(View.Persistence.class)
	public String getId() {
		return id;
	}

	@JsonView(View.Persistence.class)
	public void setId(final String id) {
		this.id = id;
	}

	@JsonView(View.Persistence.class)
	public String getRevision() {
		return rev;
	}

	@JsonView(View.Persistence.class)
	public void setRevision(final String rev) {
		this.rev = rev;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getUsername() {
		return username;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setUsername(final String username) {
		this.username = username;
	}

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
	public long getPasswordResetTime() {
		return passwordResetTime;
	}

	@JsonView(View.Persistence.class)
	public void setPasswordResetTime(final long passwordResetTime) {
		this.passwordResetTime = passwordResetTime;
	}

	@JsonView(View.Persistence.class)
	public long getCreation() {
		return creation;
	}

	@JsonView(View.Persistence.class)
	public void setCreation(final long creation) {
		this.creation = creation;
	}

	@JsonView(View.Persistence.class)
	public long getLastLogin() {
		return lastLogin;
	}

	@JsonView(View.Persistence.class)
	public void setLastLogin(final long lastLogin) {
		this.lastLogin = lastLogin;
	}
}
