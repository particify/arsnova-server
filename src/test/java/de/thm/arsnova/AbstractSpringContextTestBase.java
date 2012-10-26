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
package de.thm.arsnova;

import static org.junit.Assert.assertNotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/test/resources/test-config.xml" })
public class AbstractSpringContextTestBase extends
		AbstractJUnit4SpringContextTests {

	protected MockHttpServletRequest request;
	protected MockHttpServletResponse response;

	protected ModelAndView handle(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		final HandlerMapping handlerMapping = applicationContext
				.getBean(RequestMappingHandlerMapping.class);
		final HandlerExecutionChain handler = handlerMapping
				.getHandler(request);
		assertNotNull(
				"No handler found for request, check you request mapping",
				handler);

		final Object controller = handler.getHandler();
		// if you want to override any injected attributes do it here

		final HandlerInterceptor[] interceptors = handlerMapping.getHandler(
				request).getInterceptors();
		for (HandlerInterceptor interceptor : interceptors) {
			final boolean carryOn = interceptor.preHandle(request, response,
					controller);
			if (!carryOn) {
				return null;
			}
		}
		HandlerAdapter handlerAdapter = applicationContext
				.getBean(RequestMappingHandlerAdapter.class);
		;
		final ModelAndView mav = handlerAdapter.handle(request, response,
				controller);
		return mav;
	}
}
