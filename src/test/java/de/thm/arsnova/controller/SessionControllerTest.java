package de.thm.arsnova.controller;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;

import de.thm.arsnova.dao.StubDatabaseDao;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.StubUserService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/spring/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/test/resources/test-config.xml"
})
public class SessionControllerTest {

	@Inject
	private ApplicationContext applicationContext;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private HandlerAdapter handlerAdapter;

	@Autowired
	private SessionController sessionController;

	@Autowired
	private StubUserService userService;
	
	@Autowired
	private StubDatabaseDao databaseDao;

	@After
	public final void cleanup() {
		databaseDao.cleanupTestData();
	}

	@Before
	public void setUp() {
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
		handlerAdapter = applicationContext
				.getBean(AnnotationMethodHandlerAdapter.class);
	}

	@Test(expected = NotFoundException.class)
	public void testShouldNotGetUnknownSession() throws Exception {
		userService.setUserAuthenticated(true);

		request.setMethod("GET");
		request.setRequestURI("/session/00000000");
		final ModelAndView mav = handlerAdapter.handle(request, response,
				sessionController);

		assertNull(mav);
		assertTrue(response.getStatus() == 404);
	}

	@Test(expected = ForbiddenException.class)
	public void testShouldNotGetForbiddenSession() throws Exception {
		userService.setUserAuthenticated(true);

		request.setMethod("GET");
		request.setRequestURI("/session/99999999");
		final ModelAndView mav = handlerAdapter.handle(request, response,
				sessionController);

		assertNull(mav);
		assertTrue(response.getStatus() == 403);
	}

	@Test(expected = UnauthorizedException.class)
	public void testShouldNotGetSessionIfUnauthorized() throws Exception {
		userService.setUserAuthenticated(false);

		request.setMethod("GET");
		request.setRequestURI("/session/00000000");
		final ModelAndView mav = handlerAdapter.handle(request, response,
				sessionController);

		assertNull(mav);
		assertTrue(response.getStatus() == 401);
	}

	@Test(expected = UnauthorizedException.class)
	public void testShouldNotGetSessionIfAnonymous() throws Exception {
		userService.setUserAuthenticated(false);
		userService.useAnonymousUser();

		request.setMethod("GET");
		request.setRequestURI("/session/00000000");
		final ModelAndView mav = handlerAdapter.handle(request, response,
				sessionController);

		assertNull(mav);
		assertTrue(response.getStatus() == 401);
	}

	@Test(expected = UnauthorizedException.class)
	public void testShouldCreateSessionIfUnauthorized() throws Exception {
		userService.setUserAuthenticated(false);

		request.setMethod("POST");
		request.setRequestURI("/session/");
		request.setContentType("application/json");
		request.setContent("{}".getBytes());

		final ModelAndView mav = handlerAdapter.handle(request, response,
				sessionController);

		assertNull(mav);
		assertTrue(response.getStatus() == 401);
	}
}
