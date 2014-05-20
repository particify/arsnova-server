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

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.entities.User;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.services.UserSessionService;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@RestController
@RequestMapping("/socket")
public class SocketController extends AbstractController {

	@Autowired
	private IUserService userService;

	@Autowired
	private UserSessionService userSessionService;

	@Autowired
	private ARSnovaSocketIOServer server;

	@RequestMapping(method = RequestMethod.POST, value = "/assign")
	public final void authorize(@RequestBody final Object sessionObject, final HttpServletResponse response) {
		String socketid = (String) JSONObject.fromObject(sessionObject).get("session");
		if (socketid == null) {
			return;
		}
		User u = userService.getCurrentUser();
		response.setStatus(u != null ? HttpStatus.NO_CONTENT.value() : HttpStatus.UNAUTHORIZED.value());
		if (u != null) {
			userService.putUser2SocketId(UUID.fromString(socketid), u);
			userSessionService.setSocketId(UUID.fromString(socketid));
		}
	}

	@RequestMapping(value = "/url", method = RequestMethod.GET)
	public final String getSocketUrl(final HttpServletRequest request) {
		StringBuilder url = new StringBuilder();

		url.append(server.isUseSSL() ? "https://" : "http://");
		url.append(request.getServerName() + ":" + server.getPortNumber());

		return url.toString();
	}

}
