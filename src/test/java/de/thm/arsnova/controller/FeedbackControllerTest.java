package de.thm.arsnova.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import de.thm.arsnova.services.StubUserService;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/spring/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-security.xml",
		"file:src/test/resources/test-config.xml",
		"file:src/test/resources/test-socketioconfig.xml"
})
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
		.andExpect(status().isOk());
	}
}
