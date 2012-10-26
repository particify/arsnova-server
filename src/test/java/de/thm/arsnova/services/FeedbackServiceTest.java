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
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/test/resources/test-config.xml"
})
public class FeedbackServiceTest {

	@Autowired
	IFeedbackService feedbackService;

	@Autowired
	StubUserService userService;

	@Test(expected = NotFoundException.class)
	public void testShouldFindFeedbackForNonExistantSession() {
		userService.setUserAuthenticated(true);
		feedbackService.getFeedback("00000000");
	}

	@Test
	public void testShouldReturnFeedback() {
		userService.setUserAuthenticated(true);
		assertNotNull(feedbackService.getFeedback("87654321"));
		assertEquals(2, (int) feedbackService.getFeedback("87654321").getValues().get(0));
		assertEquals(3, (int) feedbackService.getFeedback("87654321").getValues().get(1));
		assertEquals(5, (int) feedbackService.getFeedback("87654321").getValues().get(2));
		assertEquals(7, (int) feedbackService.getFeedback("87654321").getValues().get(3));
	}

	@Test(expected = NotFoundException.class)
	public void testShouldFindFeedbackCountForNonExistantSession() {
		userService.setUserAuthenticated(true);
		feedbackService.getFeedbackCount("00000000");
	}

	@Test
	public void testShouldReturnFeedbackCount() {
		userService.setUserAuthenticated(true);
		assertEquals(17, feedbackService.getFeedbackCount("87654321"));
	}

	@Test(expected = NotFoundException.class)
	public void testShouldFindAverageFeedbackForNonExistantSession() {
		userService.setUserAuthenticated(true);
		feedbackService.getAverageFeedback("00000000");
	}

	@Test
	public void testShouldReturnZeroFeedbackCountForNoFeedbackAtAll() {
		userService.setUserAuthenticated(true);
		assertEquals(0, feedbackService.getFeedbackCount("12345678"));
	}

	@Test(expected = NoContentException.class)
	public void testShouldReturnAverageFeedbackForNoFeedbackAtAll() {
		userService.setUserAuthenticated(true);
		feedbackService.getAverageFeedback("12345678");
	}

	@Test
	public void testShouldReturnAverageFeedbackRounded() {
		userService.setUserAuthenticated(true);
		assertEquals(2, feedbackService.getAverageFeedbackRounded("18273645"));
	}

	@Test
	public void testShouldReturnAverageFeedbackNotRounded() {
		userService.setUserAuthenticated(true);
		assertEquals(2.1904, feedbackService.getAverageFeedback("18273645"), 0.001);
	}
}