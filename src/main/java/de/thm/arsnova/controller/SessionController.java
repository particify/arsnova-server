/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2016 The ARSnova Team
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

import org.springframework.context.annotation.Import;
import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.SessionFeature;
import de.thm.arsnova.entities.SessionInfo;
import de.thm.arsnova.entities.transport.ImportExportSession;
import de.thm.arsnova.entities.transport.LearningProgressValues;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.services.SessionService.SessionInfoNameComparator;
import de.thm.arsnova.services.SessionService.SessionInfoShortNameComparator;
import de.thm.arsnova.services.SessionService.SessionNameComparator;
import de.thm.arsnova.services.SessionService.SessionShortNameComparator;
import de.thm.arsnova.web.DeprecatedApi;
import de.thm.arsnova.web.Pagination;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Handles requests related to ARSnova sessions.
 */
@RestController
@RequestMapping("/session")
@Api(value = "/session", description = "the Session Controller API")
public class SessionController extends PaginationController {

	public static final Logger LOGGER = LoggerFactory.getLogger(SessionController.class);

	@Autowired
	private ISessionService sessionService;

	@Autowired
	private IUserService userService;

	@ApiOperation(value = "join a session",
			nickname = "joinSession")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/{sessionkey}", method = RequestMethod.GET)
	public Session joinSession(@ApiParam(value = "Session-Key from current session", required = true) @PathVariable final String sessionkey) {
		return Session.anonymizedCopy(sessionService.getSession(sessionkey));
	}

	@ApiOperation(value = "deletes a session",
			nickname = "deleteSession")
	@RequestMapping(value = "/{sessionkey}", method = RequestMethod.DELETE)
	public void deleteSession(@ApiParam(value = "Session-Key from current session", required = true) @PathVariable final String sessionkey) {
		sessionService.deleteSession(sessionkey);
	}

	@ApiOperation(value = "count active users",
			nickname = "countActiveUsers")
	@DeprecatedApi
	@Deprecated
	@RequestMapping(value = "/{sessionkey}/activeusercount", method = RequestMethod.GET)
	public int countActiveUsers(@ApiParam(value = "Session-Key from current session", required = true) @PathVariable final String sessionkey) {
		return sessionService.activeUsers(sessionkey);
	}

	@ApiOperation(value = "Creates a new Session and returns the Session's data",
			nickname = "postNewSession")
	@ApiResponses(value = {
		@ApiResponse(code = 201, message = HTML_STATUS_201),
		@ApiResponse(code = 503, message = HTML_STATUS_503)
	})
	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Session postNewSession(@ApiParam(value = "current session", required = true) @RequestBody final Session session, @ApiParam(value = "http servlet response", required = true) final HttpServletResponse response) {
		if (session != null && session.isCourseSession()) {
			final List<Course> courses = new ArrayList<Course>();
			final Course course = new Course();
			course.setId(session.getCourseId());
			courses.add(course);
			final int sessionCount = sessionService.countSessions(courses);
			if (sessionCount > 0) {
				final String appendix = " (" + (sessionCount + 1) + ")";
				session.setName(session.getName() + appendix);
				session.setShortName(session.getShortName() + appendix);
			}
		}

		final Session newSession = sessionService.saveSession(session);

		if (newSession == null) {
			response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
			return null;
		}

		return newSession;
	}

