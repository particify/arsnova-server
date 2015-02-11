package de.thm.arsnova.entities;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

public class QuestionTest {

	@SuppressWarnings("serial")
	@Test
	public void shouldComputeBasedOnCorrectAnswerWithExactMatch() {
		final PossibleAnswer p1 = new PossibleAnswer();
		p1.setText("Yes");
		p1.setCorrect(true);
		p1.setValue(10);
		final PossibleAnswer p2 = new PossibleAnswer();
		p2.setText("No");
		p2.setCorrect(false);
		p2.setValue(-10);
		Question q = new Question();
		q.setQuestionType("yesno");
		q.setPossibleAnswers(new ArrayList<PossibleAnswer>() {{
			add(p1);
			add(p2);
		}});
		Answer answer1 = new Answer();
		answer1.setAnswerText("Yes");
		Answer answer2 = new Answer();
		answer2.setAnswerText("No");

		assertEquals(10, q.calculateValue(answer1));
		assertEquals(-10, q.calculateValue(answer2));
	}

	@SuppressWarnings("serial")
	@Test
	public void shouldEqualAbstentionToZeroValue() {
		final PossibleAnswer p1 = new PossibleAnswer();
		p1.setText("Yes");
		p1.setCorrect(true);
		p1.setValue(10);
		final PossibleAnswer p2 = new PossibleAnswer();
		p2.setText("No");
		p2.setCorrect(false);
		p2.setValue(-10);
		Question q = new Question();
		q.setAbstention(true);
		q.setPossibleAnswers(new ArrayList<PossibleAnswer>() {{
			add(p1);
			add(p2);
		}});
		Answer answer = new Answer();
		answer.setAbstention(true);

		assertEquals(0, q.calculateValue(answer));
	}

	@SuppressWarnings("serial")
	@Test
	public void shouldCalculateMultipleChoiceAnswers() {
		final PossibleAnswer p1 = new PossibleAnswer();
		p1.setText("Yes");
		p1.setCorrect(true);
		p1.setValue(10);
		final PossibleAnswer p2 = new PossibleAnswer();
		p2.setText("No");
		p2.setCorrect(false);
		p2.setValue(-10);
		final PossibleAnswer p3 = new PossibleAnswer();
		p3.setText("Maybe");
		p3.setCorrect(true);
		p3.setValue(10);
		Question q = new Question();
		q.setQuestionType("mc");
		q.setPossibleAnswers(new ArrayList<PossibleAnswer>() {{
			add(p1);
			add(p2);
			add(p3);
		}});
		Answer answer1 = createAnswerWithText("0,0,0");
		Answer answer2 = createAnswerWithText("0,0,1");
		Answer answer3 = createAnswerWithText("0,1,0");
		Answer answer4 = createAnswerWithText("0,1,1");
		Answer answer5 = createAnswerWithText("1,0,0");
		Answer answer6 = createAnswerWithText("1,0,1");
		Answer answer7 = createAnswerWithText("1,1,0");
		Answer answer8 = createAnswerWithText("1,1,1");

		assertEquals(0, q.calculateValue(answer1));
		assertEquals(10, q.calculateValue(answer2));
		assertEquals(-10, q.calculateValue(answer3));
		assertEquals(0, q.calculateValue(answer4));
		assertEquals(10, q.calculateValue(answer5));
		assertEquals(20, q.calculateValue(answer6));
		assertEquals(0, q.calculateValue(answer7));
		assertEquals(10, q.calculateValue(answer8));
	}

	@SuppressWarnings("serial")
	@Test
	public void shouldCalculatePictureQuestionAnswers() {
		final PossibleAnswer p1 = new PossibleAnswer();
		p1.setText("0;0");
		p1.setCorrect(true);
		p1.setValue(10);
		final PossibleAnswer p2 = new PossibleAnswer();
		p2.setText("0;1");
		p2.setCorrect(false);
		p2.setValue(-10);
		final PossibleAnswer p3 = new PossibleAnswer();
		p3.setText("1;0");
		p3.setCorrect(true);
		p3.setValue(10);
		final PossibleAnswer p4 = new PossibleAnswer();
		p4.setText("1;1");
		p4.setCorrect(true);
		p4.setValue(10);
		Question q = new Question();
		q.setQuestionType("grid");
		q.setPossibleAnswers(new ArrayList<PossibleAnswer>() {{
			add(p1);
			add(p2);
			add(p3);
			add(p4);
		}});
		Answer answer1 = createAnswerWithText("0;0");
		Answer answer2 = createAnswerWithText("0;1,1;1");
		Answer answer3 = createAnswerWithText("0;0,1;0,1;1");

		assertEquals(10, q.calculateValue(answer1));
		assertEquals(0, q.calculateValue(answer2));
		assertEquals(30, q.calculateValue(answer3));
	}

	private static Answer createAnswerWithText(String text) {
		Answer answer = new Answer();
		answer.setAnswerText(text);
		return answer;
	}
}
