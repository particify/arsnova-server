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

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import de.thm.arsnova.entities.User;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.services.UserSessionService;
import de.thm.arsnova.socket.ARSnovaSocket;

/**
 * Initiates the socket communication.
 */
@RestController
@RequestMapping("/socket")
@Api(value = "/socket", description = "the Socket API")
public class SocketController extends AbstractController {

	@Autowired
	private IUserService userService;

	@Autowired
	private UserSessionService userSessionService;

	@Autowired
	private ARSnovaSocket server;

	private static final Logger LOGGER = LoggerFactory.getLogger(SocketController.class);

	@ApiOperation(value = "requested to assign Websocket session",
			nickname = "authorize")
	@ApiResponses(value = {
		@ApiResponse(code = 204, message = "No Content - successfully processed the request"),
		@ApiResponse(code = 400, message = "Bad Request - The Api cannot or will not process the request due to something that is perceived to be a client error"),
		@ApiResponse(code = 403, message = "Forbidden - The request was a valid request, but the Api is refusing to respond to it")
	})
	@RequestMapping(method = RequestMethod.POST, value = "/assign")
	public void authorize(@ApiParam(value="sessionMap", required=true) @RequestBody final Map<String, String> sessionMap, @ApiParam(value="response", required=true) final HttpServletResponse response) {
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

	@ApiOperation(value = "retrieves a socket url",
			nickname = "getSocketUrl")
	@RequestMapping(value = "/url", method = RequestMethod.GET)
	public String getSocketUrl(final HttpServletRequest request) {
		StringBuilder url = new StringBuilder();

		url.append(server.isUseSSL() ? "https://" : "http://");
		url.append(request.getServerName() + ":" + server.getPortNumber());

		return url.toString();
	}

}
