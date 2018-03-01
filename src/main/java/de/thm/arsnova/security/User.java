/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team
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
package de.thm.arsnova.security;

import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import de.thm.arsnova.entities.UserProfile;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * UserDetails implementation which identifies a user by internal (database) and external (AuthProvider + loginId) ID.
 *
 * @author Daniel Gerhardt
 */
public class User implements org.springframework.security.core.userdetails.UserDetails {
	private static final long serialVersionUID = 1L;

	private String id;
	private String loginId;
	private UserProfile.AuthProvider authProvider;
	private String password;
	private org.springframework.security.core.userdetails.UserDetails providerUserDetails;
	private Collection<? extends GrantedAuthority> authorities;
	private boolean enabled;
	private String token;

	public User(final UserProfile profile, final Collection<? extends GrantedAuthority> authorities) {
		if (profile == null || profile.getId() == null) {
			throw new IllegalArgumentException();
		}
		id = profile.getId();
		loginId = profile.getLoginId();
		authProvider = profile.getAuthProvider();
		password = profile.getAccount() == null ? null : profile.getAccount().getPassword();
		this.authorities = authorities;
		enabled = profile.getAccount() == null || profile.getAccount().getActivationKey() == null;
	}

	public User(final UserProfile profile, final Collection<? extends GrantedAuthority> authorities,
			final org.springframework.security.core.userdetails.UserDetails details) {
		this(profile, authorities);
		providerUserDetails = details;
	}

	public User(final ClientAuthentication clientAuthentication, final Collection<? extends GrantedAuthority> authorities) {
		id = clientAuthentication.getId();
		loginId = clientAuthentication.getUsername();
		authProvider = clientAuthentication.getAuthProvider();
		this.authorities = authorities;
		enabled = true;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return providerUserDetails != null ? providerUserDetails.getPassword() : password;
	}

	@Override
	public String getUsername() {
		return loginId;
	}

	@Override
	public boolean isAccountNonExpired() {
		return providerUserDetails == null || providerUserDetails.isAccountNonExpired();
	}

	@Override
	public boolean isAccountNonLocked() {
		return providerUserDetails == null || providerUserDetails.isAccountNonLocked();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return providerUserDetails == null || providerUserDetails.isCredentialsNonExpired();
	}

	@Override
	public boolean isEnabled() {
		return enabled && (providerUserDetails == null || providerUserDetails.isEnabled());
	}

	public UserProfile.AuthProvider getAuthProvider() {
		return authProvider;
	}

	public String getId() {
		return id;
	}

	public boolean hasRole(final String role) {
		return getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ROLE_" + role));
	}

	public boolean isAdmin() {
		return hasRole("ADMIN");
	}

	public String getToken() {
		return token;
	}

	public void setToken(final String token) {
		this.token = token;
	}

	@Override
	public String toString() {
		return String.format("Id: %s, LoginId: %s, AuthProvider: %s, Admin: %b",
				id, loginId, authProvider, isAdmin());
	}
}
