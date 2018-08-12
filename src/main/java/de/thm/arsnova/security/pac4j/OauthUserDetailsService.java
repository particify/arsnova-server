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
package de.thm.arsnova.security.pac4j;

import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.security.User;
import de.thm.arsnova.service.UserService;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.pac4j.oauth.profile.google2.Google2Profile;
import org.pac4j.oauth.profile.twitter.TwitterProfile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;

/**
 * Loads UserDetails for an OAuth user (e.g. {@link UserProfile.AuthProvider#GOOGLE}) based on an unique identifier
 * extracted from the OAuth profile.
 *
 * @author Daniel Gerhardt
 */
@Service
public class OauthUserDetailsService implements AuthenticationUserDetailsService<OAuthToken> {
	private final UserService userService;
	protected final Collection<GrantedAuthority> grantedAuthorities;

	public OauthUserDetailsService(UserService userService) {
		this.userService = userService;
		grantedAuthorities = new HashSet<>();
		grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_OAUTH_USER"));
	}

	public User loadUserDetails(final OAuthToken token)
			throws UsernameNotFoundException {
		User user;
		if (token.getDetails() instanceof Google2Profile) {
			final Google2Profile profile = (Google2Profile) token.getDetails();
			user = userService.loadUser(UserProfile.AuthProvider.GOOGLE, profile.getEmail(),
					grantedAuthorities, true);
		} else if (token.getDetails() instanceof TwitterProfile) {
			final TwitterProfile profile = (TwitterProfile) token.getDetails();
			user = userService.loadUser(UserProfile.AuthProvider.TWITTER, profile.getUsername(),
					grantedAuthorities, true);
		} else if (token.getDetails() instanceof FacebookProfile) {
			final FacebookProfile profile = (FacebookProfile) token.getDetails();
			user = userService.loadUser(UserProfile.AuthProvider.FACEBOOK, profile.getId(),
					grantedAuthorities, true);
		} else {
			throw new IllegalArgumentException("AuthenticationToken not supported");
		}

		return user;
	}
}
