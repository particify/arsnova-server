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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.thm.arsnova.dao.StubDatabaseDao;
import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.Question;
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
public class QuestionServiceTest {

	@Autowired
	private IQuestionService questionService;

	@Autowired
	private StubUserService userService;

	@Autowired
	private StubDatabaseDao databaseDao;

	private void setAuthenticated(boolean isAuthenticated, String username) {
		if (isAuthenticated) {
			List<GrantedAuthority> ga = new ArrayList<GrantedAuthority>();
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, "secret", ga);
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
		SecurityContextHolder.clearContext();
	}

	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void testShouldNotReturnQuestionsIfNotAuthenticated() {
		setAuthenticated(false, "nobody");
		questionService.getSkillQuestions("12345678");
	}

	@Test(expected = NotFoundException.class)
	public void testShouldFindQuestionsForNonExistantSession() {
		setAuthenticated(true, "ptsr00");
		questionService.getSkillQuestions("00000000");
	}

	@Test
	public void testShouldFindQuestions() {
		setAuthenticated(true, "ptsr00");
		assertEquals(1, questionService.getSkillQuestionCount("12345678"));
	}

	@Test
	public void testShouldMarkInterposedQuestionAsReadIfSessionCreator() throws Exception {
		setAuthenticated(true, "ptsr00");
		InterposedQuestion theQ = new InterposedQuestion();
		theQ.setRead(false);
		theQ.set_id("the internal id");
		theQ.setSessionId("12345678");
		databaseDao.interposedQuestion = theQ;

		questionService.readInterposedQuestion(theQ.get_id());

		assertTrue(theQ.isRead());
	}

	@Test
	public void testShouldNotMarkInterposedQuestionAsReadIfRegularUser() throws Exception {
		setAuthenticated(true, "regular user");
		InterposedQuestion theQ = new InterposedQuestion();
		theQ.setRead(false);
		theQ.set_id("the internal id");
		theQ.setSessionId("12345678");
		databaseDao.interposedQuestion = theQ;

		questionService.readInterposedQuestion(theQ.get_id());

		assertFalse(theQ.isRead());
	}

	@Test(expected = ForbiddenException.class)
	public void testShouldSaveQuestion() throws Exception{
		setAuthenticated(true, "regular user");
		Question question = new Question();
		question.setSessionKeyword("12345678");
		question.setQuestionVariant("freetext");
		questionService.saveQuestion(question);
	}

	@Test(expected = ForbiddenException.class)
	public void testShouldDeleteQuestion() throws Exception{
		setAuthenticated(true, "otheruser");
		questionService.deleteQuestion("a1a2a3a4a5a6a7a8a9a");
	}
}
