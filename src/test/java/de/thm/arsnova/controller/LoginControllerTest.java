/*
 * Copyright (C) 2012 THM webMedia
 * 
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.view.RedirectView;

import de.thm.arsnova.services.StubUserService;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={
		"file:src/main/webapp/WEB-INF/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/test/resources/test-config.xml"
})
public class LoginControllerTest {

	@Inject
	private ApplicationContext applicationContext;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private HandlerAdapter handlerAdapter;
	
	@Autowired
	private LoginController loginController;
	
	@Autowired
	private StubUserService userService;
	
	@Before
	public void setUp() throws Exception {
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
		handlerAdapter = applicationContext.getBean(AnnotationMethodHandlerAdapter.class);
	}

	@Test
	public void testGuestLogin() throws Exception {
		request.setMethod("GET");
		request.setRequestURI("/doLogin");
		request.addParameter("type", "guest");

		final ModelAndView mav = handlerAdapter.handle(request, response, loginController);

		assertNotNull(mav);
		assertNotNull(mav.getView());
		RedirectView view = (RedirectView) mav.getView();
		assertEquals("/#auth/checkLogin", view.getUrl());
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		assertEquals(auth.getClass(), UsernamePasswordAuthenticationToken.class);
	}
	
	@Test
	public void testReuseGuestLogin() throws Exception {
		request.setMethod("GET");
		request.setRequestURI("/doLogin");
		request.addParameter("type", "guest");
		request.addParameter("user", "Guest1234567890");

		final ModelAndView mav = handlerAdapter.handle(request, response, loginController);

		assertNotNull(mav);
		assertNotNull(mav.getView());
		RedirectView view = (RedirectView) mav.getView();
		assertEquals("/#auth/checkLogin", view.getUrl());
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		assertEquals(auth.getClass(), UsernamePasswordAuthenticationToken.class);
		assertEquals("Guest1234567890", auth.getName());
	}
	
	@Test
	public void testUser() throws Exception {
		userService.setUserAuthenticated(true);
		
		request.setMethod("GET");
		request.setRequestURI("/whoami");

		handlerAdapter.handle(request, response, loginController);
		assertNotNull(response);
		assertEquals(response.getContentAsString(),"{\"username\":\"ptsr00\"}");
	}

	@Test
	public void testLogoutWithoutRedirect() throws Exception {
		request.setMethod("GET");
		request.setRequestURI("/logout");
		final ModelAndView mav = handlerAdapter.handle(request, response, loginController);
		assertNotNull(mav);
		assertNotNull(mav.getView());
		RedirectView view = (RedirectView) mav.getView();
		assertEquals("/", view.getUrl());
	}
	
	@Test
	public void testLogoutWithRedirect() throws Exception {
		request.setMethod("GET");
		request.setRequestURI("/logout");
		request.addHeader("referer", "/dojo-index.html");

		final ModelAndView mav = handlerAdapter.handle(request, response, loginController);
		assertNotNull(mav);
		assertNotNull(mav.getView());
		RedirectView view = (RedirectView) mav.getView();
		assertEquals("/dojo-index.html", view.getUrl());
	}
}
