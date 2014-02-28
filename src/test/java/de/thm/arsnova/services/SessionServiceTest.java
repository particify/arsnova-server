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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import static org.mockito.Mockito.*;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.dao.StubDatabaseDao;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/spring/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/test/resources/test-config.xml" })
public class SessionServiceTest {

	@Autowired
	private ISessionService sessionService;

	@Autowired
	private StubUserService userService;
	
	@Autowired
	private StubDatabaseDao databaseDao;

	@After
	public final void cleanup() {
		databaseDao.cleanupTestData();
		userService.setUserAuthenticated(false);
	}
	
	@Test
	public void testShouldGenerateSessionKeyword() {
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

	@Test
	public void testShouldDeleteAllSessionData() {
		IDatabaseDao tempDatabase = (IDatabaseDao) ReflectionTestUtils.getField(getTargetObject(sessionService), "databaseDao");
		try {
			userService.setUserAuthenticated(true);
	
			Session session = new Session();
			session.setCreator(userService.getCurrentUser().getUsername());
			Question q1 = new Question();
			Question q2 = new Question();
	
			IDatabaseDao mockDatabase = mock(IDatabaseDao.class);
			when(mockDatabase.getSkillQuestions(userService.getCurrentUser(), session)).thenReturn(Arrays.asList(q1, q2));
			when(mockDatabase.getSession(anyString())).thenReturn(session);
			ReflectionTestUtils.setField(getTargetObject(sessionService), "databaseDao", mockDatabase);
	
			sessionService.deleteSession(session.getKeyword(), userService.getCurrentUser());
	
			verify(mockDatabase).deleteQuestionWithAnswers(q1);
			verify(mockDatabase).deleteQuestionWithAnswers(q2);
			verify(mockDatabase).deleteSession(session);
		} finally {
			ReflectionTestUtils.setField(getTargetObject(sessionService), "databaseDao", tempDatabase);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T getTargetObject(Object proxy) {
		if ((AopUtils.isJdkDynamicProxy(proxy))) {
			try {
				return (T) getTargetObject(((Advised) proxy).getTargetSource().getTarget());
			} catch (Exception e) {
				throw new RuntimeException("Failed to unproxy target.", e);
			}
		}
		return (T) proxy;
	}
}