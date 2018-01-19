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

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.transport.ImportExportSession;
import de.thm.arsnova.entities.transport.ScoreStatistics;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.RoomService;
import de.thm.arsnova.services.RoomServiceImpl.SessionNameComparator;
import de.thm.arsnova.services.RoomServiceImpl.SessionShortNameComparator;
import de.thm.arsnova.services.UserService;
import de.thm.arsnova.web.DeprecatedApi;
import de.thm.arsnova.web.Pagination;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles requests related to ARSnova rooms.
 */
@RestController
@RequestMapping("/session")
@Api(value = "/session", description = "the Room Controller API")
public class SessionController extends PaginationController {
	@Autowired
	private RoomService roomService;

	@Autowired
	private UserService userService;

	@ApiOperation(value = "join a session",
			nickname = "joinSession")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/{sessionkey}", method = RequestMethod.GET)
	public Room joinSession(
			@ApiParam(value = "Room-Key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "Adminflag", required = false) @RequestParam(value = "admin", defaultValue = "false")	final boolean admin
			) {
		if (admin) {
			return roomService.getForAdmin(sessionkey);
		} else {
			return roomService.getByKey(sessionkey);
		}
	}

	@ApiOperation(value = "deletes a session",
			nickname = "deleteSession")
	@RequestMapping(value = "/{sessionkey}", method = RequestMethod.DELETE)
	public void deleteSession(@ApiParam(value = "Room-Key from current session", required = true) @PathVariable final String sessionkey) {
		Room room = roomService.getByKey(sessionkey);
		roomService.deleteCascading(room);
	}

	@ApiOperation(value = "count active users",
			nickname = "countActiveUsers")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/{sessionkey}/activeusercount", method = RequestMethod.GET)
	public int countActiveUsers(@ApiParam(value = "Room-Key from current session", required = true) @PathVariable final String sessionkey) {
		return roomService.activeUsers(sessionkey);
	}

	@ApiOperation(value = "Creates a new Room and returns the Room's data",
			nickname = "postNewSession")
	@ApiResponses(value = {
		@ApiResponse(code = 201, message = HTML_STATUS_201),
		@ApiResponse(code = 503, message = HTML_STATUS_503)
	})
	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Room postNewSession(@ApiParam(value = "current session", required = true) @RequestBody final Room room, final HttpServletResponse response) {
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

		roomService.save(room);

		return room;
	}

	@ApiOperation(value = "updates a session",
			nickname = "postNewSession")
	@RequestMapping(value = "/{sessionkey}", method = RequestMethod.PUT)
	public Room updateSession(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "current session", required = true) @RequestBody final Room room
			) {
		return roomService.update(sessionkey, room);
	}

