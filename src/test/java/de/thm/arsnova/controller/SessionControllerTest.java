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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class SessionControllerTest extends AbstractControllerTest {

	@Autowired
	private StubUserService userService;

	@Autowired
	private SessionController sessionController;

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
	public void testShouldNotGetUnknownSession() throws Exception {
		setAuthenticated(true, "ptsr00");

		mockMvc.perform(get("/session/00000000").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isNotFound());
	}

	@Test
	public void testShouldNotGetUnknownSessionIfUnauthorized() throws Exception {
		setAuthenticated(false, "ptsr00");

		mockMvc.perform(get("/session/00000000").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isUnauthorized());
	}

	@Test
	public void testShouldCreateSessionIfUnauthorized() throws Exception {
		setAuthenticated(false, "ptsr00");

		mockMvc.perform(
				post("/session/")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"keyword\":12345678}")
				)
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void testShouldNotReturnMySessionsIfUnauthorized() throws Exception {
		setAuthenticated(false, "ptsr00");

		mockMvc.perform(get("/session/").param("ownedonly", "true").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isUnauthorized());
	}

	@Test
	public void testShouldNotReturnMyVisitedSessionsIfUnauthorized() throws Exception {
		setAuthenticated(false, "ptsr00");

		mockMvc.perform(get("/session/").param("visitedonly", "true").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isUnauthorized());
	}

	@Test
	public void testShouldShowUnimplementedIfNoFlagIsSet() throws Exception {
		setAuthenticated(false, "ptsr00");

		mockMvc.perform(get("/session/").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isNotImplemented());
	}

	@Test
	public void testShouldReturnActiveUserCount() throws Exception {
		setAuthenticated(false, "ptsr00");

		mockMvc.perform(get("/session/12345678/activeusercount").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
		.andExpect(content().string("0"));
	}

	@Test
	public void testShouldReturnXDeprecatedApiHeaderForActiveUserCount() throws Exception {
		setAuthenticated(false, "ptsr00");

		mockMvc.perform(get("/session/12345678/activeusercount").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(header().string(AbstractController.X_DEPRECATED_API, "1"));
	}

	@Test
	public void testShouldEndInForbidden() throws Exception {
		setAuthenticated(true, "ptsr00");

		mockMvc.perform(
				put("/session/12345678")
				.content("{\"keyword\":\"12345678\", \"name\":\"Testsession\"}, \"shortName\":\"TS\", \"creator\":\"ptsr00\", \"active\":true")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		setAuthenticated(true, "other");

		mockMvc.perform(delete("/session/12345678")).andExpect(status().isForbidden());
	}
}
