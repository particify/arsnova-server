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
package de.thm.arsnova.domain;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.*;
import de.thm.arsnova.entities.transport.LearningProgressValues;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuestionBasedLearningProgressTest {

	private QuestionBasedLearningProgress lp;

	private FixtureCreator creator;

	@Test
	public void correctAnswersShouldResultInPerfectScore() {
		creator.addQuestion("lecture").withCorrectAnswers(100);

		int expected = 100;
		int actual = lp.getCourseProgress(null).getCourseProgress();

		assertEquals(expected, actual);
	}

	@Test
	public void inCorrectAnswersShouldResultInZeroScore() {
		creator.addQuestion("lecture").withWrongAnswers(100);

		int expected = 0;
		int actual = lp.getCourseProgress(null).getCourseProgress();

		assertEquals(expected, actual);
	}

	/**
	 * If 99 users answer a question correctly, and 1 user does not, percentage should be 99%.
	 */
	@Test
	public void shouldCalculatePercentageOfOneQuestionWithSomeWrongAnswers() {
		creator.addQuestion("lecture").withCorrectAnswers(99).withWrongAnswers(1);

		int expected = 99;
		int actual = lp.getCourseProgress(null).getCourseProgress();

		assertEquals(expected, actual);
	}

	/**
	 * Given two users and two questions: the first question is answered correctly by both users, while the second
	 * is only answered correctly by one user. The first question should receive 100%, the second 50%. This should
	 * result in an overall score of 75%.
	 *
	 * Formula:		(Correct Answers) / (Questions * Users)
	 * 	==> 3/(2*2)
	 * 	==> 3/4
	 * 	==> 75%
	 */
	@Test
	public void shouldCalculatePercentageOfMultipleQuestionsAndAnswers() {
		creator.addQuestion("lecture").withCorrectAnswers(2).forUsers("user1", "user2");
		creator.addQuestion("lecture")
				.withCorrectAnswers(1).forUser("user1")
				.withWrongAnswers(1).forUser("user2");

		int expected = 75;
		int actual = lp.getCourseProgress(null).getCourseProgress();

		assertEquals(expected, actual);
	}

	@Test
	public void shouldNotBeBiasedByAnswerCount() {
		// (999+1) correct answers / (2 questions * 1000 users) ==> 1000/2000 ==> 50%
		creator.addQuestion("lecture").withCorrectAnswers(999).withCorrectAnswers(1).forUser("user1");
		creator.addQuestion("lecture").withWrongAnswers(1).forUser("user1");

		int expected = 50;
		int actual = lp.getCourseProgress(null).getCourseProgress();

		assertEquals(expected, actual);
	}

	@Test
	public void shouldFilterBasedOnQuestionVariant() {
		creator.addQuestion("lecture").withCorrectAnswers(2).forUsers("user1", "user2");
		creator.addQuestion("preparation").withWrongAnswers(2).forUsers("user1", "user2");

		lp.setQuestionVariant("lecture");
		LearningProgressValues lectureProgress = lp.getCourseProgress(null);
		LearningProgressValues myLectureProgress = lp.getMyProgress(null, new TestUser("user1"));
		lp.setQuestionVariant("preparation");
		LearningProgressValues prepProgress = lp.getCourseProgress(null);
		LearningProgressValues myPrepProgress = lp.getMyProgress(null, new TestUser("user1"));
		lp.setQuestionVariant("");
		LearningProgressValues allProgress = lp.getCourseProgress(null);
		LearningProgressValues allMyProgress = lp.getMyProgress(null, new TestUser("user1"));

		assertEquals(100, lectureProgress.getCourseProgress());
		assertEquals(100, myLectureProgress.getMyProgress());
		assertEquals(0, prepProgress.getCourseProgress());
		assertEquals(0, myPrepProgress.getMyProgress());
		assertEquals(50, allProgress.getCourseProgress());
		assertEquals(50, allMyProgress.getMyProgress());
	}

	@Test
	public void shouldConsiderOnlyAnswersForCurrentPiRound() {
		creator.addQuestion("lecture").forRound(2)
				.withWrongAnswers(1).inRound(1).forUser("user1")
				.withCorrectAnswers(1).inRound(1).forUser("user2")
				.withCorrectAnswers(1).inRound(2).forUser("user1")
				.withWrongAnswers(1).inRound(2).forUser("user2");

		LearningProgressValues u1Progress = lp.getMyProgress(null, new TestUser("user1"));
		LearningProgressValues u2Progress = lp.getMyProgress(null, new TestUser("user2"));

		// only the answer for round 2 should be considered
		assertEquals(50, u1Progress.getCourseProgress());
		assertEquals(100, u1Progress.getMyProgress());
		assertEquals(50, u2Progress.getCourseProgress());
		assertEquals(0, u2Progress.getMyProgress());
	}

	@Test
	public void shouldIncludeInactiveQuestionsWithAnswers() {
		creator.addQuestion("lecture").setInactive().withCorrectAnswers(1);

		int expected = 100;
		int actual = lp.getCourseProgress(null).getCourseProgress();
		assertEquals(expected, actual);
	}

	@Test
	public void shouldIgnoreInactiveQuestionsWithoutAnswers() {
		creator.addQuestion("lecture").setInactive();

		int expected = 0;
		int actual = lp.getCourseProgress(null).getCourseProgress();
		assertEquals(expected, actual);
	}

	@Test
	public void shouldIgnoreQuestionsWithoutCorrectAnswers() {
		creator.addQuestion("lecture").setNoCorrect().withAnswers(1);

		LearningProgressValues expected = new LearningProgressValues();
		expected.setCourseProgress(0);
		expected.setMyProgress(0);
		expected.setNumQuestions(0);
		LearningProgressValues actual = lp.getMyProgress(null, new TestUser("TestUser"));

		assertEquals(expected, actual);
	}

	@Test
	public void shouldIgnoreQuestionsWithoutPossibleAnswers() {
		creator.addQuestion("lecture").withoutPossibleAnswers(); // this question should get ignored!
		creator.addQuestion("lecture").withCorrectAnswers(1);

		int expected = 100;
		int actual = lp.getCourseProgress(null).getCourseProgress();
		assertEquals(expected, actual);
	}

	@Before
	public void setUp() {
		this.creator = new FixtureCreator();
		IDatabaseDao db = mock(IDatabaseDao.class);
		when(db.getLectureQuestionsForTeachers(any(Session.class))).then(invocationOnMock -> this.creator.returnLectureQuestions());
		when(db.getPreparationQuestionsForTeachers(any(Session.class))).then(invocationOnMock -> this.creator.returnPreparationQuestions());
		when(db.getAnswerTextAndUser(any(Question.class), anyInt())).then(invocationOnMock -> this.creator.returnAnswersForQuestion(invocationOnMock.getArgument(0)));
		this.lp = new QuestionBasedLearningProgress(db);
		this.lp.setQuestionVariant("lecture");
	}

	private static class FixtureCreator {
		List<Question> questions = new ArrayList<>();
		Map<Integer, List<Answer>> answersForQuestion = new HashMap<>();
		private int questionId = 0;
		private int last_id = 0;

		public FixtureCreator addQuestion(String variant) {
			last_id = questionId;
			Question q = createVariantQuestion(variant);
			questions.add(q);
			return this;
		}

		private Question createVariantQuestion(String variant) {
			Question q = new Question();
			q.set_id(""+(questionId++));
			q.setQuestionType("sc");
			q.setQuestionVariant(variant);
			q.setActive(true);
			q.setNoCorrect(false);
			q.setPossibleAnswers(new ArrayList<>());
			PossibleAnswer pa1 = new PossibleAnswer();
			pa1.setText("a correct answer");
			pa1.setCorrect(true);
			q.getPossibleAnswers().add(pa1);
			PossibleAnswer pa2 = new PossibleAnswer();
			pa2.setText("a wrong answer");
			pa2.setCorrect(false);
			q.getPossibleAnswers().add(pa2);
			return q;
		}

		private Question getLastQuestion() {
			return questions.get(last_id);
		}

		public FixtureCreator setInactive() {
			getLastQuestion().setActive(false);
			return this;
		}

		public FixtureCreator setNoCorrect() {
			Question q = getLastQuestion();
			q.setNoCorrect(true);
			for (PossibleAnswer pa : q.getPossibleAnswers()) {
				pa.setCorrect(false);
			}
			return this;
		}

		public FixtureCreator withoutPossibleAnswers() {
			Question q = getLastQuestion();
			q.setPossibleAnswers(new ArrayList<>());
			return this;
		}

		public FixtureCreator withAnswers(int count) {
			List<Answer> answers = answersForQuestion.get(last_id);
			if (answers == null) answers = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				Answer a = new Answer();
				a.setQuestionId("" + last_id);
				a.setAnswerText("a random answer");
				a.setUser(UUID.randomUUID().toString());
				answers.add(a);
			}
			answersForQuestion.put(last_id, answers);
			return this;
		}

		public FixtureCreator withWrongAnswers(int count) {
			List<Answer> answers = answersForQuestion.get(last_id);
			if (answers == null) answers = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				Answer a = new Answer();
				a.setQuestionId("" + last_id);
				a.setAnswerText("a wrong answer");
				a.setUser(UUID.randomUUID().toString());
				answers.add(a);
			}
			answersForQuestion.put(last_id, answers);
			return this;
		}

		public FixtureCreator withCorrectAnswers(int count) {
			List<Answer> answers = answersForQuestion.get(last_id);
			if (answers == null) answers = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				Answer a = new Answer();
				a.setQuestionId("" + last_id);
				a.setAnswerText("a correct answer");
				a.setUser(UUID.randomUUID().toString());
				answers.add(a);
			}
			answersForQuestion.put(last_id, answers);
			return this;
		}

		public FixtureCreator forUsers(String... users) {
			List<Answer> answers = answersForQuestion.get(last_id);
			Iterator<String> userIter = Arrays.asList(users).iterator();
			for (Answer a : answers) {
				a.setUser(userIter.next());
			}
			return this;
		}

		public FixtureCreator forUser(String user) {
			Answer a = getLastAnswer();
			a.setUser(user);
			return this;
		}

		private Answer getLastAnswer() {
			List<Answer> answers = answersForQuestion.get(last_id);
			return answers.get(answers.size()-1);
		}

		public FixtureCreator forRound(int round) {
			Question q = getLastQuestion();
			q.setPiRound(round);
			return this;
		}

		public FixtureCreator inRound(int round) {
			Answer a = getLastAnswer();
			a.setPiRound(round);
			return this;
		}

		public List<Answer> returnAnswersForQuestion(Question q) {
			List<Answer> answers = this.answersForQuestion.get(Integer.parseInt(q.get_id()));
			if (answers == null) return new ArrayList<>();

			return answers.stream().filter(a -> a.getPiRound() == q.getPiRound()).collect(Collectors.toList());
		}

		public List<Question> returnLectureQuestions() {
			return this.questions.stream().filter(q -> q.getQuestionVariant().equals("lecture")).collect(Collectors.toList());
		}

		public List<Question> returnPreparationQuestions() {
			return this.questions.stream().filter(q -> q.getQuestionVariant().equals("preparation")).collect(Collectors.toList());
		}
	}
}
