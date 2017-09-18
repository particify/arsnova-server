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

/**
 * Represents a user.
 */
public class UserAuthentication implements Serializable {
	public static final String GOOGLE = "google";
	public static final String TWITTER = "twitter";
	public static final String FACEBOOK = "facebook";
	public static final String THM = "thm";
	public static final String LDAP = "ldap";
	public static final String ARSNOVA = "arsnova";
	public static final String ANONYMOUS = "anonymous";
	public static final String GUEST = "guest";

	private static final long serialVersionUID = 1L;
	private String username;
	private String type;
	private UserRoomService.Role role;
	private boolean isAdmin;

	public UserAuthentication(Google2Profile profile) {
		setUsername(profile.getEmail());
		setType(UserAuthentication.GOOGLE);
	}

	public UserAuthentication(TwitterProfile profile) {
		setUsername(profile.getUsername());
		setType(UserAuthentication.TWITTER);
	}

	public UserAuthentication(FacebookProfile profile) {
		setUsername(profile.getProfileUrl().toString());
		setType(UserAuthentication.FACEBOOK);
	}

	public UserAuthentication(AttributePrincipal principal) {
		setUsername(principal.getName());
		setType(UserAuthentication.THM);
	}

	public UserAuthentication(AnonymousAuthenticationToken token) {
		setUsername(UserAuthentication.ANONYMOUS);
		setType(UserAuthentication.ANONYMOUS);
	}

	public UserAuthentication(UsernamePasswordAuthenticationToken token) {
		setUsername(token.getName());
		setType(LDAP);
	}

	@JsonView(View.Public.class)
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@JsonView(View.Public.class)
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public UserRoomService.Role getRole() {
		return role;
	}

	public void setRole(UserRoomService.Role role) {
		this.role = role;
	}

	public boolean hasRole(UserRoomService.Role role) {
		return this.role == role;
	}

	public void setAdmin(boolean a) {
		this.isAdmin = a;
	}

	@JsonView(View.Public.class)
	public boolean isAdmin() {
		return this.isAdmin;
	}

	@Override
	public String toString() {
		return "User [username=" + username + ", type=" + type + "]";
	}

	@Override
	public int hashCode() {
		// See http://stackoverflow.com/a/113600
		final int theAnswer = 42;
		final int theOthers = 37;

		int result = theAnswer;
		result = theOthers * result + this.username.hashCode();
		return theOthers * result + this.type.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !obj.getClass().equals(this.getClass())) {
			return false;
		}
		UserAuthentication other = (UserAuthentication) obj;
		return this.username.equals(other.username) && this.type.equals(other.type);
	}

}
