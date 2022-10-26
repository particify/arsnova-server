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

package de.thm.arsnova.controller.v2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.thm.arsnova.controller.AbstractController;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.service.UserService;

/**
 * Handles requests related to ARSnova's own user registration and login process.
 */
@Controller("v2UserController")
@RequestMapping("/v2/user")
public class UserController extends AbstractController {
	@Autowired
	private DaoAuthenticationProvider daoProvider;

	@Autowired
	private UserService userService;

	@PostMapping(value = "/register")
	public void register(@RequestParam final String username,
			@RequestParam final String password,
			final HttpServletRequest request, final HttpServletResponse response) {
		if (null != userService.create(username, password)) {
			return;
		}

		/* TODO: Improve error handling: send reason to client */
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}

	@PostMapping(value = "/{username}/activate")
	public void activate(
			@PathVariable final String username,
			@RequestParam final String key,
			final HttpServletRequest request,
			final HttpServletResponse response) {
		final UserProfile userProfile = userService.getByUsername(username);
		if (userProfile == null || !userService.activateAccount(userProfile.getId(), key, request.getRemoteAddr())) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	@DeleteMapping(value = "/{username}/")
	public void activate(
			@PathVariable final String username,
			final HttpServletRequest request,
			final HttpServletResponse response) {
		if (null == userService.deleteByUsername(username)) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	@PostMapping(value = "/{username}/resetpassword")
	public void resetPassword(
			@PathVariable final String username,
			@RequestParam(required = false) final String key,
			@RequestParam(required = false) final String password,
			final HttpServletRequest request,
			final HttpServletResponse response) {
		final UserProfile userProfile = userService.getByUsername(username);
		if (null == userProfile) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);

			return;
		}

		if (null != key) {
			if (!userService.resetPassword(userProfile, key, password)) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
		} else {
			userService.initiatePasswordReset(userProfile);
		}
	}
}
