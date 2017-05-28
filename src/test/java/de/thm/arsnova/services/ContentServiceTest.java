/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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

import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.config.TestAppConfig;
import de.thm.arsnova.config.TestSecurityConfig;
import de.thm.arsnova.dao.StubDatabaseDao;
import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.Content;
import de.thm.arsnova.exceptions.NotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AppConfig.class, TestAppConfig.class, TestSecurityConfig.class})
@ActiveProfiles("test")
public class ContentServiceTest {

	@Autowired
	private IContentService contentService;

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
		SecurityContextHolder.clearContext();
	}

	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void testShouldNotReturnQuestionsIfNotAuthenticated() {
		setAuthenticated(false, "nobody");
		contentService.getSkillQuestions("12345678");
	}

	@Test(expected = NotFoundException.class)
	public void testShouldFindQuestionsForNonExistantSession() {
		setAuthenticated(true, "ptsr00");
		contentService.getSkillQuestions("00000000");
	}

	@Test
	public void testShouldFindQuestions() {
		setAuthenticated(true, "ptsr00");
		assertEquals(1, contentService.getSkillQuestionCount("12345678"));
	}

	@Test
	public void testShouldMarkInterposedQuestionAsReadIfSessionCreator() throws Exception {
		setAuthenticated(true, "ptsr00");
		final Comment comment = new Comment();
		comment.setRead(false);
		comment.setId("the internal id");
		comment.setSessionId("12345678");
		databaseDao.comment = comment;

		contentService.readInterposedQuestion(comment.getId());

		assertTrue(comment.isRead());
	}

	@Test
	public void testShouldNotMarkInterposedQuestionAsReadIfRegularUser() throws Exception {
		setAuthenticated(true, "regular user");
		final Comment comment = new Comment();
		comment.setRead(false);
		comment.setId("the internal id");
		comment.setSessionId("12345678");
		comment.setCreator("regular user");
		databaseDao.comment = comment;

		contentService.readInterposedQuestion(comment.getId());

		assertFalse(comment.isRead());
	}

	@Test(expected = AccessDeniedException.class)
	public void testShouldSaveQuestion() throws Exception{
		setAuthenticated(true, "regular user");
		final Content content = new Content();
		content.setSessionKeyword("12345678");
		content.setQuestionVariant("freetext");
		contentService.saveQuestion(content);
	}

	@Test(expected = AccessDeniedException.class)
	public void testShouldNotDeleteQuestion() throws Exception{
		setAuthenticated(true, "otheruser");
		contentService.deleteQuestion("a1a2a3a4a5a6a7a8a9a");
	}

	@Test(expected = AccessDeniedException.class)
	public void testShouldNotDeleteInterposedQuestion() throws Exception{
		setAuthenticated(true, "otheruser");
		contentService.deleteInterposedQuestion("a1a2a3a4a5a6a7a8a9a");
	}
}
