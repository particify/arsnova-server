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

package de.thm.arsnova.test.context.support;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.util.StringUtils;

import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.security.User;

/**
 * @author Daniel Gerhardt
 */
public class WithMockUserSecurityContextFactory implements WithSecurityContextFactory<WithMockUser> {
	@Override
	public SecurityContext createSecurityContext(final WithMockUser withMockUser) {
		final String loginId = StringUtils.hasLength(withMockUser.loginId()) ? withMockUser.loginId() : withMockUser.value();
		final UserProfile userProfile = new UserProfile(withMockUser.authProvider(), loginId);
		userProfile.setId(!withMockUser.userId().isEmpty() ? withMockUser.userId() : loginId);
		final User user = new User(userProfile, Arrays.stream(withMockUser.roles())
				.map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList()));
		final Authentication authentication =
				new UsernamePasswordAuthenticationToken(user, withMockUser.password(), user.getAuthorities());
		final SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);

		return context;
	}
}
