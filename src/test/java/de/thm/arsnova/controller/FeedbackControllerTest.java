/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2016 The ARSnova Team
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
public class FeedbackControllerTest {

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
	public void testShouldNotGetFeedbackForUnknownSession() throws Exception {
		userService.setUserAuthenticated(true);
		mockMvc.perform(get("/session/00000000/feedback").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isNotFound());
	}

	@Test
	public void testShouldNotGetAverageFeedbackContentForSessionWithoutFeedback() throws Exception {
		userService.setUserAuthenticated(true);
		mockMvc.perform(get("/session/12345678/averagefeedback").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isNoContent());
	}

	@Test
	public void testShouldNotGetCorrectFeedbackCountForSessionWithoutFeedback() throws Exception {
		userService.setUserAuthenticated(true);
		mockMvc.perform(get("/session/12345678/feedbackcount").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(content().string("0"));
	}

	@Test
	public void testShouldReturnFeedback() throws Exception {
		userService.setUserAuthenticated(true);
		mockMvc.perform(get("/session/87654321/feedback").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$.values").isArray());
	}

	@Test
	public void testShouldReturnXDeprecatedApiHeaderForFeedback() throws Exception {
		userService.setUserAuthenticated(true);
		mockMvc.perform(get("/session/87654321/feedback").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(header().string(AbstractController.X_DEPRECATED_API, "1"));
	}
}
