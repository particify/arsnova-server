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

package de.thm.arsnova.service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import de.thm.arsnova.config.properties.SystemProperties;
import de.thm.arsnova.config.properties.AuthenticationProviderProperties;
import de.thm.arsnova.config.properties.SecurityProperties;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.persistence.UserRepository;
import de.thm.arsnova.security.User;

public class StubUserService extends UserServiceImpl {
	private final Set<GrantedAuthority> grantedAuthorities;
	private User stubUser = null;

	public StubUserService(
			final UserRepository repository,
			final SystemProperties systemProperties,
			final SecurityProperties securityProperties,
			final AuthenticationProviderProperties authenticationProviderProperties,
			final JavaMailSender mailSender,
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
		super(repository, systemProperties, securityProperties, authenticationProviderProperties,
				mailSender, jackson2HttpMessageConverter);
		grantedAuthorities = new HashSet<>();
		grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
	}

	public void setUserAuthenticated(final boolean isAuthenticated) {
		this.setUserAuthenticated(isAuthenticated, "ptsr00");
	}

	public void setUserAuthenticated(final boolean isAuthenticated, final String username) {
		if (isAuthenticated) {
			final UserProfile userProfile = new UserProfile(UserProfile.AuthProvider.ARSNOVA, username);
			userProfile.setId(UUID.randomUUID().toString());
			stubUser = new User(userProfile, grantedAuthorities);
		} else {
			stubUser = null;
		}
	}

	@Override
	public User getCurrentUser() {
		return stubUser;
	}
}
