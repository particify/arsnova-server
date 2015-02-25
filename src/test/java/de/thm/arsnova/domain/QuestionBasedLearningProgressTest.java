/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
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
package de.thm.arsnova.domain;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.AbstractMap.SimpleEntry;

import org.junit.Test;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.TestUser;
import de.thm.arsnova.entities.User;

public class QuestionBasedLearningProgressTest {

	/**
	 * Questions without "correct" answers should have a value of zero
	 */
	@Test
	public void shouldIgnoreQuestionsWithoutCorrectAnswers() {
		final int questionMaxValue = 0;
		final int userScore = 0;
		User user = new TestUser("username");
		CourseScore courseScore = new CourseScore();
		courseScore.add("question-id", questionMaxValue);
		courseScore.add("question-id", user.getUsername(), userScore);

		IDatabaseDao db = mock(IDatabaseDao.class);
		when(db.getLearningProgress(null)).thenReturn(courseScore);
		LearningProgress lp = new QuestionBasedLearningProgress(db);

		SimpleEntry<Integer, Integer> expected = new SimpleEntry<Integer, Integer>(0, 0);
		SimpleEntry<Integer, Integer> actual = lp.getMyProgress(null, user);

		assertEquals(expected, actual);
	}

	@Test
	public void shouldIgnoreQuestionsWithoutCorrectAnswersInQuestionCount() {
		User user = new TestUser("username");
		CourseScore courseScore = new CourseScore();
		courseScore.add("question-without-correct-answers", 0);
		courseScore.add("question-with-correct-answers", 50);
		courseScore.add("question-without-correct-answers", user.getUsername(), 0);
		courseScore.add("question-with-correct-answers", user.getUsername(), 50);

		IDatabaseDao db = mock(IDatabaseDao.class);
		when(db.getLearningProgress(null)).thenReturn(courseScore);
		LearningProgress lp = new QuestionBasedLearningProgress(db);

		SimpleEntry<Integer, Integer> expected = new SimpleEntry<Integer, Integer>(100, 100);
		SimpleEntry<Integer, Integer> actual = lp.getMyProgress(null, user);

		assertEquals(expected, actual);
	}

}
