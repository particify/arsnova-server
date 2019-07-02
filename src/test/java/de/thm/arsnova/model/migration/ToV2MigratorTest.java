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
import java.util.Arrays;
import java.util.Collections;
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
import de.thm.arsnova.model.AnswerStatistics;
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
public class ToV2MigratorTest {
	private static final String ANSWER_ID = "answerId";
	private static final String CONTENT_ID = "contentId";
	private static final String ROOM_ID = "roomId";
	private static final String CREATOR_ID = "creatorId";
	private static final String OPTION1_LABEL = "option1";
	private static final String OPTION2_LABEL = "option2";
	private static final String OPTION3_LABEL = "option3";
	private static final String OPTION4_LABEL = "option4";
	private static final int ROUND = 1;
	private static final int ABSTENTION_COUNT = 7;
	private static final List<Integer> ANSWER_COUNTS =
			Collections.unmodifiableList(Arrays.asList(new Integer[] {3, 2, 4, 1}));

	@Autowired
	private ToV2Migrator toV2Migrator;

	@Test
	public void testMigrateAnswerMultipleResponse() {
		final List<ChoiceQuestionContent.AnswerOption> options = new ArrayList<>();
		final ChoiceQuestionContent.AnswerOption option1 = new ChoiceQuestionContent.AnswerOption();
		option1.setLabel(OPTION1_LABEL);
		options.add(option1);
		final ChoiceQuestionContent.AnswerOption option2 = new ChoiceQuestionContent.AnswerOption();
		option2.setLabel(OPTION2_LABEL);
		options.add(option2);
		final ChoiceQuestionContent.AnswerOption option3 = new ChoiceQuestionContent.AnswerOption();
		option3.setLabel(OPTION3_LABEL);
		options.add(option3);

		final ChoiceQuestionContent contentV3 = new ChoiceQuestionContent();
		contentV3.setFormat(Content.Format.CHOICE);
		contentV3.setMultiple(true);
		contentV3.setRoomId(ROOM_ID);
		contentV3.setOptions(options);

		final List<Integer> selectedChoices = new ArrayList<>();
		selectedChoices.add(1);
		selectedChoices.add(2);

		final ChoiceAnswer answerV3 = new ChoiceAnswer();
		answerV3.setId(ANSWER_ID);
		answerV3.setCreatorId(CREATOR_ID);
		answerV3.setRoomId(ROOM_ID);
		answerV3.setContentId(CONTENT_ID);
		answerV3.setRound(ROUND);
		answerV3.setSelectedChoiceIndexes(selectedChoices);

		final Answer answerV2 = toV2Migrator.migrate(answerV3, contentV3);

		assertEquals(ANSWER_ID, answerV2.getId());
		assertEquals(CONTENT_ID, answerV2.getQuestionId());
		assertEquals(ROOM_ID, answerV2.getSessionId());
		assertEquals(ROUND, answerV2.getPiRound());
		assertEquals("0,1,1", answerV2.getAnswerText());
	}

	@Test
	public void testMigrateAnswerStatisticsSingleChoice() {
		final AnswerStatistics statsV3 = new AnswerStatistics();
		final AnswerStatistics.RoundStatistics roundStatsV3 = new AnswerStatistics.RoundStatistics();
		roundStatsV3.setRound(ROUND);
		roundStatsV3.setIndependentCounts(ANSWER_COUNTS);
		roundStatsV3.setAbstentionCount(7);
		statsV3.setRoundStatistics(Collections.singletonList(roundStatsV3));

		final ChoiceQuestionContent.AnswerOption option1 = new ChoiceQuestionContent.AnswerOption();
		option1.setLabel(OPTION1_LABEL);
		final ChoiceQuestionContent.AnswerOption option2 = new ChoiceQuestionContent.AnswerOption();
		option2.setLabel(OPTION2_LABEL);
		final ChoiceQuestionContent.AnswerOption option3 = new ChoiceQuestionContent.AnswerOption();
		option3.setLabel(OPTION3_LABEL);
		final ChoiceQuestionContent.AnswerOption option4 = new ChoiceQuestionContent.AnswerOption();
		option3.setLabel(OPTION4_LABEL);
		final List<ChoiceQuestionContent.AnswerOption> options = Arrays.asList(new ChoiceQuestionContent.AnswerOption[] {
			option1, option2, option3, option4
		});

		final ChoiceQuestionContent contentV3 = new ChoiceQuestionContent();
		contentV3.getState().setRound(ROUND);
		contentV3.setOptions(options);
		contentV3.setAbstentionsAllowed(true);

		final List<Answer> statsV2 = toV2Migrator.migrate(statsV3, contentV3, ROUND);

		final Answer abstentionStatsV2 = statsV2.get(0);
		assertEquals(ABSTENTION_COUNT, abstentionStatsV2.getAnswerCount());
		assertEquals(ABSTENTION_COUNT, abstentionStatsV2.getAbstentionCount());

		for (int i = 0; i < ANSWER_COUNTS.size(); i++) {
			final Answer answerStatsV2 = statsV2.get(i + 1);
			assertEquals(ANSWER_COUNTS.get(i).intValue(), answerStatsV2.getAnswerCount());
			assertEquals(ABSTENTION_COUNT, answerStatsV2.getAbstentionCount());
			assertEquals(options.get(i).getLabel(), answerStatsV2.getAnswerText());
		}
	}
}
