package de.thm.arsnova.controller;

import static org.junit.Assert.assertEquals;

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
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/spring/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/test/resources/test-config.xml"
})
public class StatisticsControllerTest {

	@Inject
	private ApplicationContext applicationContext;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private HandlerAdapter handlerAdapter;

	@Autowired
	private StatisticsController statisticsController;

	@Before
	public final void setUp() {
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
		handlerAdapter = applicationContext.getBean(AnnotationMethodHandlerAdapter.class);
	}

	@Test
	public final void testShouldGetCurrentOnlineUsers() throws Exception {
		request.setMethod("GET");
		request.setRequestURI("/statistics/activeusercount");
		handlerAdapter.handle(request, response, statisticsController);

		assertEquals("0", response.getContentAsString());
	}
	
	@Test
	public final void testShouldGetSessionCount() throws Exception {
		request.setMethod("GET");
		request.setRequestURI("/statistics/sessioncount");
		handlerAdapter.handle(request, response, statisticsController);

		assertEquals("3", response.getContentAsString());
	}
	
	@Test
	public final void testShouldGetStatistics() throws Exception {
		request.setMethod("GET");
		request.setRequestURI("/statistics/");
		handlerAdapter.handle(request, response, statisticsController);
		
		String expected = "{\"answers\":0,\"questions\":0,\"openSessions\":3,\"closedSessions\":0,\"activeUsers\":0}";
		assertEquals(expected, response.getContentAsString());
	}
}
