/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team
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
package de.thm.arsnova.entities.migration;

import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.config.TestAppConfig;
import de.thm.arsnova.config.TestPersistanceConfig;
import de.thm.arsnova.config.TestSecurityConfig;
import de.thm.arsnova.entities.ChoiceAnswer;
import de.thm.arsnova.entities.migration.v2.Answer;
import de.thm.arsnova.entities.migration.v2.AnswerOption;
import de.thm.arsnova.entities.migration.v2.Content;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Daniel Gerhardt
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AppConfig.class, TestAppConfig.class, TestPersistanceConfig.class, TestSecurityConfig.class})
@ActiveProfiles("test")
public class FromV2MigratorTest {
	private static final String ANSWER_ID = "answerId";
	private static final String CONTENT_ID = "contentId";
	private static final String ROOM_ID = "roomId";
	private static final String TYPE = "mc";
	private static final String OPTION1_LABEL = "option1";
	private static final String OPTION2_LABEL = "option2";
	private static final String OPTION3_LABEL = "option3";
	private static final int ROUND = 1;

	@Autowired
	private FromV2Migrator fromV2Migrator;

	@Test
	public void testMigrateAnswerMultipleResponse() {
		Answer answerV2 = new Answer();
		answerV2.setId(ANSWER_ID);
		answerV2.setQuestionId(CONTENT_ID);
		answerV2.setSessionId(ROOM_ID);
		answerV2.setPiRound(ROUND);
		answerV2.setAnswerText("0,1,1");

		List<AnswerOption> options = new ArrayList<>();
		AnswerOption option1 = new AnswerOption();
		option1.setText(OPTION1_LABEL);
		options.add(option1);
		AnswerOption option2 = new AnswerOption();
		option2.setText(OPTION2_LABEL);
		options.add(option2);
		AnswerOption option3 = new AnswerOption();
		option3.setText(OPTION3_LABEL);
		options.add(option3);

		Content contentV2 = new Content();
		contentV2.setQuestionType(TYPE);
		contentV2.setSessionId(ROOM_ID);
		contentV2.setPossibleAnswers(options);

		List<Integer> selectedChoices = new ArrayList<>();
		selectedChoices.add(1);
		selectedChoices.add(2);

		ChoiceAnswer answerV3 = (ChoiceAnswer) fromV2Migrator.migrate(answerV2, contentV2);

		assertEquals(ANSWER_ID, answerV3.getId());
		assertEquals(CONTENT_ID, answerV3.getContentId());
		assertEquals(ROOM_ID, answerV3.getRoomId());
		assertEquals(ROUND, answerV3.getRound());
		assertEquals(selectedChoices, answerV3.getSelectedChoiceIndexes());
	}
}
