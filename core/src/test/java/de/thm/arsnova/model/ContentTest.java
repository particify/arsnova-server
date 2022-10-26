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

package de.thm.arsnova.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import de.thm.arsnova.model.migration.v2.Answer;
import de.thm.arsnova.model.migration.v2.AnswerOption;
import de.thm.arsnova.model.migration.v2.Content;

public class ContentTest {

	@SuppressWarnings("serial")
	@Test
	public void shouldComputeBasedOnCorrectAnswerWithExactMatch() {
		final AnswerOption p1 = new AnswerOption();
		p1.setText("Yes");
		p1.setCorrect(true);
		p1.setValue(10);
		final AnswerOption p2 = new AnswerOption();
		p2.setText("No");
		p2.setCorrect(false);
		p2.setValue(-10);
		final Content q = new Content();
		q.setQuestionType("yesno");
		q.setPossibleAnswers(new ArrayList<AnswerOption>() {{
			add(p1);
			add(p2);
		}});
		final Answer answer1 = new Answer();
		answer1.setAnswerText("Yes");
		final Answer answer2 = new Answer();
		answer2.setAnswerText("No");

		assertEquals(10, q.calculateValue(answer1));
		assertEquals(-10, q.calculateValue(answer2));
	}

	@SuppressWarnings("serial")
	@Test
	public void shouldEqualAbstentionToZeroValue() {
		final AnswerOption p1 = new AnswerOption();
		p1.setText("Yes");
		p1.setCorrect(true);
		p1.setValue(10);
		final AnswerOption p2 = new AnswerOption();
		p2.setText("No");
		p2.setCorrect(false);
		p2.setValue(-10);
		final Content q = new Content();
		q.setAbstention(true);
		q.setPossibleAnswers(new ArrayList<AnswerOption>() {{
			add(p1);
			add(p2);
		}});
		final Answer answer = new Answer();
		answer.setAbstention(true);

		assertEquals(0, q.calculateValue(answer));
	}

	@SuppressWarnings("serial")
	@Test
	public void shouldCalculateMultipleChoiceAnswers() {
		final AnswerOption p1 = new AnswerOption();
		p1.setText("Yes");
		p1.setCorrect(true);
		p1.setValue(10);
		final AnswerOption p2 = new AnswerOption();
		p2.setText("No");
		p2.setCorrect(false);
		p2.setValue(-10);
		final AnswerOption p3 = new AnswerOption();
		p3.setText("Maybe");
		p3.setCorrect(true);
		p3.setValue(10);
		final Content q = new Content();
		q.setQuestionType("mc");
		q.setPossibleAnswers(new ArrayList<AnswerOption>() {{
			add(p1);
			add(p2);
			add(p3);
		}});
		final Answer answer1 = createAnswerWithText("0,0,0");
		final Answer answer2 = createAnswerWithText("0,0,1");
		final Answer answer3 = createAnswerWithText("0,1,0");
		final Answer answer4 = createAnswerWithText("0,1,1");
		final Answer answer5 = createAnswerWithText("1,0,0");
		final Answer answer6 = createAnswerWithText("1,0,1");
		final Answer answer7 = createAnswerWithText("1,1,0");
		final Answer answer8 = createAnswerWithText("1,1,1");

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
		final AnswerOption p1 = new AnswerOption();
		p1.setText("0;0");
		p1.setCorrect(true);
		p1.setValue(10);
		final AnswerOption p2 = new AnswerOption();
		p2.setText("0;1");
		p2.setCorrect(false);
		p2.setValue(-10);
		final AnswerOption p3 = new AnswerOption();
		p3.setText("1;0");
		p3.setCorrect(true);
		p3.setValue(10);
		final AnswerOption p4 = new AnswerOption();
		p4.setText("1;1");
		p4.setCorrect(true);
		p4.setValue(10);
		final Content q = new Content();
		q.setQuestionType("grid");
		q.setPossibleAnswers(new ArrayList<AnswerOption>() {{
			add(p1);
			add(p2);
			add(p3);
			add(p4);
		}});
		final Answer answer1 = createAnswerWithText("0;0");
		final Answer answer2 = createAnswerWithText("0;1,1;1");
		final Answer answer3 = createAnswerWithText("0;0,1;0,1;1");

		assertEquals(10, q.calculateValue(answer1));
		assertEquals(0, q.calculateValue(answer2));
		assertEquals(30, q.calculateValue(answer3));
	}

	private static Answer createAnswerWithText(final String text) {
		final Answer answer = new Answer();
		answer.setAnswerText(text);
		return answer;
	}
}
