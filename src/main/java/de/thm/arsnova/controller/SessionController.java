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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.SessionInfo;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.services.SessionService.SessionNameComparator;
import de.thm.arsnova.services.SessionService.SessionInfoNameComparator;
import de.thm.arsnova.services.SessionService.SessionShortNameComparator;
import de.thm.arsnova.services.SessionService.SessionInfoShortNameComparator;
import de.thm.arsnova.web.DeprecatedApi;

@RestController
@RequestMapping("/session")
public class SessionController extends AbstractController {

	public static final Logger LOGGER = LoggerFactory.getLogger(SessionController.class);

	@Autowired
	private ISessionService sessionService;

	@Autowired
	private IUserService userService;

	@RequestMapping(value = "/{sessionkey}", method = RequestMethod.GET)
	public final Session joinSession(@PathVariable final String sessionkey) {
		final Session session = sessionService.getSession(sessionkey);
		if (!session.isCreator(userService.getCurrentUser())) {
			session.setCreator("NOT VISIBLE TO YOU");
		} else {
			session.setCreator(Sha512DigestUtils.shaHex(session.getCreator()));
		}
		return session;
	}

	@RequestMapping(value = "/{sessionkey}", method = RequestMethod.DELETE)
	public final void deleteSession(@PathVariable final String sessionkey) {
		sessionService.deleteSession(sessionkey);
	}

	@DeprecatedApi
	@RequestMapping(value = "/{sessionkey}/activeusercount", method = RequestMethod.GET)
	public final int countActiveUsers(@PathVariable final String sessionkey) {
		return sessionService.activeUsers(sessionkey);
	}

	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public final Session postNewSession(@RequestBody final Session session, final HttpServletResponse response) {
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

	@RequestMapping(value = "/{sessionkey}", method = RequestMethod.PUT)
	public final Session updateSession(
			@PathVariable final String sessionkey,
			@RequestBody final Session session
			) {
		return sessionService.updateSession(sessionkey, session);
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public final List<Session> getSessions(
			@RequestParam(value = "ownedonly", defaultValue = "false") final boolean ownedOnly,
			@RequestParam(value = "visitedonly", defaultValue = "false") final boolean visitedOnly,
			@RequestParam(value = "sortby", defaultValue = "name") final String sortby,
			final HttpServletResponse response
			) {
		List<Session> sessions = null;

		/* TODO implement all parameter combinations, implement use of user parameter */
		try {
			if (ownedOnly && !visitedOnly) {
				sessions = sessionService.getMySessions();
			} else if (visitedOnly && !ownedOnly) {
				sessions = sessionService.getMyVisitedSessions();
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
	@RequestMapping(value = "/", method = RequestMethod.GET, params = "statusonly=true")
	public final List<SessionInfo> getMySessions(
			@RequestParam(value = "visitedonly", defaultValue = "false") final boolean visitedOnly,
			@RequestParam(value = "sortby", defaultValue = "name") final String sortby,
			final HttpServletResponse response
			) {
		List<SessionInfo> sessions;
		if (!visitedOnly) {
			sessions = sessionService.getMySessionsInfo();
		} else {
			sessions = sessionService.getMyVisitedSessionsInfo();
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
	

	@RequestMapping(value = "/public_pool", method = RequestMethod.GET)
	public final List<Session> getPublicPoolSessions(
			final HttpServletResponse response
			) {
		List<Session> sessions;
		sessions = sessionService.getPublicPoolSessions();

		if (sessions == null || sessions.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return null;
		}

		return sessions;
	}

	@RequestMapping(value = "/{sessionkey}/lock", method = RequestMethod.POST)
	public final Session lockSession(
			@PathVariable final String sessionkey,
			@RequestParam(required = false) final Boolean lock,
			final HttpServletResponse response
			) {
		if (lock != null) {
			return sessionService.setActive(sessionkey, lock);
		}
		response.setStatus(HttpStatus.NOT_FOUND.value());
		return null;
	}

	@RequestMapping(value = "/{sessionkey}/learningprogress", method = RequestMethod.GET)
	public final int learningProgress(
			@PathVariable final String sessionkey,
			final HttpServletResponse response
			) {
		return sessionService.getLearningProgress(sessionkey);
	}

	@RequestMapping(value = "/{sessionkey}/mylearningprogress", method = RequestMethod.GET)
	public final JSONObject myLearningProgress(
			@PathVariable final String sessionkey,
			final HttpServletResponse response
			) {
		final SimpleEntry<Integer, Integer> result = sessionService.getMyLearningProgress(sessionkey);
		final JSONObject json = new JSONObject();
		json.put("myprogress", result.getKey());
		json.put("courseprogress", result.getValue());
		return json;
	}

	/* internal redirections */

	@RequestMapping(value = "/{sessionKey}/lecturerquestion")
	public final String redirectLecturerQuestion(
			@PathVariable final String sessionKey,
			final HttpServletResponse response
			) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/lecturerquestion/?sessionkey=%s", sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/lecturerquestion/{arg1}")
	public final String redirectLecturerQuestionWithOneArgument(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			final HttpServletResponse response
			) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/lecturerquestion/%s/?sessionkey=%s", arg1, sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/lecturerquestion/{arg1}/{arg2}")
	public final String redirectLecturerQuestionWithTwoArguments(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			@PathVariable final String arg2,
			final HttpServletResponse response
			) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/lecturerquestion/%s/%s/?sessionkey=%s", arg1, arg2, sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/lecturerquestion/{arg1}/{arg2}/{arg3}")
	public final String redirectLecturerQuestionWithThreeArguments(
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
	public final String redirectAudienceQuestion(
			@PathVariable final String sessionKey,
			final HttpServletResponse response
			) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/audiencequestion/?sessionkey=%s", sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/audiencequestion/{arg1}")
	public final String redirectAudienceQuestionWithOneArgument(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			final HttpServletResponse response
			) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/audiencequestion/%s/?sessionkey=%s", arg1, sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/audiencequestion/{arg1}/{arg2}")
	public final String redirectAudienceQuestionWithTwoArguments(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			@PathVariable final String arg2,
			final HttpServletResponse response
			) {
		response.addHeader(X_FORWARDED, "1");

		return String.format("forward:/audiencequestion/%s/%s/?sessionkey=%s", arg1, arg2, sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/audiencequestion/{arg1}/{arg2}/{arg3}")
	public final String redirectAudienceQuestionWithThreeArguments(
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
