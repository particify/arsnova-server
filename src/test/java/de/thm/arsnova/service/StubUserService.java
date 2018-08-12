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
package de.thm.arsnova.service;

import de.thm.arsnova.model.migration.v2.ClientAuthentication;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.persistence.UserRepository;
import de.thm.arsnova.security.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StubUserService extends UserServiceImpl {
	private final Set<GrantedAuthority> grantedAuthorities;
	private ClientAuthentication stubUser = null;

	public StubUserService(
			UserRepository repository,
			JavaMailSender mailSender,
			@Qualifier("defaultJsonMessageConverter") MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
		super(repository, mailSender, jackson2HttpMessageConverter);
		grantedAuthorities = new HashSet<>();
		grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
	}

	public void setUserAuthenticated(boolean isAuthenticated) {
		this.setUserAuthenticated(isAuthenticated, "ptsr00");
	}

	public void setUserAuthenticated(boolean isAuthenticated, String username) {
		if (isAuthenticated) {
			UserProfile userProfile = new UserProfile(UserProfile.AuthProvider.ARSNOVA, username);
			userProfile.setId(UUID.randomUUID().toString());
			User user = new User(userProfile, grantedAuthorities);
			stubUser = new ClientAuthentication(user);
		} else {
			stubUser = null;
		}
	}

	public void useAnonymousUser() {
		stubUser = new ClientAuthentication(new AnonymousAuthenticationToken(UUID.randomUUID().toString(), "anonymous", Collections.emptyList()));
	}

	@Override
	public ClientAuthentication getCurrentUser() {
		return stubUser;
	}
}
