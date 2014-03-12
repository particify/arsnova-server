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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.services.SessionService.SessionNameComperator;
import de.thm.arsnova.services.SessionService.SessionShortNameComperator;

@Controller
@RequestMapping("/session")
public class SessionController extends AbstractController {

	public static final Logger LOGGER = LoggerFactory.getLogger(SessionController.class);

	@Autowired
	private ISessionService sessionService;

	@Autowired
	private IUserService userService;

	@RequestMapping(value = "/{sessionkey}", method = RequestMethod.GET)
	@ResponseBody
	public final Session joinSession(@PathVariable final String sessionkey) {
		Session session = sessionService.joinSession(sessionkey);
		return session;
	}

	@RequestMapping(value = "/{sessionkey}", method = RequestMethod.DELETE)
	@ResponseBody
	public final void deleteSession(@PathVariable final String sessionkey) {
		User user = userService.getCurrentUser();
		sessionService.deleteSession(sessionkey, user);
	}

	@RequestMapping(value = "/{sessionkey}/online", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	public final LoggedIn registerAsOnlineUser(
			@PathVariable final String sessionkey,
			final HttpServletResponse response
	) {
		response.addHeader("X-Deprecated-API", "1");

		User user = userService.getCurrentUser();
		LoggedIn loggedIn = sessionService.registerAsOnlineUser(user, sessionkey);
		if (loggedIn != null) {
			return loggedIn;
		}

		throw new RuntimeException();
	}

	@RequestMapping(value = "/{sessionkey}/activeusercount", method = RequestMethod.GET)
	@ResponseBody
	public final int countActiveUsers(
			@PathVariable final String sessionkey,
			final HttpServletResponse response
	) {
		response.addHeader("X-Deprecated-API", "1");

		return userService.getUsersInSessionCount(sessionkey);
	}

	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	public final Session postNewSession(@RequestBody final Session session, final HttpServletResponse response) {
		if (session != null && session.isCourseSession()) {
			List<Course> courses = new ArrayList<Course>();
			Course course = new Course();
			course.setId(session.getCourseId());
			courses.add(course);
			int sessionCount = sessionService.countSessions(courses);
			if (sessionCount > 0) {
				String appendix = " (" + String.valueOf(sessionCount + 1) + ")";
				session.setName(session.getName() + appendix);
				session.setShortName(session.getShortName() + appendix);
			}
		}
		Session newSession = sessionService.saveSession(session);

		if (newSession == null) {
			response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
			return null;
		}

		return newSession;
	}

	@RequestMapping(value = "/{sessionkey}", method = RequestMethod.PUT)
	@ResponseBody
	public final Session updateSession(
			@PathVariable final String sessionkey,
			@RequestBody final Session session
	) {
		return sessionService.updateSession(sessionkey, session);
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	@ResponseBody
	public final List<Session> getSessions(
			@RequestParam(value = "ownedonly", defaultValue = "false") final boolean ownedOnly,
			@RequestParam(value = "visitedonly", defaultValue = "false") final boolean visitedOnly,
			@RequestParam(value = "sortby", defaultValue = "name") final String sortby,
			final HttpServletResponse response
	) {
		User user = userService.getCurrentUser();
		List<Session> sessions = null;

		/* TODO Could @Authorized annotation be used instead of this check? */
		if (null == user) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return null;
		}

		/* TODO implement all parameter combinations, implement use of user parameter */
		if (ownedOnly && !visitedOnly) {
			sessions = sessionService.getMySessions(user);
		} else if (visitedOnly && !ownedOnly) {
			sessions = sessionService.getMyVisitedSessions(userService.getCurrentUser());
		} else {
			response.setStatus(HttpStatus.NOT_IMPLEMENTED.value());
			return null;
		}

		if (sessions == null || sessions.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return null;
		}

		if (sortby != null && sortby.equals("shortname")) {
			Collections.sort(sessions, new SessionShortNameComperator());
		} else {
			Collections.sort(sessions, new SessionNameComperator());
		}

		return sessions;
	}

	@RequestMapping(value = "/{sessionkey}/lock", method = RequestMethod.POST)
	@ResponseBody
	public final Session lockSession(
			@PathVariable final String sessionkey,
			@RequestParam(required = false) final Boolean lock,
			final HttpServletResponse response
	) {
		if (lock != null) {
			return this.sessionService.setActive(sessionkey, lock);
		}
		response.setStatus(HttpStatus.NOT_FOUND.value());
		return null;
	}

	@RequestMapping(value = "/{sessionkey}/learningprogress", method = RequestMethod.GET)
	@ResponseBody
	public final int learningProgress(
			@PathVariable final String sessionkey,
			final HttpServletResponse response
	) {
		return sessionService.getLearningProgress(sessionkey);
	}

	@RequestMapping(value = "/{sessionkey}/mylearningprogress", method = RequestMethod.GET)
	@ResponseBody
	public final int myLearningProgress(
			@PathVariable final String sessionkey,
			final HttpServletResponse response
	) {
		return sessionService.getMyLearningProgress(sessionkey);
	}

	/* internal redirections */

	@RequestMapping(value = "/{sessionKey}/lecturerquestion")
	public final String redirectLecturerQuestion(
			@PathVariable final String sessionKey,
			final HttpServletResponse response
	) {
		response.addHeader("X-Forwarded", "1");

		return String.format("forward:/lecturerquestion/?sessionkey=%s", sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/lecturerquestion/{arg1}")
	public final String redirectLecturerQuestionWithOneArgument(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			final HttpServletResponse response
	) {
		response.addHeader("X-Forwarded", "1");

		return String.format("forward:/lecturerquestion/%s/?sessionkey=%s", arg1, sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/lecturerquestion/{arg1}/{arg2}")
	public final String redirectLecturerQuestionWithTwoArguments(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			@PathVariable final String arg2,
			final HttpServletResponse response
	) {
		response.addHeader("X-Forwarded", "1");

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
		response.addHeader("X-Forwarded", "1");

		return String.format("forward:/lecturerquestion/%s/%s/%s/?sessionkey=%s", arg1, arg2, arg3, sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/audiencequestion")
	public final String redirectAudienceQuestion(
			@PathVariable final String sessionKey,
			final HttpServletResponse response
	) {
		response.addHeader("X-Forwarded", "1");

		return String.format("forward:/audiencequestion/?sessionkey=%s", sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/audiencequestion/{arg1}")
	public final String redirectAudienceQuestionWithOneArgument(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			final HttpServletResponse response
	) {
		response.addHeader("X-Forwarded", "1");

		return String.format("forward:/audiencequestion/%s/?sessionkey=%s", arg1, sessionKey);
	}

	@RequestMapping(value = "/{sessionKey}/audiencequestion/{arg1}/{arg2}")
	public final String redirectAudienceQuestionWithTwoArguments(
			@PathVariable final String sessionKey,
			@PathVariable final String arg1,
			@PathVariable final String arg2,
			final HttpServletResponse response
	) {
		response.addHeader("X-Forwarded", "1");

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
		response.addHeader("X-Forwarded", "1");

		return String.format("forward:/audiencequestion/%s/%s/%s/?sessionkey=%s", arg1, arg2, arg3, sessionKey);
	}
}
