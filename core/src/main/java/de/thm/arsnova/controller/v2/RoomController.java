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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.controller.PaginationController;
import de.thm.arsnova.model.migration.FromV2Migrator;
import de.thm.arsnova.model.migration.ToV2Migrator;
import de.thm.arsnova.model.migration.v2.Room;
import de.thm.arsnova.model.migration.v2.RoomFeature;
import de.thm.arsnova.model.migration.v2.RoomInfo;
import de.thm.arsnova.model.transport.ImportExportContainer;
import de.thm.arsnova.model.transport.ScoreStatistics;
import de.thm.arsnova.service.RoomService;
import de.thm.arsnova.service.RoomServiceImpl;
import de.thm.arsnova.service.RoomServiceImpl.RoomNameComparator;
import de.thm.arsnova.service.RoomServiceImpl.RoomShortNameComparator;
import de.thm.arsnova.service.UserService;
import de.thm.arsnova.web.DeprecatedApi;
import de.thm.arsnova.web.Pagination;
import de.thm.arsnova.web.exceptions.NotImplementedException;
import de.thm.arsnova.web.exceptions.UnauthorizedException;

/**
 * Handles requests related to ARSnova Rooms.
 */
@RestController("v2RoomController")
@RequestMapping("/v2/session")
@Api(value = "/session", description = "Room (Session) API")
public class RoomController extends PaginationController {
	@Autowired
	private RoomService roomService;

	@Autowired
	private UserService userService;

	@Autowired
	private ToV2Migrator toV2Migrator;

	@Autowired
	private FromV2Migrator fromV2Migrator;

	@ApiOperation(value = "join a Room",
			nickname = "joinRoom")
	@DeprecatedApi
	@Deprecated
	@GetMapping("/{shortId}")
	public Room joinRoom(
				@ApiParam(value = "Room-Key from current Room", required = true)
				@PathVariable final String shortId,
				@ApiParam(value = "Adminflag", required = false)
				@RequestParam(value = "admin", defaultValue = "false")
				final boolean admin) {
		if (admin) {
			return toV2Migrator.migrate(roomService.getForAdmin(shortId));
		} else {
			return toV2Migrator.migrate(roomService.getByShortId(shortId));
		}
	}

	@ApiOperation(value = "deletes a Room",
			nickname = "deleteRoom")
	@DeleteMapping("/{shortId}")
	public void deleteRoom(
			@ApiParam(value = "Room-Key from current Room", required = true)
			@PathVariable
			final String shortId) {
		final de.thm.arsnova.model.Room room = roomService.getByShortId(shortId);
		roomService.delete(room);
	}

	@ApiOperation(value = "count active users",
			nickname = "countActiveUsers")
	@DeprecatedApi
	@Deprecated
	@GetMapping(value = "/{shortId}/activeusercount", produces = MediaType.TEXT_PLAIN_VALUE)
	public String countActiveUsers(
			@ApiParam(value = "Room-Key from current Room", required = true) @PathVariable final String shortId) {
		return String.valueOf(roomService.activeUsers(roomService.getIdByShortId(shortId)));
	}

	@ApiOperation(value = "Creates a new Room and returns the Room's data",
			nickname = "postNewRoom")
	@ApiResponses(value = {
			@ApiResponse(code = 201, message = HTML_STATUS_201),
			@ApiResponse(code = 503, message = HTML_STATUS_503)
	})
	@PostMapping("/")
	@ResponseStatus(HttpStatus.CREATED)
	public Room postNewRoom(
			@ApiParam(value = "current Room", required = true)
			@RequestBody
			final Room room,
			final HttpServletResponse response) {
		/* FIXME: migrate LMS course support
		if (room != null && room.isCourseSession()) {
			final List<Course> courses = new ArrayList<>();
			final Course course = new Course();
			course.setId(room.getCourseId());
			courses.add(course);
			final int sessionCount = roomService.countSessionsByCourses(courses);
			if (sessionCount > 0) {
				final String appendix = " (" + (sessionCount + 1) + ")";
				room.setName(room.getName() + appendix);
				room.setAbbreviation(room.getAbbreviation() + appendix);
			}
		}
		*/

		return toV2Migrator.migrate(roomService.create(fromV2Migrator.migrate(room)));
	}

	@ApiOperation(value = "updates a Room",
			nickname = "postNewRoom")
	@PutMapping("/{shortId}")
	public Room updateRoom(
			@ApiParam(value = "Room-Key from current Room", required = true) @PathVariable final String shortId,
			@ApiParam(value = "current room", required = true) @RequestBody final Room room) {
		return toV2Migrator.migrate(roomService.update(fromV2Migrator.migrate(room)));
	}

