/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2021 The ARSnova Team and Contributors
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

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.NotImplementedException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.services.IUserService;
import io.swagger.annotations.ApiParam;
import net.particify.arsnova.connector.client.ConnectorClient;
import net.particify.arsnova.connector.model.Course;
import net.particify.arsnova.connector.model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Provides access to a user's courses in an LMS such as Moodle.
 */
@RestController
public class CourseController extends PaginationController {
	@Autowired(required = false)
	private ConnectorClient connectorClient;

	@Autowired
	private IUserService userService;

	@Autowired
	private ISessionService sessionService;

	@RequestMapping(value = "/mycourses", method = RequestMethod.GET)
	public List<Course> myCourses(
			@ApiParam(value = "sort my courses by name", required = true)
			@RequestParam(value = "sortby", defaultValue = "startdate") final String sortby
			) {

		final User currentUser = userService.getCurrentUser();

		if (currentUser == null || currentUser.getUsername() == null) {
			throw new UnauthorizedException();
		}

		if (connectorClient == null) {
			throw new NotImplementedException();
		}

		final List<Course> result = new ArrayList<>();

		for (final Course course : connectorClient.getCourses(currentUser.getUsername()).getCourse()) {
			if (
					course.getMembership().isMember()
					&& course.getMembership().getUserrole().equals(UserRole.TEACHER)
					) {
				result.add(course);
			}
		}

		switch (sortby) {
			case "name":
				Collections.sort(result, Comparator
						.comparing(Course::getFullname)
						.thenComparing(Course::getStartdate, Comparator.nullsFirst(Comparator.reverseOrder())));
				break;
			case "shortname":
				Collections.sort(result, Comparator
						.comparing(Course::getShortname)
						.thenComparing(Course::getStartdate, Comparator.nullsFirst(Comparator.reverseOrder())));
				break;
			case "enddate":
				Collections.sort(result, Comparator
						.comparing(Course::getEnddate, Comparator.nullsFirst(Comparator.reverseOrder()))
						.thenComparing(Course::getFullname));
				break;
			default:
				Collections.sort(result, Comparator
						.comparing(Course::getStartdate, Comparator.nullsFirst(Comparator.reverseOrder()))
						.thenComparing(Course::getFullname));
				break;
		}

		return result;
	}

	@RequestMapping(value = "/mycoursesessions", method = RequestMethod.GET)
	public final List<Session> myCourseSessions() {
		final User currentUser = userService.getCurrentUser();

		if (currentUser == null || currentUser.getUsername() == null) {
			throw new UnauthorizedException();
		}

		return sessionService.getCourseSessions(currentUser, offset, limit);
	}
}
