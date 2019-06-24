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

package de.thm.arsnova.model.migration;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.config.TestAppConfig;
import de.thm.arsnova.config.TestPersistanceConfig;
import de.thm.arsnova.config.TestSecurityConfig;
import de.thm.arsnova.config.WebSocketConfig;
import de.thm.arsnova.model.ChoiceAnswer;
import de.thm.arsnova.model.ChoiceQuestionContent;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.migration.v2.Answer;

/**
 * @author Daniel Gerhardt
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
		AppConfig.class,
		TestAppConfig.class,
		TestPersistanceConfig.class,
		TestSecurityConfig.class,
		WebSocketConfig.class})
@ActiveProfiles("test")
public class FromV2MigratorTest {
	private static final String ANSWER_ID = "answerId";
	private static final String CONTENT_ID = "contentId";
	private static final String ROOM_ID = "roomId";
	private static final Content.Format FORMAT = Content.Format.CHOICE;
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

		List<ChoiceQuestionContent.AnswerOption> options = new ArrayList<>();
		ChoiceQuestionContent.AnswerOption option1 = new ChoiceQuestionContent.AnswerOption();
		option1.setLabel(OPTION1_LABEL);
		options.add(option1);
		ChoiceQuestionContent.AnswerOption option2 = new ChoiceQuestionContent.AnswerOption();
		option2.setLabel(OPTION2_LABEL);
		options.add(option2);
		ChoiceQuestionContent.AnswerOption option3 = new ChoiceQuestionContent.AnswerOption();
		option3.setLabel(OPTION3_LABEL);
		options.add(option3);

		ChoiceQuestionContent content = new ChoiceQuestionContent();
		content.setFormat(FORMAT);
		content.setRoomId(ROOM_ID);
		content.setMultiple(true);
		content.setOptions(options);

		List<Integer> selectedChoices = new ArrayList<>();
		selectedChoices.add(1);
		selectedChoices.add(2);

		ChoiceAnswer answerV3 = (ChoiceAnswer) fromV2Migrator.migrate(answerV2, content);

		assertEquals(ANSWER_ID, answerV3.getId());
		assertEquals(CONTENT_ID, answerV3.getContentId());
		assertEquals(ROOM_ID, answerV3.getRoomId());
		assertEquals(ROUND, answerV3.getRound());
		assertEquals(selectedChoices, answerV3.getSelectedChoiceIndexes());
	}
}
