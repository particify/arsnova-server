/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;
import de.thm.arsnova.security.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a user.
 */
public class UserAuthentication implements Serializable {
	public static final String ANONYMOUS = "anonymous";

	private static final long serialVersionUID = 1L;
	private String id;
	private String username;
	private UserProfile.AuthProvider authProvider;
	private boolean isAdmin;

	public UserAuthentication() {
		username = ANONYMOUS;
		authProvider = UserProfile.AuthProvider.NONE;
	}

	public UserAuthentication(User user) {
		id = user.getId();
		username = user.getUsername();
		authProvider = user.getAuthProvider();
		isAdmin = user.isAdmin();
	}

	public UserAuthentication(Authentication authentication) {
		if (authentication instanceof AnonymousAuthenticationToken) {
			setUsername(UserAuthentication.ANONYMOUS);
		} else {
			if (!(authentication.getPrincipal() instanceof User)) {
				throw new IllegalArgumentException("Unsupported authentication token");
			}
			User user = (User) authentication.getPrincipal();
			id = user.getId();
			username = user.getUsername();
			authProvider = user.getAuthProvider();
			isAdmin = user.isAdmin();
		}
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	@JsonView(View.Public.class)
	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	@JsonView(View.Public.class)
	public UserProfile.AuthProvider getAuthProvider() {
		return authProvider;
	}

	public void setAuthProvider(final UserProfile.AuthProvider authProvider) {
		this.authProvider = authProvider;
	}

	public void setAdmin(final boolean a) {
		this.isAdmin = a;
	}

	@JsonView(View.Public.class)
	public boolean isAdmin() {
		return this.isAdmin;
	}

	@Override
	public String toString() {
		return "User [username=" + username + ", authProvider=" + authProvider + "]";
	}

	@Override
	public int hashCode() {
		// See http://stackoverflow.com/a/113600
		final int theAnswer = 42;
		final int theOthers = 37;

		int result = theAnswer;
		result = theOthers * result + this.username.hashCode();
		return theOthers * result + this.authProvider.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !obj.getClass().equals(this.getClass())) {
			return false;
		}
		UserAuthentication other = (UserAuthentication) obj;

		return this.authProvider == other.authProvider && Objects.equals(this.id, other.id) && this.username.equals(other.username);
	}

}
