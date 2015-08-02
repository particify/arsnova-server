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

import org.junit.Before;
import org.junit.Test;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.TestUser;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.LearningProgressValues;

public class QuestionBasedLearningProgressTest {

	private CourseScore courseScore;
	private VariantLearningProgress lp;

	private int id = 1;

	private String addQuestion(String questionVariant, int points) {
		final String questionId = "question" + (id++);
		final int piRound = 1;
		courseScore.addQuestion(questionId, questionVariant, piRound, points);
		return questionId;
	}

	private void addAnswer(String questionId, User user, int points) {
		final int piRound = 1;
		courseScore.addAnswer(questionId, piRound, user.getUsername(), points);
	}

	@Before
	public void setUp() {
		this.courseScore = new CourseScore();
		IDatabaseDao db = mock(IDatabaseDao.class);
		when(db.getLearningProgress(null)).thenReturn(courseScore);
		this.lp = new QuestionBasedLearningProgress(db);
	}

	/**
	 * Questions without "correct" answers should have a value of zero
	 */
	@Test
	public void shouldIgnoreQuestionsWithoutCorrectAnswers() {
		final int questionMaxValue = 0;
		final int userScore = 0;
		User user = new TestUser("username");
		String questionId = this.addQuestion("lecture", questionMaxValue);
		this.addAnswer(questionId, user, userScore);

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
		courseScore.addQuestion("question-without-correct-answers", "lecture", 1, 0);
		courseScore.addQuestion("question-with-correct-answers", "lecture", 1, 50);
		courseScore.addAnswer("question-without-correct-answers", 1, user.getUsername(), 0);
		courseScore.addAnswer("question-with-correct-answers", 1, user.getUsername(), 50);

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
		String questionId = this.addQuestion("lecture", 10);
		for (int i = 0; i < 99; i++) {
			this.addAnswer(questionId, new TestUser("user"+i), 10);
		}
		this.addAnswer(questionId, new TestUser("user-with-a-wrong-answer"), 0);

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
		// two questions
		String q1 = this.addQuestion("lecture", 10);
		String q2 = this.addQuestion("lecture", 10);
		// two users
		User u1 = new TestUser("user1");
		User u2 = new TestUser("user2");
		// four answers, last one is wrong
		this.addAnswer(q1, u1, 10);
		this.addAnswer(q1, u2, 10);
		this.addAnswer(q2, u1, 10);
		this.addAnswer(q2, u2, 0);

		int expected = 75;
		int actual = lp.getCourseProgress(null).getCourseProgress();

		assertEquals(expected, actual);
	}

	@Test
	public void shouldNotBeBiasedByPointsOrAnswerCount() {
		// two questions
		String q1 = this.addQuestion("lecture", 1000);
		String q2 = this.addQuestion("lecture", 1);
		// first question has many answers, all of them correct
		for (int i = 0; i < 100; i++) {
			this.addAnswer(q1, new TestUser("user"+i), 1000);
		}
		// second question has one wrong answer
		this.addAnswer(q2,  new TestUser("another-user"), 0);

		int expected = 50;
		int actual = lp.getCourseProgress(null).getCourseProgress();

		assertEquals(expected, actual);
	}

	@Test
	public void shouldFilterBasedOnQuestionVariant() {
		String q1 = this.addQuestion("lecture", 100);
		String q2 = this.addQuestion("preparation", 100);
		User u1 = new TestUser("user1");
		User u2 = new TestUser("user2");
		// first question is answered correctly, second one is not
		this.addAnswer(q1, u1, 100);
		this.addAnswer(q1, u2, 100);
		this.addAnswer(q2, u1, 0);
		this.addAnswer(q2, u2, 0);

		lp.setQuestionVariant("lecture");
		LearningProgressValues lectureProgress = lp.getCourseProgress(null);
		LearningProgressValues myLectureProgress = lp.getMyProgress(null, u1);
		lp.setQuestionVariant("preparation");
		LearningProgressValues prepProgress = lp.getCourseProgress(null);
		LearningProgressValues myPrepProgress = lp.getMyProgress(null, u1);

		assertEquals(100, lectureProgress.getCourseProgress());
		assertEquals(100, myLectureProgress.getMyProgress());
		assertEquals(0, prepProgress.getCourseProgress());
		assertEquals(0, myPrepProgress.getMyProgress());
	}

	@Test
	public void shouldConsiderAnswersOfSamePiRound() {
		User u1 = new TestUser("user1");
		User u2 = new TestUser("user2");
		// question is in round 2
		courseScore.addQuestion("q1", "lecture", 2, 100);
		// 25 points in round 1, 75 points in round two for the first user
		courseScore.addAnswer("q1", 1, u1.getUsername(), 25);
		courseScore.addAnswer("q1", 2, u1.getUsername(), 100);
		// 75 points in round 1, 25 points in round two for the second user
		courseScore.addAnswer("q1", 1, u2.getUsername(), 100);
		courseScore.addAnswer("q1", 2, u2.getUsername(), 25);

		LearningProgressValues u1Progress = lp.getMyProgress(null, u1);
		LearningProgressValues u2Progress = lp.getMyProgress(null, u2);

		// only the answer for round 2 should be considered
		assertEquals(50, u1Progress.getCourseProgress());
		assertEquals(100, u1Progress.getMyProgress());
		assertEquals(50, u2Progress.getCourseProgress());
		assertEquals(0, u2Progress.getMyProgress());
	}

	@Test
	public void shouldIncludeNominatorAndDenominatorOfResultExcludingStudentCount() {
		// two questions
		String q1 = this.addQuestion("lecture", 10);
		String q2 = this.addQuestion("lecture", 10);
		// three users
		User u1 = new TestUser("user1");
		User u2 = new TestUser("user2");
		User u3 = new TestUser("user3");
		// six answers
		this.addAnswer(q1, u1, 10);
		this.addAnswer(q2, u1, -100);
		this.addAnswer(q1, u2, -100);
		this.addAnswer(q2, u2, -100);
		this.addAnswer(q1, u3, -100);
		this.addAnswer(q2, u3, -100);

		int numerator = lp.getCourseProgress(null).getNumerator();
		int denominator = lp.getCourseProgress(null).getDenominator();

		// If the percentage is wrong, then we need to adapt this test case!
		assertEquals("Precondition failed -- The underlying calculation has changed", 17, lp.getCourseProgress(null).getCourseProgress());
		assertEquals(0, numerator);
		assertEquals(2, denominator);
	}

}
