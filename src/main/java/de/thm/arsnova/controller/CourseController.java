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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import de.thm.arsnova.connector.client.ConnectorClient;
import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.connector.model.UserRole;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.NotImplementedException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.IUserService;

/**
 * Provides access to a user's courses in an LMS such as Moodle.
 */
@RestController
public class CourseController extends AbstractController {

	public static final Logger LOGGER = LoggerFactory.getLogger(CourseController.class);

	@Autowired(required = false)
	private ConnectorClient connectorClient;

	@Autowired
	private IUserService userService;

	@RequestMapping(value = "/mycourses", method = RequestMethod.GET)
	public List<Course> myCourses(
			@ApiParam(value="sort my courses by name", required=true)
			@RequestParam(value = "sortby", defaultValue = "name") final String sortby
			) {

		final User currentUser = userService.getCurrentUser();

		if (currentUser == null || currentUser.getUsername() == null) {
			throw new UnauthorizedException();
		}

		if (connectorClient == null) {
			throw new NotImplementedException();
		}

		final List<Course> result = new ArrayList<Course>();

		for (final Course course : connectorClient.getCourses(currentUser.getUsername()).getCourse()) {
			if (
					course.getMembership().isMember()
					&& course.getMembership().getUserrole().equals(UserRole.TEACHER)
					) {
				result.add(course);
			}
		}

		if ("shortname".equals(sortby)) {
			Collections.sort(result, new CourseShortNameComperator());
		} else {
			Collections.sort(result, new CourseNameComperator());
		}

		return result;
	}

	private static class CourseNameComperator implements Comparator<Course>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final Course course1, final Course course2) {
			return course1.getFullname().compareToIgnoreCase(course2.getFullname());
		}
	}

	private static class CourseShortNameComperator implements Comparator<Course>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final Course course1, final Course course2) {
			return course1.getShortname().compareToIgnoreCase(course2.getShortname());
		}
	}
}
