/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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

import de.thm.arsnova.connector.client.ConnectorClient;
import de.thm.arsnova.services.StubUserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CourseControllerTest extends AbstractControllerTest {

	private MockMvc mockMvc;

	@InjectMocks
	private final CourseController courseController = new CourseController();

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private StubUserService userService;

	@Mock
	private ConnectorClient connectorClient;

	@Before
	public void setup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	public void testShouldIndicateNotImplementedIfInactiveClient() throws Exception {
		setAuthenticated(true, "ptsr00");

		mockMvc.perform(get("/mycourses").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotImplemented());
	}

	@Test
	public void testShouldNotReturnCurrentUsersCoursesIfUnauthorized() throws Exception {
		setAuthenticated(false, "nobody");

		mockMvc.perform(get("/mycourses").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}
}