	@ApiOperation(value = "updates a session",
			nickname = "postNewSession")
	@RequestMapping(value = "/{sessionkey}", method = RequestMethod.PUT)
	public Session updateSession(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "current session", required = true) @RequestBody final Session session
			) {
		return sessionService.updateSession(sessionkey, session);
	}

	@ApiOperation(value = "Retrieves a list of Sessions",
			nickname = "getSessions")
	@ApiResponses(value = {
		@ApiResponse(code = 204, message = HTML_STATUS_204),
		@ApiResponse(code = 501, message = HTML_STATUS_501)
	})
	@RequestMapping(value = "/", method = RequestMethod.GET)
	@Pagination
	public List<Session> getSessions(
			@ApiParam(value = "ownedOnly", required = true) @RequestParam(value = "ownedonly", defaultValue = "false") final boolean ownedOnly,
			@ApiParam(value = "visitedOnly", required = true) @RequestParam(value = "visitedonly", defaultValue = "false") final boolean visitedOnly,
			@ApiParam(value = "sortby", required = true) @RequestParam(value = "sortby", defaultValue = "name") final String sortby,
			final HttpServletResponse response
			) {
		List<Session> sessions = null;

		/* TODO implement all parameter combinations, implement use of user parameter */
		try {
			if (ownedOnly && !visitedOnly) {
				sessions = sessionService.getMySessions(offset, limit);
			} else if (visitedOnly && !ownedOnly) {
				sessions = sessionService.getMyVisitedSessions(offset, limit);
			} else {
				response.setStatus(HttpStatus.NOT_IMPLEMENTED.value());
				return null;
			}
		} catch (final AccessDeniedException e) {
			throw new UnauthorizedException();
		}

		if (sessions == null || sessions.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return null;
		}

		if (sortby != null && sortby.equals("shortname")) {
			Collections.sort(sessions, new SessionShortNameComparator());
		} else {
			Collections.sort(sessions, new SessionNameComparator());
		}

		return sessions;
	}

	/**
	 * Returns a list of my own sessions with only the necessary information like name, keyword, or counters.
	 * @param statusOnly The flag that has to be set in order to get this shortened list.
	 * @param response
	 * @return
	 */
	@ApiOperation(value = "Retrieves a Session",
			nickname = "getMySessions")
	@ApiResponses(value = {
		@ApiResponse(code = 204, message = HTML_STATUS_204)
	})
	@RequestMapping(value = "/", method = RequestMethod.GET, params = "statusonly=true")
	@Pagination
	public List<SessionInfo> getMySessions(
			@ApiParam(value = "visitedOnly", required = true) @RequestParam(value = "visitedonly", defaultValue = "false") final boolean visitedOnly,
			@ApiParam(value = "sort by", required = false) @RequestParam(value = "sortby", defaultValue = "name") final String sortby,
			@ApiParam(value = "http servlet response", required = true) final HttpServletResponse response
			) {
		List<SessionInfo> sessions;
		if (!visitedOnly) {
			sessions = sessionService.getMySessionsInfo(offset, limit);
		} else {
			sessions = sessionService.getMyVisitedSessionsInfo(offset, limit);
		}

		if (sessions == null || sessions.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return null;
		}

		if (sortby != null && sortby.equals("shortname")) {
			Collections.sort(sessions, new SessionInfoShortNameComparator());
		} else {
			Collections.sort(sessions, new SessionInfoNameComparator());
		}
		return sessions;
	}

	@ApiOperation(value = "Retrieves all public pool sessions for the current user",
			nickname = "getMyPublicPoolSessions")
	@ApiResponses(value = {
		@ApiResponse(code = 204, message = HTML_STATUS_204)
	})
	@RequestMapping(value = "/publicpool", method = RequestMethod.GET, params = "statusonly=true")
	public List<SessionInfo> getMyPublicPoolSessions(
			@ApiParam(value = "http servlet response", required = true) final HttpServletResponse response
			) {
		List<SessionInfo> sessions = sessionService.getMyPublicPoolSessionsInfo();

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
	public List<SessionInfo> getPublicPoolSessions(
			@ApiParam(value = "http servlet response", required = true) final HttpServletResponse response
			) {
		List<SessionInfo> sessions = sessionService.getPublicPoolSessionsInfo();

		if (sessions == null || sessions.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return null;
		}

		return sessions;
	}

	@ApiOperation(value = "imports a session",
			nickname = "importSession")
	@RequestMapping(value = "/import", method = RequestMethod.POST)
	public SessionInfo importSession(
			@ApiParam(value = "current session", required = true) @RequestBody final ImportExportSession session,
			@ApiParam(value = "http servlet response", required = true) final HttpServletResponse response
			) {
		return sessionService.importSession(session);
	}

	@ApiOperation(value = "export sessions", nickname = "exportSession")
	@RequestMapping(value = "/export", method = RequestMethod.GET)
	public List<ImportExportSession> exportSession(
			@ApiParam(value = "comma seperated list of sessionkeys", required = true) @RequestParam(value = "sessionkeys", defaultValue = "") final String sessionkeys,
			@ApiParam(value = "wether statistics shall be exported", required = true) @RequestParam(value = "withAnswerStatistics", defaultValue = "false") final Boolean withAnswerStatistics,
			@ApiParam(value = "wether interposed questions shall be exported", required = true) @RequestParam(value = "withFeedbackQuestions", defaultValue = "false") final Boolean withFeedbackQuestions,
			@ApiParam(value = "http servlet response", required = true) final HttpServletResponse response
		) {
		List<ImportExportSession> sessions = new ArrayList<ImportExportSession>();
		ImportExportSession temp;
		String[] splittedKeys = sessionkeys.split(",");
		for (String key : splittedKeys) {
			sessionService.setActive(key, false);
			temp = sessionService.exportSession(key);
			if (temp != null) {
				sessions.add(temp);
			}
			sessionService.setActive(key, true);
		}
		return sessions;
	}

	@ApiOperation(value = "Locks or unlocks a Session",
			nickname = "lockSession")
	@ApiResponses(value = {
		@ApiResponse(code = 404, message = HTML_STATUS_404)
	})
	@RequestMapping(value = "/{sessionkey}/lock", method = RequestMethod.POST)
	public Session lockSession(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "lock", required = true) @RequestParam(required = false) final Boolean lock,
			@ApiParam(value = "http servlet response", required = true) final HttpServletResponse response
			) {
		if (lock != null) {
			return sessionService.setActive(sessionkey, lock);
		}
		response.setStatus(HttpStatus.NOT_FOUND.value());
		return null;
	}

	@ApiOperation(value = "retrieves a value for the learning progress",
			nickname = "learningProgress")
	@RequestMapping(value = "/{sessionkey}/learningprogress", method = RequestMethod.GET)
	public LearningProgressValues learningProgress(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "progress type", required = false) @RequestParam(value = "type", defaultValue = "questions") final String progressType,
			@ApiParam(value = "question variant", required = false) @RequestParam(value = "questionVariant", required = false) final String questionVariant,
			@ApiParam(value = "http servlet response", required = true) final HttpServletResponse response
			) {
		return sessionService.getLearningProgress(sessionkey, progressType, questionVariant);
	}

	@ApiOperation(value = "retrieves a value for the learning progress for the current user",
			nickname = "myLearningProgress")
	@RequestMapping(value = "/{sessionkey}/mylearningprogress", method = RequestMethod.GET)
	public LearningProgressValues myLearningProgress(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@RequestParam(value = "type", defaultValue = "questions") final String progressType,
			@RequestParam(value = "questionVariant", required = false) final String questionVariant,
			@ApiParam(value = "http servlet response", required = true) final HttpServletResponse response
			) {
		return sessionService.getMyLearningProgress(sessionkey, progressType, questionVariant);
	}

	@ApiOperation(value = "retrieves all session features",
			nickname = "sessionFeatures")
	@RequestMapping(value = "/{sessionkey}/features", method = RequestMethod.GET)
	public SessionFeature sessionFeatures(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "http servlet response", required = true) final HttpServletResponse response
			) {
		return sessionService.getSessionFeatures(sessionkey);
	}

	@RequestMapping(value = "/{sessionkey}/features", method = RequestMethod.PUT)
	@ApiOperation(value = "change all session features",
			nickname = "changeSessionFeatures")
	public SessionFeature changeSessionFeatures(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "session feature", required = true) @RequestBody final SessionFeature features,
			@ApiParam(value = "http servlet response", required = true) final HttpServletResponse response
			) {
		return sessionService.changeSessionFeatures(sessionkey, features);
	}

	@RequestMapping(value = "/{sessionkey}/lockfeedbackinput", method = RequestMethod.POST)
	@ApiOperation(value = "locks input of user live feedback",
			nickname = "lockFeedbackInput")
	public boolean lockFeedbackInput(
			@ApiParam(value = "session-key from current session", required = true) @PathVariable final String sessionkey,
			@ApiParam(value = "lock", required = true) @RequestParam(required = true) final Boolean lock,
			@ApiParam(value = "http servlet response", required = true) final HttpServletResponse response
			) {
		return sessionService.lockFeedbackInput(sessionkey, lock);
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
