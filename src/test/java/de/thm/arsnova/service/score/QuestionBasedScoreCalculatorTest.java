/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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

package de.thm.arsnova.service.score;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import de.thm.arsnova.model.transport.ScoreStatistics;
import de.thm.arsnova.persistence.SessionStatisticsRepository;

public class QuestionBasedScoreCalculatorTest {

	private Score courseScore;
	private VariantScoreCalculator lp;

	private int id = 1;

	private String addQuestion(String questionVariant, int points) {
		final String questionId = "question" + (id++);
		final int piRound = 1;
		courseScore.addQuestion(questionId, questionVariant, piRound, points);
		return questionId;
	}

	private void addAnswer(String questionId, String userId, int points) {
		final int piRound = 1;
		courseScore.addAnswer(questionId, piRound, userId, points);
	}

	@Before
	public void setUp() {
		this.courseScore = new Score();
		SessionStatisticsRepository db = mock(SessionStatisticsRepository.class);
		when(db.getLearningProgress(null)).thenReturn(courseScore);
		this.lp = new QuestionBasedScoreCalculator(db);
	}

	/**
	 * Questions without "correct" answers should have a value of zero
	 */
	@Test
	public void shouldIgnoreQuestionsWithoutCorrectAnswers() {
		final int questionMaxValue = 0;
		final int userScore = 0;
		String userId = "user1";
		String questionId = this.addQuestion("lecture", questionMaxValue);
		this.addAnswer(questionId, userId, userScore);

		ScoreStatistics expected = new ScoreStatistics();
		expected.setCourseProgress(0);
		expected.setMyProgress(0);
		expected.setNumQuestions(0);
		ScoreStatistics actual = lp.getMyProgress(null, userId);

		assertEquals(expected, actual);
	}

	@Test
	public void shouldIgnoreQuestionsWithoutCorrectAnswersInQuestionCount() {
		String userId = "user";
		courseScore.addQuestion("question-without-correct-answers", "lecture", 1, 0);
		courseScore.addQuestion("question-with-correct-answers", "lecture", 1, 50);
		courseScore.addAnswer("question-without-correct-answers", 1, userId, 0);
		courseScore.addAnswer("question-with-correct-answers", 1, userId, 50);

		ScoreStatistics expected = new ScoreStatistics();
		expected.setCourseProgress(100);
		expected.setMyProgress(100);
		expected.setNumQuestions(1);
		ScoreStatistics actual = lp.getMyProgress(null, userId);

		assertEquals(expected, actual);
	}

	/**
	 * If 99 users answer a question correctly, and 1 user does not, percentage should be 99%.
	 */
	@Test
	public void shouldCalculatePercentageOfOneQuestionWithSomeWrongAnswers() {
		String questionId = this.addQuestion("lecture", 10);
		for (int i = 0; i < 99; i++) {
			this.addAnswer(questionId, "user" + i, 10);
		}
		this.addAnswer(questionId, "user-with-a-wrong-answer", 0);

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
		String userId1 = "user1";
		String userId2 = "user2";
		// four answers, last one is wrong
		this.addAnswer(q1, userId1, 10);
		this.addAnswer(q1, userId2, 10);
		this.addAnswer(q2, userId1, 10);
		this.addAnswer(q2, userId2, 0);

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
			this.addAnswer(q1, "user" + i, 1000);
		}
		// second question has one wrong answer
		this.addAnswer(q2,  "another-user", 0);

		int expected = 50;
		int actual = lp.getCourseProgress(null).getCourseProgress();

		assertEquals(expected, actual);
	}

	@Test
	public void shouldFilterBasedOnQuestionVariant() {
		String q1 = this.addQuestion("lecture", 100);
		String q2 = this.addQuestion("preparation", 100);
		String userId1 = "user1";
		String userId2 = "user2";
		// first question is answered correctly, second one is not
		this.addAnswer(q1, userId1, 100);
		this.addAnswer(q1, userId2, 100);
		this.addAnswer(q2, userId1, 0);
		this.addAnswer(q2, userId2, 0);

		lp.setQuestionVariant("lecture");
		ScoreStatistics lectureProgress = lp.getCourseProgress(null);
		ScoreStatistics myLectureProgress = lp.getMyProgress(null, userId1);
		lp.setQuestionVariant("preparation");
		ScoreStatistics prepProgress = lp.getCourseProgress(null);
		ScoreStatistics myPrepProgress = lp.getMyProgress(null, userId1);

		assertEquals(100, lectureProgress.getCourseProgress());
		assertEquals(100, myLectureProgress.getMyProgress());
		assertEquals(0, prepProgress.getCourseProgress());
		assertEquals(0, myPrepProgress.getMyProgress());
	}

	@Test
	public void shouldConsiderAnswersOfSamePiRound() {
		String userId1 = "user1";
		String userId2 = "user2";
		// question is in round 2
		courseScore.addQuestion("q1", "lecture", 2, 100);
		// 25 points in round 1, 75 points in round two for the first user
		courseScore.addAnswer("q1", 1, userId1, 25);
		courseScore.addAnswer("q1", 2, userId1, 100);
		// 75 points in round 1, 25 points in round two for the second user
		courseScore.addAnswer("q1", 1, userId2, 100);
		courseScore.addAnswer("q1", 2, userId2, 25);

		ScoreStatistics u1Progress = lp.getMyProgress(null, userId1);
		ScoreStatistics u2Progress = lp.getMyProgress(null, userId2);

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
		String userId1 = "user1";
		String userId2 = "user2";
		String userId3 = "user3";
		// six answers
		this.addAnswer(q1, userId1, 10);
		this.addAnswer(q2, userId1, -100);
		this.addAnswer(q1, userId2, -100);
		this.addAnswer(q2, userId2, -100);
		this.addAnswer(q1, userId3, -100);
		this.addAnswer(q2, userId3, -100);

		int numerator = lp.getCourseProgress(null).getNumerator();
		int denominator = lp.getCourseProgress(null).getDenominator();

		// If the percentage is wrong, then we need to adapt this test case!
		assertEquals("Precondition failed -- The underlying calculation has changed",
				17, lp.getCourseProgress(null).getCourseProgress());
		assertEquals(0, numerator);
		assertEquals(2, denominator);
	}

}
