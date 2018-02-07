/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
package de.thm.arsnova.controller.v2;

import de.thm.arsnova.controller.AbstractControllerTest;
import de.thm.arsnova.services.StubUserService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthenticationControllerTest extends AbstractControllerTest {

	@Autowired
	private StubUserService userService;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Before
	public void setup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	public void testGuestLogin() throws Exception {
		mockMvc.perform(
				get("/v2/auth/doLogin")
				.param("type", "guest")
				).andExpect(status().isOk());
	}

	@Test
	@Ignore("Mockup needed for UserService")
	public void testReuseGuestLogin() throws Exception {
		mockMvc.perform(
				get("/v2/auth/doLogin")
						.param("type", "guest")
		).andExpect(status().isOk());
		final Authentication auth1 = SecurityContextHolder.getContext().getAuthentication();
		cleanup();
		mockMvc.perform(
				get("/v2/auth/doLogin")
				.param("type", "guest").param("user", auth1.getName())
				).andExpect(status().isOk());

		final Authentication auth2 = SecurityContextHolder.getContext().getAuthentication();
		assertEquals(auth2.getClass(), UsernamePasswordAuthenticationToken.class);
		assertNotSame(auth1, auth2);
		assertEquals(auth1, auth2);
	}

	@Test
	@Ignore("Causes 'ServletException: Circular view path' for an unknown reason.")
	public void testUser() throws Exception {
		userService.setUserAuthenticated(true);

		mockMvc.perform(get("/v2/auth/whoami").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$.username").value("ptsr00"))
		.andExpect(jsonPath("$.type").value("ldap"));
	}

	@Test
	public void testLogoutWithoutRedirect() throws Exception {
		mockMvc.perform(get("/v2/auth/logout"))
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/"));
	}
}