	@ApiOperation(value = "change the Room creator (owner)", nickname = "changeRoomCreator")
	@RequestMapping(value = "/{shortId}/changecreator", method = RequestMethod.PUT)
	public Room changeRoomCreator(
				@ApiParam(value = "Room-key from current Room", required = true) @PathVariable final String shortId,
				@ApiParam(value = "new Room creator", required = true) @RequestBody final String newCreator) {
		return toV2Migrator.migrate(roomService.updateCreator(roomService.getIdByShortId(shortId), newCreator));
	}

	@ApiOperation(value = "Retrieves a list of Rooms",
			nickname = "getRooms")
	@ApiResponses(value = {
			@ApiResponse(code = 204, message = HTML_STATUS_204),
			@ApiResponse(code = 501, message = HTML_STATUS_501)
	})
	@GetMapping("/")
	@Pagination
	public List<Room> getRooms(
			@ApiParam(value = "ownedOnly", required = true)
			@RequestParam(value = "ownedonly", defaultValue = "false")
			final boolean ownedOnly,
			@ApiParam(value = "visitedOnly", required = true)
			@RequestParam(value = "visitedonly", defaultValue = "false")
			final boolean visitedOnly,
			@ApiParam(value = "sortby", required = true)
			@RequestParam(value = "sortby", defaultValue = "name")
			final String sortby,
			@ApiParam(value = "for a given username. admin rights needed", required = false)
			@RequestParam(value = "username", defaultValue = "")
			final String username,
			final HttpServletResponse response) {
		final List<de.thm.arsnova.model.Room> rooms;

		if (!"".equals(username)) {
			final String userId = userService.getByUsername(username).getId();
			try {
				if (ownedOnly && !visitedOnly) {
					rooms = roomService.getUserRooms(userId);
				} else if (visitedOnly && !ownedOnly) {
					rooms = roomService.getUserRoomHistory(username);
				} else {
					response.setStatus(HttpStatus.NOT_IMPLEMENTED.value());
					return null;
				}
			} catch (final AccessDeniedException e) {
				throw new UnauthorizedException();
			}
		} else {
			/* TODO implement all parameter combinations, implement use of user parameter */
			try {
				if (ownedOnly && !visitedOnly) {
					rooms = roomService.getMyRooms(offset, limit);
				} else if (visitedOnly && !ownedOnly) {
					rooms = roomService.getMyRoomHistory(offset, limit);
				} else {
					response.setStatus(HttpStatus.NOT_IMPLEMENTED.value());
					return null;
				}
			} catch (final AccessDeniedException e) {
				throw new UnauthorizedException();
			}
		}

		if (rooms == null || rooms.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return null;
		}

		if ("shortname".equals(sortby)) {
			Collections.sort(rooms, new RoomShortNameComparator());
		} else {
			Collections.sort(rooms, new RoomServiceImpl.RoomNameComparator());
		}

		return rooms.stream().map(toV2Migrator::migrate).collect(Collectors.toList());
	}

	/**
	 * Returns a list of my own Rooms with only the necessary information like name, keyword, or counters.
	 */
	@ApiOperation(value = "Retrieves a Room",
			nickname = "getMyRooms")
	@ApiResponses(value = {
			@ApiResponse(code = 204, message = HTML_STATUS_204)
	})
	@GetMapping(value = "/", params = "statusonly=true")
	@Pagination
	public List<RoomInfo> getMyRooms(
			@ApiParam(value = "visitedOnly", required = true)
			@RequestParam(value = "visitedonly", defaultValue = "false")
			final boolean visitedOnly,
			@ApiParam(value = "sort by", required = false)
			@RequestParam(value = "sortby", defaultValue = "name")
			final String sortby,
			final HttpServletResponse response) {
		final List<de.thm.arsnova.model.Room> rooms;
		if (!visitedOnly) {
			rooms = roomService.getMyRoomsInfo(offset, limit);
		} else {
			rooms = roomService.getMyRoomHistoryInfo(offset, limit);
		}

		if (rooms == null || rooms.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return null;
		}

		if ("shortname".equals(sortby)) {
			Collections.sort(rooms, new RoomShortNameComparator());
		} else {
			Collections.sort(rooms, new RoomNameComparator());
		}

		return rooms.stream().map(toV2Migrator::migrateStats).collect(Collectors.toList());
	}

	@ApiOperation(value = "Retrieves all public pool Rooms for the current user",
			nickname = "getMyPublicPoolRooms")
	@ApiResponses(value = {
			@ApiResponse(code = 204, message = HTML_STATUS_204)
	})
	@GetMapping(value = "/publicpool", params = "statusonly=true")
	public List<RoomInfo> getMyPublicPoolRooms(
			final HttpServletResponse response) {
		final List<de.thm.arsnova.model.Room> rooms = roomService.getMyPublicPoolRoomsInfo();

		if (rooms == null || rooms.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return null;
		}

		return rooms.stream().map(toV2Migrator::migrateStats).collect(Collectors.toList());
	}

