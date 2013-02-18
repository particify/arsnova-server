/*
 * Copyright (C) 2012 THM webMedia
 *
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.controller;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.services.IUserService;

@Controller
@RequestMapping("/session")
public class SessionController extends AbstractController {

	public static final Logger LOGGER = LoggerFactory.getLogger(SessionController.class);

	@Autowired
	private ISessionService sessionService;

	@Autowired
	private IUserService userService;

	@RequestMapping(value = "/{sessionkey}", method = RequestMethod.GET)
	@ResponseBody
	public final Session joinSession(@PathVariable final String sessionkey) {
		return sessionService.joinSession(sessionkey);
	}

	@RequestMapping(value = "/{sessionkey}/online", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	public final LoggedIn registerAsOnlineUser(
			@PathVariable final String sessionkey,
			final HttpServletResponse response
	) {
		User user = userService.getCurrentUser();
		LoggedIn loggedIn = sessionService.registerAsOnlineUser(user, sessionkey);
		if (loggedIn != null) {
			return loggedIn;
		}

		throw new RuntimeException();
	}

	@RequestMapping(value = "/{sessionkey}/activeusercount", method = RequestMethod.GET)
	@ResponseBody
	public final int countActiveUsers(
			@PathVariable final String sessionkey,
			final HttpServletResponse response
	) {
		return sessionService.countActiveUsers(sessionkey);
	}

	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	public final Session postNewSession(@RequestBody final Session session, final HttpServletResponse response) {
		Session newSession = sessionService.saveSession(session);
		if (session != null) {
			return newSession;
		}

		response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
		return null;
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	@ResponseBody
	public final List<Session> getSessions(
			@RequestParam final String filter,
			final HttpServletResponse response
	) {
		User user = userService.getCurrentUser();
		List<Session> sessions = null;
		
		/* TODO: Could @Authorized annotation be used instead of this check? */
		if (null == user) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			
			return null;
		}
		
		if ("owned".equals(filter)) {
			sessions = sessionService.getMySessions(user);
		} else if ("visited".equals(filter)) {
			sessions = sessionService.getMyVisitedSessions(userService.getCurrentUser());
		}
		
		if (sessions == null || sessions.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
		
		return sessions;
	}
}
