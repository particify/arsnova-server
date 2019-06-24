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

package de.thm.arsnova.controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.model.ClientAuthentication;
import de.thm.arsnova.model.LoginCredentials;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.service.UserService;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
	private UserService userService;

	public AuthenticationController(final UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/login")
	public ClientAuthentication login() {
		return userService.getCurrentClientAuthentication();
	}

	@PostMapping("/login/registered")
	public ClientAuthentication loginRegistered(@RequestBody LoginCredentials loginCredentials) {
		final String loginId = loginCredentials.getLoginId().toLowerCase();
		userService.authenticate(new UsernamePasswordAuthenticationToken(loginId, loginCredentials.getPassword()),
				UserProfile.AuthProvider.ARSNOVA);
		return userService.getCurrentClientAuthentication();
	}

	@PostMapping("/login/guest")
	public ClientAuthentication loginGuest() {
		final ClientAuthentication currentAuthentication = userService.getCurrentClientAuthentication();
		if (currentAuthentication != null
				&& currentAuthentication.getAuthProvider() == UserProfile.AuthProvider.ARSNOVA_GUEST) {
			return currentAuthentication;
		}
		userService.authenticate(new UsernamePasswordAuthenticationToken(null, null),
				UserProfile.AuthProvider.ARSNOVA_GUEST);

		return userService.getCurrentClientAuthentication();
	}
}
