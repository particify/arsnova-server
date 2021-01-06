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

import de.thm.arsnova.services.StubUserService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/spring/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/test/resources/test-config.xml",
		"file:src/test/resources/test-socketioconfig.xml"
})
@ActiveProfiles("test")
public class SessionControllerTest {

	@Autowired
	private StubUserService userService;

	@Autowired
	private SessionController sessionController;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	private void setAuthenticated(final boolean isAuthenticated, final String username) {
		if (isAuthenticated) {
			final List<GrantedAuthority> ga = new ArrayList<>();
			final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, "secret", ga);
			SecurityContextHolder.getContext().setAuthentication(token);
			userService.setUserAuthenticated(isAuthenticated, username);
		} else {
			userService.setUserAuthenticated(isAuthenticated);
		}
	}

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
