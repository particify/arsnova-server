package de.thm.arsnova.controller;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

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
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.services.StubUserService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/test/resources/test-config.xml"
})
public class FeedbackControllerTest {

	@Inject
	private ApplicationContext applicationContext;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private HandlerAdapter handlerAdapter;

	@Autowired
	private FeedbackController feedbackController;

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
		handlerAdapter = applicationContext.getBean(AnnotationMethodHandlerAdapter.class);
	}

	@Test(expected = NotFoundException.class)
	public void testShouldNotGetFeedbackForUnknownSession() throws Exception {
		userService.setUserAuthenticated(true);

		request.setMethod("GET");
		request.setRequestURI("/session/00000000/feedback");
		final ModelAndView mav = handlerAdapter.handle(request, response, feedbackController);

		assertNull(mav);
		assertTrue(response.getStatus() == 404);
	}

	@Test(expected = NoContentException.class)
	public void testShouldNotGetAverageFeedbackContentForSessionWithoutFeedback() throws Exception {
		userService.setUserAuthenticated(true);

		request.setMethod("GET");
		request.setRequestURI("/session/12345678/averagefeedback");
		final ModelAndView mav = handlerAdapter.handle(request, response, feedbackController);

		assertNull(mav);
		assertTrue(response.getStatus() == 204);
	}

	@Test
	public void testShouldNotGetCorrectFeedbackCountForSessionWithoutFeedback() throws Exception {
		userService.setUserAuthenticated(true);

		request.setMethod("GET");
		request.setRequestURI("/session/12345678/feedbackcount");
		handlerAdapter.handle(request, response, feedbackController);

		assertTrue(response.getStatus() == 200);
		assertEquals("0", response.getContentAsString());
	}
	
	@Test
	public void testShouldReturnFeedback() throws Exception {
		userService.setUserAuthenticated(true);

		request.setMethod("GET");
		request.setRequestURI("/session/87654321/feedback");
		handlerAdapter.handle(request, response, feedbackController);

		assertTrue(response.getStatus() == 200);
		assertEquals("{\"values\":[2,3,5,7]}", response.getContentAsString());
	}
}
