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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.dao.StubDatabaseDao;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NotFoundException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/spring/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-security.xml",
		"file:src/test/resources/test-config.xml"
})
@ActiveProfiles("test")
public class SessionServiceTest {

	@Autowired
	private ISessionService sessionService;

	@Autowired
	private StubUserService userService;

	@Autowired
	private StubDatabaseDao databaseDao;

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
	public final void startup() {
		SecurityContextHolder.clearContext();
	}

	@After
	public final void cleanup() {
		databaseDao.cleanupTestData();
		SecurityContextHolder.clearContext();
		userService.setUserAuthenticated(false);
	}

	@Test
	public void testShouldGenerateSessionKeyword() {
		assertTrue(sessionService.generateKeyword().matches("^[0-9]{8}$"));
	}

	@Test(expected = NotFoundException.class)
	public void testShouldNotFindNonExistantSession() {
		setAuthenticated(true, "ptsr00");
		sessionService.joinSession("00000000");
	}

	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void testShouldNotReturnSessionIfUnauthorized() {
		setAuthenticated(false, null);
		sessionService.joinSession("12345678");
	}

	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void testShouldNotSaveSessionIfUnauthorized() {
		setAuthenticated(false, null);

		final Session session = new Session();
		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("11111111");
		session.setName("TestSessionX");
		session.setShortName("TSX");
		sessionService.saveSession(session);

		setAuthenticated(true, "ptsr00");

		assertNull(sessionService.joinSession("11111111"));
	}

	@Test
	public void testShouldSaveSession() {
		setAuthenticated(true, "ptsr00");

		final Session session = new Session();
		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("11111111");
		session.setName("TestSessionX");
		session.setShortName("TSX");
		sessionService.saveSession(session);
		assertNotNull(sessionService.joinSession("11111111"));
	}

	@Test(expected = ForbiddenException.class)
	public void testShouldUpdateSession() {
		setAuthenticated(true, "ptsr00");

		final Session session = new Session();
		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("11111111");
		session.setName("TestSessionX");
		session.setShortName("TSX");
		sessionService.saveSession(session);

		setAuthenticated(true, "other");
		sessionService.updateSession(session.getKeyword(), session);
	}

	@Test
	public void testShouldDeleteAllSessionData() {
		final IDatabaseDao tempDatabase = (IDatabaseDao) ReflectionTestUtils.getField(getTargetObject(sessionService), "databaseDao");
		try {
			setAuthenticated(true, "ptsr00");

			final Session session = new Session();
			session.setCreator(userService.getCurrentUser().getUsername());
			final Question q1 = new Question();
			final Question q2 = new Question();

			final IDatabaseDao mockDatabase = mock(IDatabaseDao.class);
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

	@Test
	public void testShouldCompareSessionByName() {
		final Session sessionA = new Session();
		sessionA.setName("TestSessionA");
		sessionA.setShortName("TSA");

		final Session sessionB = new Session();
		sessionB.setName("TestSessionB");
		sessionB.setShortName("TSB");

		final Comparator<Session> comp = new SessionService.SessionNameComperator();
		assertTrue(comp.compare(sessionA, sessionB) < 0);
	}

	@Test
	public void testShouldCompareSessionByShortName() {
		final Session sessionA = new Session();
		sessionA.setName("TestSessionA");
		sessionA.setShortName("TSA");

		final Session sessionB = new Session();
		sessionB.setName("TestSessionB");
		sessionB.setShortName("TSB");

		final Comparator<Session> comp = new SessionService.SessionShortNameComperator();
		assertTrue(comp.compare(sessionA, sessionB) < 0);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getTargetObject(final Object proxy) {
		if (AopUtils.isJdkDynamicProxy(proxy)) {
			try {
				return (T) getTargetObject(((Advised) proxy).getTargetSource().getTarget());
			} catch (final Exception e) {
				throw new RuntimeException("Failed to unproxy target.", e);
			}
		}
		return (T) proxy;
	}
}