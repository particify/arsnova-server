package de.thm.arsnova.entities;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AnswerTest {

	@Test
	public void testIsCorrectForSingleChoiceQuestionTypes() {
		Question sc = createQuestion("sc", 1);
		Question yesno = createQuestion("yesno", 0);

		Answer scAnswer1 = createAnswer(sc.getPossibleAnswers().get(0).getText());
		Answer scAnswer2 = createAnswer(sc.getPossibleAnswers().get(1).getText());
		Answer ynAnswer1 = createAnswer(yesno.getPossibleAnswers().get(0).getText());
		Answer ynAnswer2 = createAnswer(yesno.getPossibleAnswers().get(1).getText());

		assertFalse(scAnswer1.isCorrect(sc));
		assertTrue(scAnswer2.isCorrect(sc));
		assertTrue(ynAnswer1.isCorrect(yesno));
		assertFalse(ynAnswer2.isCorrect(yesno));
	}

	@Test
	public void testIsCorrectForMultipleChoice() {
		Question q = createQuestion("mc");
		// Set all answer options to "correct". An answer is considered "correct", if
		// all "correct" possible answers are selected.
		q.getPossibleAnswers().stream().forEach(pa -> pa.setCorrect(true));

		Answer incorrect1 = createAnswer("0,0");
		Answer incorrect2 = createAnswer("0,1");
		Answer incorrect3 = createAnswer("1,0");
		Answer correct = createAnswer("1,1");

		assertFalse(incorrect1.isCorrect(q));
		assertFalse(incorrect2.isCorrect(q));
		assertFalse(incorrect3.isCorrect(q));
		assertTrue(correct.isCorrect(q));
	}

	@Test
	public void testIsCorrectForGridQuestion() {
		Question q = new Question();
		q.setQuestionType("grid");
		// create a 2x2 grid, first row of fields are correct
		List<PossibleAnswer> pas = new ArrayList<>();
		for (int y = 0; y < 2; y++) {
			for (int x = 0; x < 2; x++) {
				PossibleAnswer pa = new PossibleAnswer();
				pa.setText(y + ";" + x);
				pa.setCorrect(y == 0);
				pas.add(pa);
			}
		}
		q.setPossibleAnswers(pas);

		Answer incorrect1 = createAnswer("");
		Answer incorrect2 = createAnswer("0;0");
		Answer incorrect3 = createAnswer("0;1");
		Answer incorrect4 = createAnswer("1;0");
		Answer incorrect5 = createAnswer("1;1");
		Answer correct = createAnswer("0;0,0;1");
		Answer incorrect6 = createAnswer("0;0,1;0");
		Answer incorrect7 = createAnswer("0;0,1;1");
		Answer incorrect8 = createAnswer("0;1,1;0");
		Answer incorrect9 = createAnswer("0;1,1;1");
		Answer incorrect10 = createAnswer("1;0,1;1");
		Answer incorrect11 = createAnswer("0;0,0;1,1;0");
		Answer incorrect12 = createAnswer("0;0,0;1,1;1");
		Answer incorrect13 = createAnswer("0;0,1;0,1;1");
		Answer incorrect14 = createAnswer("0;0,0;1,1;0,1;1");

		assertFalse(incorrect1.isCorrect(q));
		assertFalse(incorrect2.isCorrect(q));
		assertFalse(incorrect3.isCorrect(q));
		assertFalse(incorrect4.isCorrect(q));
		assertFalse(incorrect5.isCorrect(q));
		assertFalse(incorrect6.isCorrect(q));
		assertFalse(incorrect7.isCorrect(q));
		assertFalse(incorrect8.isCorrect(q));
		assertFalse(incorrect9.isCorrect(q));
		assertFalse(incorrect10.isCorrect(q));
		assertFalse(incorrect11.isCorrect(q));
		assertFalse(incorrect12.isCorrect(q));
		assertFalse(incorrect13.isCorrect(q));
		assertFalse(incorrect14.isCorrect(q));
		assertTrue(correct.isCorrect(q));
	}

	private Answer createAnswer(String answerText) {
		Answer a = new Answer();
		a.setAnswerText(answerText);
		return a;
	}

	private Question createQuestion(String questionType) {
		return createQuestion(questionType, -1);
	}

	private Question createQuestion(String questionType, int correctAnswerIndex) {
		Question q = new Question();
		q.setQuestionType(questionType);
		List<PossibleAnswer> pas = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			PossibleAnswer pa = new PossibleAnswer();
			pa.setText("Answer Option " + (i+1));
			pa.setCorrect(i == correctAnswerIndex);
			pas.add(pa);
		}
		q.setPossibleAnswers(pas);
		return q;
	}
}
