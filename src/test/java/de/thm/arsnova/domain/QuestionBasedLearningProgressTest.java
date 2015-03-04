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

import org.junit.Test;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.TestUser;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.LearningProgressValues;

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

		LearningProgressValues expected = new LearningProgressValues();
		expected.setCourseProgress(0);
		expected.setMyProgress(0);
		expected.setNumQuestions(0);
		LearningProgressValues actual = lp.getMyProgress(null, user);

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

		LearningProgressValues expected = new LearningProgressValues();
		expected.setCourseProgress(100);
		expected.setMyProgress(100);
		expected.setNumQuestions(1);
		LearningProgressValues actual = lp.getMyProgress(null, user);

		assertEquals(expected, actual);
	}

	/**
	 * If 99 users answer a question correctly, and 1 user does not, percentage should be 99%.
	 */
	@Test
	public void shouldCalculatePercentageOfOneQuestionWithSomeWrongAnswers() {
		CourseScore courseScore = new CourseScore();
		courseScore.add("question", 10);
		for (int i = 0; i < 99; i++) {
			courseScore.add("question", new TestUser("user"+i).getUsername(), 10);
		}
		courseScore.add("question", new TestUser("user-with-a-wrong-answer").getUsername(), 0);

		IDatabaseDao db = mock(IDatabaseDao.class);
		when(db.getLearningProgress(null)).thenReturn(courseScore);
		LearningProgress lp = new QuestionBasedLearningProgress(db);

		int expected = 99;
		int actual = lp.getCourseProgress(null).getCourseProgress();

		assertEquals(expected, actual);
	}

	/**
	 * Given two users and two questions: the first question is answered correctly by both users, while the second
	 * is only answered correctly by one user. The first question should receive 100%, the second 50%. This should
	 * result in an overall score of 75%.
	 */
	@Test
	public void shouldCalculatePercentageOfMultipleQuestionsAndAnswers() {
		CourseScore courseScore = new CourseScore();
		// two questions
		courseScore.add("question1", 10);
		courseScore.add("question2", 10);
		// two users
		User u1 = new TestUser("user1");
		User u2 = new TestUser("user2");
		// four answers, last one is wrong
		courseScore.add("question1", u1.getUsername(), 10);
		courseScore.add("question1", u2.getUsername(), 10);
		courseScore.add("question2", u1.getUsername(), 10);
		courseScore.add("question2", u2.getUsername(), 0);

		IDatabaseDao db = mock(IDatabaseDao.class);
		when(db.getLearningProgress(null)).thenReturn(courseScore);
		LearningProgress lp = new QuestionBasedLearningProgress(db);

		int expected = 75;
		int actual = lp.getCourseProgress(null).getCourseProgress();

		assertEquals(expected, actual);
	}

	@Test
	public void shouldNotBeBiasedByPointsOrAnswerCount() {
		CourseScore courseScore = new CourseScore();
		// two questions
		courseScore.add("question1", 1000);
		courseScore.add("question2", 1);
		// first question has many answers, all of them correct
		for (int i = 0; i < 100; i++) {
			courseScore.add("question1", new TestUser("user"+i).getUsername(), 1000);
		}
		// second question has one wrong answer
		courseScore.add("question2",  new TestUser("another-user").getUsername(), 0);

		IDatabaseDao db = mock(IDatabaseDao.class);
		when(db.getLearningProgress(null)).thenReturn(courseScore);
		LearningProgress lp = new QuestionBasedLearningProgress(db);

		int expected = 50;
		int actual = lp.getCourseProgress(null).getCourseProgress();

		assertEquals(expected, actual);
	}

}
