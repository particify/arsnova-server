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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import de.thm.arsnova.connector.client.ConnectorClient;
import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.connector.model.UserRole;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.IUserService;

@Controller
public class CourseController extends AbstractController {

	public static final Logger LOGGER = LoggerFactory
			.getLogger(CourseController.class);

	@Autowired(required = false)
	private ConnectorClient connectorClient;

	@Autowired
	private IUserService userService;

	@RequestMapping(value = "/mycourses", method = RequestMethod.GET)
	@ResponseBody
	public final List<Course> myCourses(
			@RequestParam(value = "sortby", defaultValue = "name") final String sortby
	) {
		String username = userService.getCurrentUser().getUsername();

		if (username == null) {
			throw new UnauthorizedException();
		}

		if (connectorClient == null) {
			throw new NotFoundException();
		}

		List<Course> result = new ArrayList<Course>();

		for (Course course : connectorClient.getCourses(username).getCourse()) {
			if (
					course.getMembership().isMember()
					&& course.getMembership().getUserrole().equals(UserRole.TEACHER)
			) {
				result.add(course);
			}
		}

		if (sortby != null && sortby.equals("shortname")) {
			Collections.sort(result, new CourseShortNameComperator());
		} else {
			Collections.sort(result, new CourseNameComperator());
		}

		return result;
	}

	private static class CourseNameComperator implements Comparator<Course>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(Course course1, Course course2) {
			return course1.getFullname().compareToIgnoreCase(course2.getFullname());
		}
	}

	private static class CourseShortNameComperator implements Comparator<Course>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(Course course1, Course course2) {
			return course1.getShortname().compareToIgnoreCase(course2.getShortname());
		}
	}
}
