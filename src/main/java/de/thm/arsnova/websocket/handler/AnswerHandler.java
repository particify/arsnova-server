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

package de.thm.arsnova.websocket.handler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.thm.arsnova.config.RabbitConfig;
import de.thm.arsnova.config.properties.MessageBrokerProperties;
import de.thm.arsnova.event.BulkChangeEvent;
import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.AnswerStatistics;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.service.AnswerService;
import de.thm.arsnova.websocket.message.AnswersChanged;

@Component
@EnableConfigurationProperties(MessageBrokerProperties.class)
@ConditionalOnProperty(
		name = RabbitConfig.RabbitConfigProperties.RABBIT_ENABLED,
		prefix = MessageBrokerProperties.PREFIX,
		havingValue = "true")
public class AnswerHandler {
	private static final Logger logger = LoggerFactory.getLogger(AnswerHandler.class);

	private final RabbitTemplate messagingTemplate;
	private final AnswerService answerService;

	public AnswerHandler(final RabbitTemplate messagingTemplate,
			final AnswerService answerService) {
		this.messagingTemplate = messagingTemplate;
		this.answerService = answerService;
	}

	@EventListener
	public void handleAnswersChanged(final BulkChangeEvent<Answer> event) {
		/* Group by content so we can send one event per distinct content */
		final Map<String, List<Answer>> groupedAnswers = StreamSupport.stream(event.getEntities().spliterator(), false)
				.collect(Collectors.groupingBy(Answer::getContentId));
		final Set<String> ids = groupedAnswers.keySet();
		logger.debug("Sending events to topic with key answers-changed for contents: {}", ids);
		for (final String contentId : ids) {
			final Answer anyAnswer = groupedAnswers.get(contentId).get(0);
			final String roomId = anyAnswer.getRoomId();
			final List<String> answerIds = groupedAnswers.get(contentId).stream()
					.map(Answer::getId).collect(Collectors.toList());
			final AnswerStatistics stats =
					anyAnswer.getFormat() == Content.Format.CHOICE || anyAnswer.getFormat() == Content.Format.SCALE
					|| anyAnswer.getFormat() == Content.Format.SORT || anyAnswer.getFormat() == Content.Format.WORDCLOUD
							? answerService.getStatistics(contentId)
							: null;
			final AnswersChanged stompEvent = new AnswersChanged(answerIds, stats);
			messagingTemplate.convertAndSend(
					"amq.topic",
					roomId + ".content-" + contentId + ".answers-changed.stream",
					stompEvent
			);
		}
	}
}
