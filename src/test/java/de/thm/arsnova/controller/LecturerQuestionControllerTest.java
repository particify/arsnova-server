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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/spring/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/test/resources/test-config.xml",
		"file:src/test/resources/test-socketioconfig.xml"
})
@ActiveProfiles("test")
public class LecturerQuestionControllerTest {

	@Autowired
	private StubUserService userService;

	@Autowired
	private LecturerQuestionController questionController;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	private void setAuthenticated(final boolean isAuthenticated, final String username) {
		if (isAuthenticated) {
			final List<GrantedAuthority> ga = new ArrayList<GrantedAuthority>();
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
