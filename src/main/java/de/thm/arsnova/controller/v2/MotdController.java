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

import de.thm.arsnova.controller.AbstractController;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.model.migration.FromV2Migrator;
import de.thm.arsnova.model.migration.ToV2Migrator;
import de.thm.arsnova.model.migration.v2.Motd;
import de.thm.arsnova.model.migration.v2.MotdList;
import de.thm.arsnova.security.User;
import de.thm.arsnova.service.MotdService;
import de.thm.arsnova.service.RoomService;
import de.thm.arsnova.service.UserService;
import de.thm.arsnova.web.exceptions.ForbiddenException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import java.util.stream.Collectors;

/**
 *
 */
@RestController("v2MotdController")
@RequestMapping("/v2/motd")
@Api(value = "/motd", description = "Message of the Day API")
public class MotdController extends AbstractController {
	@Autowired
	private MotdService motdService;

	@Autowired
	private RoomService roomService;

	@Autowired
	private UserService userService;

	@Autowired
	private ToV2Migrator toV2Migrator;

	@Autowired
	private FromV2Migrator fromV2Migrator;

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
		@ApiParam(value = "sessionkey", required = false) @RequestParam(value = "sessionkey", required = false) final String roomShortId
	) {
		List<de.thm.arsnova.model.Motd> motds;
		Date date = new Date(System.currentTimeMillis());
		if (!clientdate.isEmpty()) {
			date.setTime(Long.parseLong(clientdate));
		}
		String roomId = "";
		if (roomShortId != null) {
			roomId = roomService.getIdByShortId(roomShortId);
		}
		if (adminview) {
			motds = roomShortId != null ?
					motdService.getAllRoomMotds(roomId) :
					motdService.getAdminMotds();
		} else {
			motds = roomShortId != null ?
					motdService.getCurrentRoomMotds(date, roomId) :
					motdService.getCurrentMotds(date, audience);
		}

		return motds.stream().map(toV2Migrator::migrate).collect(Collectors.toList());
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
		de.thm.arsnova.model.Motd motdV3 = fromV2Migrator.migrate(motd);
		String roomId = roomService.getIdByShortId(motd.getSessionkey());
		if (de.thm.arsnova.model.Motd.Audience.ROOM == motdV3.getAudience() && roomId != null) {
			motdService.save(roomId, motdV3);
		} else {
			motdService.save(motdV3);
		}

		return toV2Migrator.migrate(motdV3);
	}

	@ApiOperation(value = "update a message of the day", nickname = "updateMotd")
	@RequestMapping(value = "/{motdId}", method = RequestMethod.PUT)
	public Motd updateMotd(
			@ApiParam(value = "motdkey from current motd", required = true) @PathVariable final String motdId,
			@ApiParam(value = "current motd", required = true) @RequestBody final Motd motd
			) {
		de.thm.arsnova.model.Motd motdV3 = fromV2Migrator.migrate(motd);
		String roomId = roomService.getIdByShortId(motd.getSessionkey());
		if (motdV3.getAudience() == de.thm.arsnova.model.Motd.Audience.ROOM && roomId != null) {
			motdService.update(roomId, motdV3);
		} else {
			motdService.update(motdV3);
		}

		return toV2Migrator.migrate(motdV3);
	}

	@ApiOperation(value = "deletes a message of the day", nickname = "deleteMotd")
	@RequestMapping(value = "/{motdId}", method = RequestMethod.DELETE)
	public void deleteMotd(@ApiParam(value = "Motd-key from the message that shall be deleted", required = true) @PathVariable final String motdId) {
		de.thm.arsnova.model.Motd motd = motdService.get(motdId);
		motdService.delete(motd);
	}

	@RequestMapping(value = "/userlist", method =  RequestMethod.GET)
	public MotdList getAcknowledgedIds(@AuthenticationPrincipal User user, @RequestParam final String username) {
		if (user == null || !user.getUsername().equals(username)) {
			throw new ForbiddenException();
		}
		UserProfile profile = userService.get(user.getId());

		return toV2Migrator.migrateMotdList(profile);
	}

	@RequestMapping(value = "/userlist", method =  RequestMethod.PUT)
	public void putAcknowledgedIds(@AuthenticationPrincipal User user, @RequestBody final MotdList motdList) {
		if (user == null || !user.getUsername().equals(motdList.getUsername())) {
			throw new ForbiddenException();
		}
		UserProfile profile = userService.get(user.getId());
		profile.setAcknowledgedMotds(fromV2Migrator.migrate(motdList));
		userService.update(profile);
	}
}
