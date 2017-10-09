/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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

import de.thm.arsnova.entities.Motd;
import de.thm.arsnova.services.MotdService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 *
 */
@RestController
@RequestMapping("/motd")
@Api(value = "/motd", description = "the Motd Controller API")
public class MotdController extends AbstractController {
	@Autowired
	private MotdService motdService;

	@ApiOperation(value = "get messages. if adminview=false, only messages with startdate<clientdate<enddate are returned")
	@RequestMapping(value = "/", method = RequestMethod.GET)
	@ApiResponses(value = {
		@ApiResponse(code = 204, message = HTML_STATUS_204),
		@ApiResponse(code = 501, message = HTML_STATUS_501)
	})
	public List<Motd> getMotd(
		@ApiParam(value = "clientdate", required = false) @RequestParam(value = "clientdate", defaultValue = "") final String clientdate,
		@ApiParam(value = "adminview", required = false) @RequestParam(value = "adminview", defaultValue = "false") final Boolean adminview,
		@ApiParam(value = "audience", required = false) @RequestParam(value = "audience", defaultValue = "all") final String audience,
		@ApiParam(value = "sessionkey", required = false) @RequestParam(value = "sessionkey", defaultValue = "null") final String sessionkey
	) {
		List<Motd> motds;
		Date date = new Date(System.currentTimeMillis());
		if (!clientdate.isEmpty()) {
			date.setTime(Long.parseLong(clientdate));
		}
		if (adminview) {
			motds = "session".equals(audience) ?
					motdService.getAllSessionMotds(sessionkey) :
					motdService.getAdminMotds();
		} else {
			motds = "session".equals(audience) ?
					motdService.getCurrentSessionMotds(date, sessionkey) :
					motdService.getCurrentMotds(date, audience);
		}
		return motds;
	}

	@ApiOperation(value = "create a new message of the day", nickname = "createMotd")
	@ApiResponses(value = {
		@ApiResponse(code = 201, message = HTML_STATUS_201),
		@ApiResponse(code = 503, message = HTML_STATUS_503)
	})
	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Motd postNewMotd(
			@ApiParam(value = "current motd", required = true) @RequestBody final Motd motd,
			final HttpServletResponse response
			) {
		if (motd != null) {
			Motd newMotd;
			if ("session".equals(motd.getAudience()) && motd.getSessionId() != null) {
				newMotd = motdService.save(motd.getSessionId(), motd);
			} else {
				newMotd = motdService.save(motd);
			}
			if (newMotd == null) {
				response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
				return null;
			}
			return newMotd;
		} else {
			response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
			return null;
		}
	}

	@ApiOperation(value = "update a message of the day", nickname = "updateMotd")
	@RequestMapping(value = "/{motdId}", method = RequestMethod.PUT)
	public Motd updateMotd(
			@ApiParam(value = "motdkey from current motd", required = true) @PathVariable final String motdId,
			@ApiParam(value = "current motd", required = true) @RequestBody final Motd motd
			) {
		if ("session".equals(motd.getAudience()) && motd.getSessionId() != null) {
			return motdService.update(motd.getSessionId(), motd);
		} else {
			return motdService.update(motd);
		}
	}

	@ApiOperation(value = "deletes a message of the day", nickname = "deleteMotd")
	@RequestMapping(value = "/{motdId}", method = RequestMethod.DELETE)
	public void deleteMotd(@ApiParam(value = "Motd-key from the message that shall be deleted", required = true) @PathVariable final String motdId) {
		Motd motd = motdService.get(motdId);
		if ("session".equals(motd.getAudience())) {
			motdService.deleteBySessionKey(motd.getSessionId(), motd);
		} else {
			motdService.delete(motd);
		}
	}
}
