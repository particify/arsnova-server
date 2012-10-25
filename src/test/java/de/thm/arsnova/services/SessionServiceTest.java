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
package de.thm.arsnova.services;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/test/resources/test-config.xml" })
public class SessionServiceTest {

	@Autowired
	ISessionService sessionService;

	@Autowired
	StubUserService userService;

	@Test
	public void testShouldGenerateSessionKeyword() {
		System.out.println(sessionService.generateKeyword());
		assertTrue(sessionService.generateKeyword().matches("^[0-9]{8}$"));
	}

	@Test(expected = NotFoundException.class)
	public void testShouldFindNonExistantSession() {
		userService.setUserAuthenticated(true);
		sessionService.joinSession("00000000");
	}

	@Test(expected = UnauthorizedException.class)
	public void testShouldNotReturnSessionIfUnauthorized() {
		userService.setUserAuthenticated(false);
		sessionService.joinSession("12345678");
	}

	@Test(expected = UnauthorizedException.class)
	public void testShouldNotSaveSessionIfUnauthorized() {
		userService.setUserAuthenticated(false);

		Session session = new Session();
		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("11111111");
		session.setName("TestSessionX");
		session.setShortName("TSX");
		sessionService.saveSession(session);

		userService.setUserAuthenticated(true);

		assertNull(sessionService.joinSession("11111111"));
	}

	@Test
	public void testShouldSaveSession() {
		userService.setUserAuthenticated(true);

		Session session = new Session();
		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("11111111");
		session.setName("TestSessionX");
		session.setShortName("TSX");
		sessionService.saveSession(session);
		assertNotNull(sessionService.joinSession("11111111"));
	}
}