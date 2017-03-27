/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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

import de.thm.arsnova.entities.DbUser;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.services.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles requests related to ARSnova's own user registration and login process.
 */
@Controller
@RequestMapping("/user")
public class UserController extends AbstractController {
	@Autowired
	private DaoAuthenticationProvider daoProvider;

	@Autowired
	private IUserService userService;

	@Autowired
	private UserSessionService userSessionService;

	public static final Logger logger = LoggerFactory
			.getLogger(UserController.class);

	@RequestMapping(value = "/register", method = RequestMethod.POST)
	public void register(@RequestParam final String username,
			@RequestParam final String password,
			final HttpServletRequest request, final HttpServletResponse response) {
		if (null != userService.createDbUser(username, password)) {
			return;
		}

		/* TODO: Improve error handling: send reason to client */
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}

	@RequestMapping(value = "/{username}/activate", method = { RequestMethod.POST,
			RequestMethod.GET })
	public void activate(
			@PathVariable final String username,
			@RequestParam final String key, final HttpServletRequest request,
			final HttpServletResponse response) {
		DbUser dbUser = userService.getDbUser(username);
		if (null != dbUser && key.equals(dbUser.getActivationKey())) {
			dbUser.setActivationKey(null);
			userService.updateDbUser(dbUser);

			return;
		}

		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}

	@RequestMapping(value = "/{username}/", method = RequestMethod.DELETE)
	public void activate(
			@PathVariable final String username,
			final HttpServletRequest request,
			final HttpServletResponse response) {
		if (null == userService.deleteDbUser(username)) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	@RequestMapping(value = "/{username}/resetpassword", method = RequestMethod.POST)
	public void resetPassword(
			@PathVariable final String username,
			@RequestParam(required = false) final String key,
			@RequestParam(required = false) final String password,
			final HttpServletRequest request,
			final HttpServletResponse response) {
		DbUser dbUser = userService.getDbUser(username);
		if (null == dbUser) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);

			return;
		}

		if (null != key) {
			if (!userService.resetPassword(dbUser, key, password)) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
		} else {
			userService.initiatePasswordReset(username);
		}
	}
}
