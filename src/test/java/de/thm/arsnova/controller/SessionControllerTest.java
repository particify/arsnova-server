package de.thm.arsnova.controller;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

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

import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.StubUserService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/test/resources/test-config.xml" })
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

	@Before
	public void setUp() {
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
		handlerAdapter = applicationContext
				.getBean(AnnotationMethodHandlerAdapter.class);
	}

	@Test
	public void testShouldNotGetUnknownSession() {
		userService.setUserAuthenticated(true);
		
		request.setMethod("GET");
		request.setRequestURI("/session/00000000");
		try {
			final ModelAndView mav = handlerAdapter.handle(request, response,
					sessionController);
			assertNull(mav);
			assertTrue(response.getStatus() == 404);
		} catch (NotFoundException e) {
			return;
		} catch (Exception e) {
			e.printStackTrace();
			fail("An exception occured");
		}

		fail("Expected exception 'NotFoundException' did not occure");
	}

	@Test
	public void testShouldNotGetForbiddenSession() {
		userService.setUserAuthenticated(true);
		
		request.setMethod("GET");
		request.setRequestURI("/session/99999999");
		try {
			final ModelAndView mav = handlerAdapter.handle(request, response,
					sessionController);
			assertNull(mav);
			assertTrue(response.getStatus() == 403);
		} catch (ForbiddenException e) {
			return;
		} catch (Exception e) {
			e.printStackTrace();
			fail("An exception occured");
		}

		fail("Expected exception 'ForbiddenException' did not occure");
	}
	
	@Test
	public void testShouldNotGetFeedbackForUnknownSession() {
		userService.setUserAuthenticated(true);
		
		request.setMethod("GET");
		request.setRequestURI("/session/00000000/feedback");
		try {
			final ModelAndView mav = handlerAdapter.handle(request, response,
					sessionController);
			assertNull(mav);
			assertTrue(response.getStatus() == 404);
		} catch (NotFoundException e) {
			return;
		} catch (Exception e) {
			e.printStackTrace();
			fail("An exception occured");
		}

		fail("Expected exception 'NotFoundException' did not occure");
	}
	
	@Test
	public void testShouldNotGetSessionIfUnauthorized() {
		userService.setUserAuthenticated(false);
		
		request.setMethod("GET");
		request.setRequestURI("/session/00000000");
		try {
			final ModelAndView mav = handlerAdapter.handle(request, response,
					sessionController);
			assertNull(mav);
			assertTrue(response.getStatus() == 403);
		} catch (UnauthorizedException e) {
			return;
		} catch (Exception e) {
			e.printStackTrace();
			fail("An exception occured");
		}

		fail("Expected exception 'UnauthorizedException' did not occure");
	}
}
