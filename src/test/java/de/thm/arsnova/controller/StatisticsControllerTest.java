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
public class StatisticsControllerTest {

	@Autowired
	private StatisticsController statisticsController;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Before
	public void setup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	public void testShouldGetCurrentOnlineUsers() throws Exception {
		mockMvc.perform(get("/statistics/activeusercount").accept(MediaType.TEXT_PLAIN))
		.andExpect(status().isOk())
		.andExpect(content().contentTypeCompatibleWith("text/plain"));
	}

	@Test
	public void testShouldSendXDeprecatedApiForGetCurrentOnlineUsers() throws Exception {
		mockMvc.perform(get("/statistics/activeusercount").accept(MediaType.TEXT_PLAIN))
		.andExpect(status().isOk())
		.andExpect(content().contentTypeCompatibleWith("text/plain"))
		.andExpect(header().string(AbstractController.X_DEPRECATED_API,"1"));
	}

	@Test
	public void testShouldGetSessionCount() throws Exception {
		mockMvc.perform(get("/statistics/sessioncount").accept(MediaType.TEXT_PLAIN))
		.andExpect(status().isOk())
		.andExpect(content().contentTypeCompatibleWith("text/plain"))
		.andExpect(content().string("3"));
	}

	@Test
	public void testShouldSendXDeprecatedApiForGetSessionCount() throws Exception {
		mockMvc.perform(get("/statistics/sessioncount").accept(MediaType.TEXT_PLAIN))
		.andExpect(status().isOk())
		.andExpect(content().contentTypeCompatibleWith("text/plain"))
		.andExpect(header().string(AbstractController.X_DEPRECATED_API,"1"));
	}

	@Test
	public void testShouldGetStatistics() throws Exception {
		mockMvc.perform(get("/statistics").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$.answers").value(0))
		.andExpect(jsonPath("$.questions").value(0))
		.andExpect(jsonPath("$.openSessions").value(3))
		.andExpect(jsonPath("$.closedSessions").value(0))
		.andExpect(jsonPath("$.activeUsers").exists())
		.andExpect(jsonPath("$.interposedQuestions").exists());
	}

	@Test
	public void testShouldGetCacheControlHeaderForStatistics() throws Exception {
		mockMvc.perform(get("/statistics").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(header().string("cache-control", "public, max-age=60"));
	}
}
