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
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import de.thm.arsnova.AbstractSpringContextTestBase;
import de.thm.arsnova.entities.User;

public class LoginControllerTest extends AbstractSpringContextTestBase {

	@Before
	public void setUp() throws Exception {
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
	}

	@Test
	public void testGuestLogin() throws Exception {
		request.setMethod("GET");
		request.setRequestURI("/doLogin");
		request.addParameter("type", "guest");

		final ModelAndView mav = handle(request, response);

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

		final ModelAndView mav = handle(request, response);

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
		request.setMethod("GET");
		request.setRequestURI("/whoami");

		final ModelAndView mav = handle(request, response);

		assertNotNull(mav);
		assertTrue(mav.getModel().containsKey("user"));
		assertEquals(mav.getModel().get("user").getClass(), User.class);
		assertEquals("Guest1234567890", ((User)mav.getModel().get("user")).getUsername());
	}

}
