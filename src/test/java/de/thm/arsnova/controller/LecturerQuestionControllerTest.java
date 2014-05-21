package de.thm.arsnova.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class LecturerQuestionControllerTest {

	@Autowired
	private StubUserService userService;

	@Autowired
	private LecturerQuestionController questionController;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	private void setAuthenticated(boolean isAuthenticated, String username) {
		if (isAuthenticated) {
			List<GrantedAuthority> ga = new ArrayList<GrantedAuthority>();
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, "secret", ga);
			SecurityContextHolder.getContext().setAuthentication(token);
			userService.setUserAuthenticated(isAuthenticated, username);
		} else {
			userService.setUserAuthenticated(isAuthenticated);
		}
	}

	@Before
	public final void startup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		SecurityContextHolder.clearContext();
	}

	@After
	public final void cleanup() {
		SecurityContextHolder.clearContext();
		userService.setUserAuthenticated(false);
	}

	@Test
	public void testShouldNotGetLecturerQuestionsIfUnauthorized() throws Exception {
		setAuthenticated(false, "nobody");

		mockMvc.perform(get("/lecturerquestion/").param("sessionkey", "12345678").param("lecturequestionsonly", "true"))
		.andExpect(status().isUnauthorized());
	}

	@Test
	public void testShouldNotGetPreparationQuestionsIfUnauthorized() throws Exception {
		setAuthenticated(false, "nobody");

		mockMvc.perform(get("/lecturerquestion/").param("sessionkey", "12345678").param("preparationquestionsonly", "true"))
		.andExpect(status().isUnauthorized());
	}
}
