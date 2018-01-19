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
import de.thm.arsnova.services.UserRoomService;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.pac4j.oauth.profile.google2.Google2Profile;
import org.pac4j.oauth.profile.twitter.TwitterProfile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

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
	private UserRoomService.Role role;
	private boolean isAdmin;

	public UserAuthentication(final Google2Profile profile) {
		setUsername(profile.getEmail());
		setAuthProvider(UserProfile.AuthProvider.GOOGLE);
	}

	public UserAuthentication(final TwitterProfile profile) {
		setUsername(profile.getUsername());
		setAuthProvider(UserProfile.AuthProvider.TWITTER);
	}

	public UserAuthentication(final FacebookProfile profile) {
		setUsername(profile.getProfileUrl().toString());
		setAuthProvider(UserProfile.AuthProvider.FACEBOOK);
	}

	public UserAuthentication(final AttributePrincipal principal) {
		setUsername(principal.getName());
		setAuthProvider(UserProfile.AuthProvider.CAS);
	}

	public UserAuthentication(final UsernamePasswordAuthenticationToken token) {
		setUsername(token.getName());
		setAuthProvider(UserProfile.AuthProvider.LDAP);
	}

	public UserAuthentication(final AnonymousAuthenticationToken token) {
		setUsername(UserAuthentication.ANONYMOUS);
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

	public UserRoomService.Role getRole() {
		return role;
	}

	public void setRole(final UserRoomService.Role role) {
		this.role = role;
	}

	public boolean hasRole(UserRoomService.Role role) {
		return this.role == role;
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
