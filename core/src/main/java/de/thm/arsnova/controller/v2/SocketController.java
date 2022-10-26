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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.controller.AbstractController;
import de.thm.arsnova.security.User;
import de.thm.arsnova.service.UserService;
import de.thm.arsnova.websocket.ArsnovaSocketioServer;

/**
 * Initiates the socket communication.
 */
@RestController("v2SocketController")
@RequestMapping("/v2/socket")
@Api(value = "/socket", description = "WebSocket Initialization API")
public class SocketController extends AbstractController {

	@Autowired
	private UserService userService;

	@Autowired
	private ArsnovaSocketioServer server;

	private static final Logger logger = LoggerFactory.getLogger(SocketController.class);

	@ApiOperation(value = "requested to assign Websocket session",
			nickname = "authorize")
	@ApiResponses(value = {
			@ApiResponse(code = 204, message = HTML_STATUS_204),
			@ApiResponse(code = 400, message = HTML_STATUS_400),
			@ApiResponse(code = 403, message = HTML_STATUS_403)
	})
	@PostMapping("/assign")
	public void authorize(
			@ApiParam(value = "sessionMap", required = true) @RequestBody final Map<String, String> sessionMap,
			@ApiParam(value = "response", required = true) final HttpServletResponse response) {
		final String socketid = sessionMap.get("session");
		if (null == socketid) {
			logger.debug("Expected property 'session' missing.");
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;
		}
		final User user = userService.getCurrentUser();
		if (null == user) {
			logger.debug("Client {} requested to assign Websocket session but has not authenticated.", socketid);
			response.setStatus(HttpStatus.FORBIDDEN.value());
			return;
		}
		userService.putUserIdToSocketId(UUID.fromString(socketid), user.getId());
		response.setStatus(HttpStatus.NO_CONTENT.value());
	}

	@ApiOperation(value = "retrieves a socket url",
			nickname = "getSocketUrl")
	@GetMapping(value = "/url", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getSocketUrl(final HttpServletRequest request) {
		return (server.isUseSsl() ? "https://" : "http://") + request.getServerName() + ":" + server.getPortNumber();
	}

}