	@ApiOperation(value = "Retrieves all public pool Rooms",
			nickname = "getMyPublicPoolRooms")
	@ApiResponses(value = {
			@ApiResponse(code = 204, message = HTML_STATUS_204)
	})
	@GetMapping("/publicpool")
	public List<Room> getPublicPoolRooms(
			final HttpServletResponse response) {
		final List<de.thm.arsnova.model.Room> rooms = roomService.getPublicPoolRoomsInfo();

		if (rooms == null || rooms.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return null;
		}

		return rooms.stream().map(toV2Migrator::migrate).collect(Collectors.toList());
	}

	@ApiOperation(value = "imports a Room",
			nickname = "importRoom")
	@PostMapping("/import")
	public Room importRoom(
			@ApiParam(value = "current Room", required = true) @RequestBody final ImportExportContainer room,
			final HttpServletResponse response) {
		return toV2Migrator.migrate(roomService.importRooms(room));
	}

	@ApiOperation(value = "export Rooms", nickname = "exportRoom")
	@GetMapping("/export")
	public List<ImportExportContainer> getExport(
			@ApiParam(value = "Room-Key", required = true)
			@RequestParam(value = "sessionkey", defaultValue = "")
			final List<String> shortIds,
			@ApiParam(value = "wether statistics shall be exported", required = true)
			@RequestParam(value = "withAnswerStatistics", defaultValue = "false")
			final Boolean withAnswerStatistics,
			@ApiParam(value = "wether comments shall be exported", required = true)
			@RequestParam(value = "withFeedbackQuestions", defaultValue = "false")
			final Boolean withFeedbackQuestions,
			final HttpServletResponse response) throws IOException {
		final List<ImportExportContainer> rooms = new ArrayList<>();
		ImportExportContainer temp;
		for (final String shortId : shortIds) {
			final String id = roomService.getIdByShortId(shortId);
			roomService.setActive(id, false);
			temp = roomService.exportRoom(id, withAnswerStatistics, withFeedbackQuestions);
			if (temp != null) {
				rooms.add(temp);
			}
			roomService.setActive(id, true);
		}
		return rooms;
	}

	@ApiOperation(value = "copy a Rooms to the public pool if enabled")
	@PostMapping("/{shortId}/copytopublicpool")
	public Room copyToPublicPool(
			@ApiParam(value = "Room-Key from current Room", required = true)
			@PathVariable
			final String shortId,
			@ApiParam(value = "public pool attributes for Room", required = true)
			@RequestBody
			final ImportExportContainer.PublicPool publicPool)
			throws IOException {
		final String id = roomService.getIdByShortId(shortId);
		roomService.setActive(id, false);
		final de.thm.arsnova.model.Room roomInfo = roomService.copyRoomToPublicPool(shortId, publicPool);
		roomService.setActive(id, true);

		return toV2Migrator.migrate(roomInfo);
	}

	@ApiOperation(value = "copy a Room from the public pool if enabled")
	@PostMapping("/{shortId}/copyfrompublicpool")
	public Room copyFromPublicPool(
			@ApiParam(value = "Short ID of the Room", required = true) @PathVariable final String shortId,
			@ApiParam(value = "custom attributes for Room", required = true) @RequestBody final Room sessionAttributes) {
		throw new NotImplementedException();
	}

