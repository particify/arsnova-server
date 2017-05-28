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

import de.thm.arsnova.services.StubUserService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ContentControllerTest extends AbstractControllerTest {

	@Autowired
	private StubUserService userService;

	@Autowired
	private ContentController questionController;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Before
	public void startup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		SecurityContextHolder.clearContext();
	}

	@After
	public void cleanup() {
		SecurityContextHolder.clearContext();
		userService.setUserAuthenticated(false);
	}

	@Test
	public void testShouldNotGetLecturerQuestionsIfUnauthorized() throws Exception {
		setAuthenticated(false, "nobody");

		mockMvc.perform(
				get("/lecturerquestion/")
				.param("sessionkey", "12345678").param("lecturequestionsonly", "true")
				.accept(MediaType.APPLICATION_JSON)
				).andExpect(status().isUnauthorized()
						);
	}

	@Test
	public void testShouldNotGetPreparationQuestionsIfUnauthorized() throws Exception {
		setAuthenticated(false, "nobody");

		mockMvc.perform(
				get("/lecturerquestion/")
				.param("sessionkey", "12345678").param("preparationquestionsonly", "true")
				.accept(MediaType.APPLICATION_JSON)
				).andExpect(status().isUnauthorized()
						);
	}

	@Test
	public void testShouldReturnQuestionCount() throws Exception {
		setAuthenticated(true, "somebody");

		mockMvc.perform(
				get("/lecturerquestion/count")
				.param("sessionkey", "12345678").param("lecturequestionsonly", "true")
				.accept(MediaType.APPLICATION_JSON)
				).andExpect(status().isOk())
				.andExpect(content().string("0")
						);
	}

	@Test
	public void testShouldReturnXDeprecatedApiHeaderForQuestionCount() throws Exception {
		setAuthenticated(true, "somebody");

		mockMvc.perform(
				get("/lecturerquestion/count")
				.param("sessionkey", "12345678").param("lecturequestionsonly", "true")
				.accept(MediaType.APPLICATION_JSON)
				).andExpect(status().isOk())
				.andExpect(header().string(AbstractController.X_DEPRECATED_API, "1")
						);
	}
}
