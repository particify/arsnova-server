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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.thm.arsnova.connector.client.ConnectorClient;
import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.services.IUserService;

@Controller
public class CourseController extends AbstractController {

	public static final Logger LOGGER = LoggerFactory
			.getLogger(CourseController.class);

	@Autowired(required = false)
	private ConnectorClient connectorClient;

	@Autowired
	private IUserService userService;

	@Autowired
	private ISessionService sessionService;

	@RequestMapping(value = "/mycourses", method = RequestMethod.GET)
	@ResponseBody
	public final List<Course> myCourses() {
		String username = userService.getCurrentUser().getUsername();

		if (username == null) {
			throw new UnauthorizedException();
		}

		if (connectorClient == null) {
			throw new NotFoundException();
		}

		List<Course> result = connectorClient.getCourses(username).getCourse();
		Collections.sort(result, new CourseNameComperator());
		
		return result;
	}

	private class CourseNameComperator implements Comparator<Course> {
		@Override
		public int compare(Course course1, Course course2) {
			return course1.getFullname().compareToIgnoreCase(course2.getFullname());
		}
	}
}