	@ApiOperation(value = "Locks or unlocks a Room",
			nickname = "lockRoom")
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = HTML_STATUS_404)
	})
	@PostMapping("/{shortId}/lock")
	public Room lockRoom(
			@ApiParam(value = "Room-Key from current Room", required = true) @PathVariable final String shortId,
			@ApiParam(value = "lock", required = true) @RequestParam(required = false) final Boolean lock,
			final HttpServletResponse response) throws IOException {
		if (lock != null) {
			return toV2Migrator.migrate(roomService.setActive(roomService.getIdByShortId(shortId), lock));
		}
		response.setStatus(HttpStatus.NOT_FOUND.value());
		return null;
	}

	@ApiOperation(value = "retrieves a value for the score",
			nickname = "getLearningProgress")
	@GetMapping("/{shortId}/learningprogress")
	public ScoreStatistics getLearningProgress(
			@ApiParam(value = "Room-Key from current Room", required = true)
			@PathVariable
			final String shortId,
			@ApiParam(value = "type", required = false)
			@RequestParam(value = "type", defaultValue = "questions")
			final String type,
			@ApiParam(value = "question variant", required = false)
			@RequestParam(value = "questionVariant", required = false)
			final String questionVariant,
			final HttpServletResponse response) {
		throw new NotImplementedException();
	}

	@ApiOperation(value = "retrieves a value for the learning progress for the current user",
			nickname = "getMyLearningProgress")
	@GetMapping("/{shortId}/mylearningprogress")
	public ScoreStatistics getMyLearningProgress(
			@ApiParam(value = "Room-Key from current Room", required = true) @PathVariable final String shortId,
			@RequestParam(value = "type", defaultValue = "questions") final String type,
			@RequestParam(value = "questionVariant", required = false) final String questionVariant,
			final HttpServletResponse response) {
		throw new NotImplementedException();
	}

	@ApiOperation(value = "retrieves all Room features",
			nickname = "getRoomFeatures")
	@GetMapping("/{shortId}/features")
	public RoomFeature getRoomFeatures(
			@ApiParam(value = "Room-Key from current Room", required = true) @PathVariable final String shortId,
			final HttpServletResponse response) {
		final de.thm.arsnova.model.Room room = roomService.getByShortId(shortId);
		return toV2Migrator.migrate(room.getSettings());
	}

	@PutMapping("/{shortId}/features")
	@ApiOperation(value = "change all Room features",
			nickname = "changeRoomFeatures")
	public RoomFeature changeRoomFeatures(
			@ApiParam(value = "Room-Key from current Room", required = true) @PathVariable final String shortId,
			@ApiParam(value = "Room feature", required = true) @RequestBody final RoomFeature features,
			final HttpServletResponse response) {
		final de.thm.arsnova.model.Room room = roomService.getByShortId(shortId);
		room.setSettings(fromV2Migrator.migrate(features));
		roomService.update(room);

		return toV2Migrator.migrate(room.getSettings());
	}

	@PostMapping(value = "/{shortId}/lockfeedbackinput", produces = MediaType.TEXT_PLAIN_VALUE)
	@ApiOperation(value = "locks input of user live feedback",
			nickname = "lockFeedbackInput")
	public String lockFeedbackInput(
			@ApiParam(value = "Room-Key from current Room", required = true) @PathVariable final String shortId,
			@ApiParam(value = "lock", required = true) @RequestParam(required = true) final Boolean lock,
			final HttpServletResponse response) throws IOException {
		return String.valueOf(roomService.lockFeedbackInput(roomService.getIdByShortId(shortId), lock));
	}

	@PostMapping(value = "/{shortId}/flipflashcards", produces = MediaType.TEXT_PLAIN_VALUE)
	@ApiOperation(value = "flip all flashcards in Room",
			nickname = "lockFeedbackInput")
	public String flipFlashcards(
			@ApiParam(value = "Room-Key from current Room", required = true) @PathVariable final String shortId,
			@ApiParam(value = "flip", required = true) @RequestParam(required = true) final Boolean flip,
			final HttpServletResponse response) {
		return String.valueOf(roomService.flipFlashcards(roomService.getIdByShortId(shortId), flip));
	}

	/* internal redirections */

	@RequestMapping(value = "/{shortId}/lecturerquestion")
	public String redirectLecturerQuestion(
			@PathVariable final String shortId,
			final HttpServletResponse response) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/lecturerquestion/?sessionkey=%s", shortId);
	}

	@RequestMapping(value = "/{shortId}/lecturerquestion/{arg1}")
	public String redirectLecturerQuestionWithOneArgument(
			@PathVariable final String shortId,
			@PathVariable final String arg1,
			final HttpServletResponse response) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/lecturerquestion/%s/?sessionkey=%s", arg1, shortId);
	}

	@RequestMapping(value = "/{shortId}/lecturerquestion/{arg1}/{arg2}")
	public String redirectLecturerQuestionWithTwoArguments(
			@PathVariable final String shortId,
			@PathVariable final String arg1,
			@PathVariable final String arg2,
			final HttpServletResponse response) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/lecturerquestion/%s/%s/?sessionkey=%s", arg1, arg2, shortId);
	}

	@RequestMapping(value = "/{shortId}/lecturerquestion/{arg1}/{arg2}/{arg3}")
	public String redirectLecturerQuestionWithThreeArguments(
			@PathVariable final String shortId,
			@PathVariable final String arg1,
			@PathVariable final String arg2,
			@PathVariable final String arg3,
			final HttpServletResponse response) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/lecturerquestion/%s/%s/%s/?sessionkey=%s", arg1, arg2, arg3, shortId);
	}

	@RequestMapping(value = "/{shortId}/audiencequestion")
	public String redirectAudienceQuestion(
			@PathVariable final String shortId,
			final HttpServletResponse response) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/audiencequestion/?sessionkey=%s", shortId);
	}

	@RequestMapping(value = "/{shortId}/audiencequestion/{arg1}")
	public String redirectAudienceQuestionWithOneArgument(
			@PathVariable final String shortId,
			@PathVariable final String arg1,
			final HttpServletResponse response) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/audiencequestion/%s/?sessionkey=%s", arg1, shortId);
	}
}
