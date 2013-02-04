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
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Controller
public class SessionController extends AbstractController {

	public static final Logger LOGGER = LoggerFactory.getLogger(SessionController.class);

	@Autowired
	private ISessionService sessionService;

	@Autowired
	private IUserService userService;

	@Autowired
	private ARSnovaSocketIOServer server;

	@RequestMapping(method = RequestMethod.POST, value = "/authorize")
	public final void authorize(@RequestBody final Object sessionObject, final HttpServletResponse response) {
		String socketid = (String) JSONObject.fromObject(sessionObject).get("session");
		if (socketid == null) {
			return;
		}
		User u = userService.getCurrentUser();
		LOGGER.info("authorize session: " + socketid + ", user is:  " + u);
		response.setStatus(u != null ? HttpStatus.CREATED.value() : HttpStatus.UNAUTHORIZED.value());
		if(u != null) {
			userService.putUser2SocketId(UUID.fromString(socketid), u);	
		}		
	}

	@RequestMapping(value = "/session/{sessionkey}", method = RequestMethod.GET)
	@ResponseBody
	public final Session joinSession(@PathVariable final String sessionkey) {
		return sessionService.joinSession(sessionkey);
	}

	@RequestMapping(value = "/session/{sessionkey}/online", method = RequestMethod.POST)
	@ResponseBody
	public final LoggedIn registerAsOnlineUser(
			@PathVariable final String sessionkey,
			final HttpServletResponse response
	) {
		User user = userService.getCurrentUser();
		LoggedIn loggedIn = sessionService.registerAsOnlineUser(user, sessionkey);
		if (loggedIn != null) {
			response.setStatus(HttpStatus.CREATED.value());
			return loggedIn;
		}

		response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		return null;
	}

	@RequestMapping(value = "/session/{sessionkey}/activeusercount", method = RequestMethod.GET)
	@ResponseBody
	public final int countActiveUsers(
			@PathVariable final String sessionkey,
			final HttpServletResponse response
	) {
		return sessionService.countActiveUsers(sessionkey);
	}

	@RequestMapping(value = "/session", method = RequestMethod.POST)
	@ResponseBody
	public final Session postNewSession(@RequestBody final Session session, final HttpServletResponse response) {
		Session newSession = sessionService.saveSession(session);
		if (session != null) {
			response.setStatus(HttpStatus.CREATED.value());
			return newSession;
		}

		response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
		return null;
	}

	@RequestMapping(value = "/socketurl", method = RequestMethod.GET)
	@ResponseBody
	public final String getSocketUrl() {
		StringBuilder url = new StringBuilder();

		url.append(server.isUseSSL() ? "https://" : "http://");
		url.append(server.getHostIp() + ":" + server.getPortNumber());

		return url.toString();
	}

	@RequestMapping(value = { "/mySessions", "/session/mysessions" }, method = RequestMethod.GET)
	@ResponseBody
	public final List<Session> getMySession(final HttpServletResponse response) {
		String username = userService.getCurrentUser().getUsername();
		if (username == null) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		List<Session> sessions = sessionService.getMySessions(username);
		if (sessions == null || sessions.isEmpty()) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return sessions;
	}

	@RequestMapping(value = "/session/visitedsessions", method = RequestMethod.GET)
	@ResponseBody
	public final List<Session> getMyVisitedSession(final HttpServletResponse response) {
		List<Session> sessions = sessionService.getMyVisitedSessions(userService.getCurrentUser());
		if (sessions == null || sessions.isEmpty()) {
			response.setStatus(HttpStatus.NO_CONTENT.value());
			return null;
		}
		return sessions;
	}
}