	@ApiOperation(value = "change the session creator (owner)", nickname = "changeSessionCreator")
	@RequestMapping(value = "/{sessionkey}/changecreator", method = RequestMethod.PUT)
	public Room changeSessionCreator(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "new session creator", required = true) @RequestBody final String newCreator
			) {
		return roomService.updateCreator(sessionkey, newCreator);
	}

	@ApiOperation(value = "Retrieves a list of Sessions",
			nickname = "getSessions")
	@ApiResponses(value = {
		@ApiResponse(code = 204, message = HTML_STATUS_204),
		@ApiResponse(code = 501, message = HTML_STATUS_501)
	})
	@RequestMapping(value = "/", method = RequestMethod.GET)
	@Pagination
	public List<Room> getSessions(
			@ApiParam(value = "ownedOnly", required = true) @RequestParam(value = "ownedonly", defaultValue = "false") final boolean ownedOnly,
			@ApiParam(value = "visitedOnly", required = true) @RequestParam(value = "visitedonly", defaultValue = "false") final boolean visitedOnly,
			@ApiParam(value = "sortby", required = true) @RequestParam(value = "sortby", defaultValue = "name") final String sortby,
			@ApiParam(value = "for a given username. admin rights needed", required = false) @RequestParam(value =
					"username", defaultValue = "") final String username,
			final HttpServletResponse response
			) {
		List<Room> rooms;

		if (!"".equals(username)) {
			final String userId = userService.getByUsername(username).getId();
			try {
				if (ownedOnly && !visitedOnly) {
					rooms = roomService.getUserSessions(userId);
				} else if (visitedOnly && !ownedOnly) {
					rooms = roomService.getUserVisitedSessions(username);
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
					rooms = roomService.getMySessions(offset, limit);
				} else if (visitedOnly && !ownedOnly) {
					rooms = roomService.getMyVisitedSessions(offset, limit);
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
			Collections.sort(rooms, new SessionShortNameComparator());
		} else {
			Collections.sort(rooms, new SessionNameComparator());
		}

		return rooms;
	}

	/**
	 * Returns a list of my own sessions with only the necessary information like name, keyword, or counters.
	 */
	@ApiOperation(value = "Retrieves a Room",
			nickname = "getMySessions")
	@ApiResponses(value = {
		@ApiResponse(code = 204, message = HTML_STATUS_204)
	})
	@RequestMapping(value = "/", method = RequestMethod.GET, params = "statusonly=true")
	@Pagination
	public List<Room> getMySessions(
			@ApiParam(value = "visitedOnly", required = true) @RequestParam(value = "visitedonly", defaultValue = "false") final boolean visitedOnly,
			@ApiParam(value = "sort by", required = false) @RequestParam(value = "sortby", defaultValue = "name") final String sortby,
			final HttpServletResponse response
			) {
		List<Room> rooms;
		if (!visitedOnly) {
			rooms = roomService.getMySessionsInfo(offset, limit);
		} else {
			rooms = roomService.getMyVisitedSessionsInfo(offset, limit);
		}

		if (rooms == null || rooms.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return null;
		}

		if ("shortname".equals(sortby)) {
			Collections.sort(rooms, new SessionShortNameComparator());
		} else {
			Collections.sort(rooms, new SessionNameComparator());
		}
		return rooms;
	}

	@ApiOperation(value = "Retrieves all public pool sessions for the current user",
			nickname = "getMyPublicPoolSessions")
	@ApiResponses(value = {
		@ApiResponse(code = 204, message = HTML_STATUS_204)
	})
	@RequestMapping(value = "/publicpool", method = RequestMethod.GET, params = "statusonly=true")
	public List<Room> getMyPublicPoolSessions(
			final HttpServletResponse response
			) {
		List<Room> sessions = roomService.getMyPublicPoolSessionsInfo();

		if (sessions == null || sessions.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return null;
		}

		return sessions;
	}

	@ApiOperation(value = "Retrieves all public pool sessions",
			nickname = "getMyPublicPoolSessions")
	@ApiResponses(value = {
		@ApiResponse(code = 204, message = HTML_STATUS_204)
	})
	@RequestMapping(value = "/publicpool", method = RequestMethod.GET)
	public List<Room> getPublicPoolSessions(
			final HttpServletResponse response
			) {
		List<Room> rooms = roomService.getPublicPoolSessionsInfo();

		if (rooms == null || rooms.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return null;
		}

		return rooms;
	}

	@ApiOperation(value = "imports a session",
			nickname = "importSession")
	@RequestMapping(value = "/import", method = RequestMethod.POST)
	public Room importSession(
			@ApiParam(value = "current session", required = true) @RequestBody final ImportExportSession session,
			final HttpServletResponse response
			) {
		return roomService.importSession(session);
	}

	@ApiOperation(value = "export sessions", nickname = "exportSession")
	@RequestMapping(value = "/export", method = RequestMethod.GET)
	public List<ImportExportSession> getExport(
			@ApiParam(value = "sessionkey", required = true) @RequestParam(value = "sessionkey", defaultValue = "") final List<String> sessionkey,
			@ApiParam(value = "wether statistics shall be exported", required = true) @RequestParam(value = "withAnswerStatistics", defaultValue = "false") final Boolean withAnswerStatistics,
			@ApiParam(value = "wether comments shall be exported", required = true) @RequestParam(value = "withFeedbackQuestions", defaultValue = "false") final Boolean withFeedbackQuestions,
			final HttpServletResponse response
		) {
		List<ImportExportSession> sessions = new ArrayList<>();
		ImportExportSession temp;
		for (String key : sessionkey) {
			roomService.setActive(key, false);
			temp = roomService.exportSession(key, withAnswerStatistics, withFeedbackQuestions);
			if (temp != null) {
				sessions.add(temp);
			}
			roomService.setActive(key, true);
		}
		return sessions;
	}

	@ApiOperation(value = "copy a session to the public pool if enabled")
	@RequestMapping(value = "/{sessionkey}/copytopublicpool", method = RequestMethod.POST)
	public Room copyToPublicPool(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "public pool attributes for session", required = true) @RequestBody final de.thm.arsnova.entities.transport.ImportExportSession.PublicPool publicPool
			) {
		roomService.setActive(sessionkey, false);
		Room roomInfo = roomService.copySessionToPublicPool(sessionkey, publicPool);
		roomService.setActive(sessionkey, true);
		return roomInfo;
	}


	@ApiOperation(value = "Locks or unlocks a Room",
			nickname = "lockSession")
	@ApiResponses(value = {
		@ApiResponse(code = 404, message = HTML_STATUS_404)
	})
	@RequestMapping(value = "/{sessionkey}/lock", method = RequestMethod.POST)
	public Room lockSession(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "lock", required = true) @RequestParam(required = false) final Boolean lock,
			final HttpServletResponse response
			) {
		if (lock != null) {
			return roomService.setActive(sessionkey, lock);
		}
		response.setStatus(HttpStatus.NOT_FOUND.value());
		return null;
	}

	@ApiOperation(value = "retrieves a value for the score",
			nickname = "getLearningProgress")
	@RequestMapping(value = "/{sessionkey}/learningprogress", method = RequestMethod.GET)
	public ScoreStatistics getLearningProgress(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "type", required = false) @RequestParam(value = "type", defaultValue = "questions") final String type,
			@ApiParam(value = "question variant", required = false) @RequestParam(value = "questionVariant", required = false) final String questionVariant,
			final HttpServletResponse response
			) {
		return roomService.getLearningProgress(sessionkey, type, questionVariant);
	}

	@ApiOperation(value = "retrieves a value for the learning progress for the current user",
			nickname = "getMyLearningProgress")
	@RequestMapping(value = "/{sessionkey}/mylearningprogress", method = RequestMethod.GET)
	public ScoreStatistics getMyLearningProgress(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@RequestParam(value = "type", defaultValue = "questions") final String type,
			@RequestParam(value = "questionVariant", required = false) final String questionVariant,
			final HttpServletResponse response
			) {
		return roomService.getMyLearningProgress(sessionkey, type, questionVariant);
	}

	@ApiOperation(value = "retrieves all session features",
			nickname = "getSessionFeatures")
	@RequestMapping(value = "/{sessionkey}/features", method = RequestMethod.GET)
	public Room.Settings getSessionFeatures(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			final HttpServletResponse response
			) {
		return roomService.getFeatures(sessionkey);
	}

	@RequestMapping(value = "/{sessionkey}/features", method = RequestMethod.PUT)
	@ApiOperation(value = "change all session features",
			nickname = "changeSessionFeatures")
	public Room.Settings changeSessionFeatures(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "session feature", required = true) @RequestBody final Room.Settings features,
			final HttpServletResponse response
			) {
		return roomService.updateFeatures(sessionkey, features);
	}

	@RequestMapping(value = "/{sessionkey}/lockfeedbackinput", method = RequestMethod.POST)
	@ApiOperation(value = "locks input of user live feedback",
			nickname = "lockFeedbackInput")
	public boolean lockFeedbackInput(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "lock", required = true) @RequestParam(required = true) final Boolean lock,
			final HttpServletResponse response
			) {
		return roomService.lockFeedbackInput(sessionkey, lock);
	}

	@RequestMapping(value = "/{sessionkey}/flipflashcards", method = RequestMethod.POST)
	@ApiOperation(value = "flip all flashcards in session",
			nickname = "lockFeedbackInput")
	public boolean flipFlashcards(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "flip", required = true) @RequestParam(required = true) final Boolean flip,
			final HttpServletResponse response
			) {
		return roomService.flipFlashcards(sessionkey, flip);
	}

	/* internal redirections */

	@RequestMapping(value = "/{sessionKey}/lecturerquestion")
	public String redirectLecturerQuestion(
			@PathVariable final String sessionKey,
			final HttpServletResponse response
			) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/lecturerquestion/?sessionkey=%s", sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/lecturerquestion/{arg1}")
	public String redirectLecturerQuestionWithOneArgument(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			final HttpServletResponse response
			) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/lecturerquestion/%s/?sessionkey=%s", arg1, sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/lecturerquestion/{arg1}/{arg2}")
	public String redirectLecturerQuestionWithTwoArguments(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			@PathVariable final String arg2,
			final HttpServletResponse response
			) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/lecturerquestion/%s/%s/?sessionkey=%s", arg1, arg2, sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/lecturerquestion/{arg1}/{arg2}/{arg3}")
	public String redirectLecturerQuestionWithThreeArguments(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			@PathVariable final String arg2,
			@PathVariable final String arg3,
			final HttpServletResponse response
			) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/lecturerquestion/%s/%s/%s/?sessionkey=%s", arg1, arg2, arg3, sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/audiencequestion")
	public String redirectAudienceQuestion(
			@PathVariable final String sessionKey,
			final HttpServletResponse response
			) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/audiencequestion/?sessionkey=%s", sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/audiencequestion/{arg1}")
	public String redirectAudienceQuestionWithOneArgument(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			final HttpServletResponse response
			) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/audiencequestion/%s/?sessionkey=%s", arg1, sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/audiencequestion/{arg1}/{arg2}")
	public String redirectAudienceQuestionWithTwoArguments(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			@PathVariable final String arg2,
			final HttpServletResponse response
			) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/audiencequestion/%s/%s/?sessionkey=%s", arg1, arg2, sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/audiencequestion/{arg1}/{arg2}/{arg3}")
	public String redirectAudienceQuestionWithThreeArguments(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			@PathVariable final String arg2,
			@PathVariable final String arg3,
			final HttpServletResponse response
			) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/audiencequestion/%s/%s/%s/?sessionkey=%s", arg1, arg2, arg3, sessionKey);
	}
}
