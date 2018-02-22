/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.services;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.dao.StubDatabaseDao;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.exceptions.NotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/spring/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
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
			final List<GrantedAuthority> ga = new ArrayList<>();
			final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, "secret", ga);
			SecurityContextHolder.getContext().setAuthentication(token);
			userService.setUserAuthenticated(isAuthenticated, username);
		} else {
			userService.setUserAuthenticated(isAuthenticated);
		}
	}

	@Before
	public void startup() {
		SecurityContextHolder.clearContext();
	}

	@After
	public void cleanup() {
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
		sessionService.getSession("00000000");
	}

	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void testShouldNotReturnSessionIfUnauthorized() {
		setAuthenticated(false, null);
		sessionService.getSession("12345678");
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

		assertNull(sessionService.getSession("11111111"));
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
		assertNotNull(sessionService.getSession("11111111"));
	}

	@Test(expected = AccessDeniedException.class)
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
	@Ignore("Test fails on JDK 8 (ClassCastException)")
	public void testShouldDeleteAllSessionData() {
		/* FIXME: fails with ClassCastException on JDK 8 */
		final IDatabaseDao tempDatabase = (IDatabaseDao) ReflectionTestUtils.getField(getTargetObject(sessionService), "databaseDao");
		try {
			setAuthenticated(true, "ptsr00");

			final Session session = new Session();
			session.setKeyword("12345678");
			session.setCreator(userService.getCurrentUser().getUsername());

			final IDatabaseDao mockDatabase = mock(IDatabaseDao.class);
			when(mockDatabase.getSessionFromKeyword(anyString())).thenReturn(session);
			ReflectionTestUtils.setField(getTargetObject(sessionService), "databaseDao", mockDatabase);

			sessionService.deleteSession(session.getKeyword());

			verify(mockDatabase).deleteSession(session);
		} finally {
			ReflectionTestUtils.setField(getTargetObject(sessionService), "databaseDao", tempDatabase);
		}
	}

	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void testShouldNotDeleteSessionIfUnauthorized() {
		setAuthenticated(false, "nobody");
		sessionService.deleteSession("12345678");
	}

	@Test(expected = AccessDeniedException.class)
	public void testShouldNotDeleteSessionIfNotOwner() {
		setAuthenticated(true, "anybody");
		sessionService.deleteSession("12345678");
	}

	@Test
	public void testShouldCompareSessionByName() {
		final Session sessionA = new Session();
		sessionA.setName("TestSessionA");
		sessionA.setShortName("TSA");

		final Session sessionB = new Session();
		sessionB.setName("TestSessionB");
		sessionB.setShortName("TSB");

		final Comparator<Session> comp = new SessionService.SessionNameComparator();
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

		final Comparator<Session> comp = new SessionService.SessionShortNameComparator();
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
