package de.thm.arsnova.controller;

import de.thm.arsnova.connector.client.ConnectorClient;
import de.thm.arsnova.services.StubUserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
public class CourseControllerTest {

	private MockMvc mockMvc;

	@InjectMocks
	private final CourseController courseController = new CourseController();

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private StubUserService userService;

	@Mock
	private ConnectorClient connectorClient;

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
	public void setup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	public void testShouldIndicateNotImplementedIfInactiveClient() throws Exception {
		setAuthenticated(true, "ptsr00");

		mockMvc.perform(get("/mycourses").accept(MediaType.TEXT_PLAIN))
		.andExpect(status().isNotImplemented());
	}

	@Test
	public void testShouldNotReturnCurrentUsersCoursesIfUnauthorized() throws Exception {
		setAuthenticated(false, "nobody");

		mockMvc.perform(get("/mycourses").accept(MediaType.TEXT_PLAIN))
		.andExpect(status().isUnauthorized());
	}
}
