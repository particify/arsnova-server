/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
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

import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.entities.User;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.services.UserSessionService;
import de.thm.arsnova.socket.ARSnovaSocket;

@RestController
@RequestMapping("/socket")
public class SocketController extends AbstractController {

	@Autowired
	private IUserService userService;

	@Autowired
	private UserSessionService userSessionService;

	@Autowired
	private ARSnovaSocket server;

	private static final Logger LOGGER = LoggerFactory.getLogger(SocketController.class);

	@RequestMapping(method = RequestMethod.POST, value = "/assign")
	public final void authorize(@RequestBody final Map<String, String> sessionMap, final HttpServletResponse response) {
		String socketid = sessionMap.get("session");
		if (null == socketid) {
			LOGGER.debug("Expected property 'session' missing", socketid);
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;
		}
		User u = userService.getCurrentUser();
		if (null == u) {
			LOGGER.debug("Client {} requested to assign Websocket session but has not authenticated", socketid);
			response.setStatus(HttpStatus.FORBIDDEN.value());
			return;
		}
		userService.putUser2SocketId(UUID.fromString(socketid), u);
		userSessionService.setSocketId(UUID.fromString(socketid));
		response.setStatus(HttpStatus.NO_CONTENT.value());
	}

	@RequestMapping(value = "/url", method = RequestMethod.GET)
	public final String getSocketUrl(final HttpServletRequest request) {
		StringBuilder url = new StringBuilder();

		url.append(server.isUseSSL() ? "https://" : "http://");
		url.append(request.getServerName() + ":" + server.getPortNumber());

		return url.toString();
	}

}
