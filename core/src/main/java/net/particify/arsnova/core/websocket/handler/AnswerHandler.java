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

package net.particify.arsnova.core.websocket.handler;

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

import net.particify.arsnova.core.config.RabbitConfig;
import net.particify.arsnova.core.config.properties.MessageBrokerProperties;
import net.particify.arsnova.core.event.BulkChangeEvent;
import net.particify.arsnova.core.model.Answer;
import net.particify.arsnova.core.model.AnswerStatistics;
import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.Content.Format;
import net.particify.arsnova.core.model.TextAnswer;
import net.particify.arsnova.core.service.AnswerService;
import net.particify.arsnova.core.websocket.message.AnswersChanged;
import net.particify.arsnova.core.websocket.message.TextAnswerCreated;

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
    for (final Map.Entry<String, List<Answer>> entry : groupedAnswers.entrySet()) {
      final String contentId = entry.getKey();
      final Answer anyAnswer = entry.getValue().get(0);
      final String roomId = anyAnswer.getRoomId();
      final List<String> answerIds = groupedAnswers.get(contentId).stream()
          .map(Answer::getId).collect(Collectors.toList());
      AnswerStatistics stats = null;
      final Format format = anyAnswer.getFormat();
      if (format == Content.Format.CHOICE || format == Content.Format.SCALE || format == Content.Format.SORT) {
        stats = answerService.getChoiceStatistics(contentId);
      } else if (format == Content.Format.WORDCLOUD) {
        stats = answerService.getTextStatistics(contentId);
      } else if (format == Content.Format.SHORT_ANSWER) {
        stats = answerService.getShortAnswerStatistics(contentId);
      } else if (format == Content.Format.PRIORITIZATION) {
        stats = answerService.getPrioritizationStatistics(contentId);
      } else if (format == Content.Format.NUMERIC) {
        stats = answerService.getNumericStatistics(contentId);
      }
      final AnswersChanged changedMessage = new AnswersChanged(answerIds, stats);
      messagingTemplate.convertAndSend(
          "amq.topic",
          roomId + ".content-" + contentId + ".answers-changed.stream",
          changedMessage
      );
    }
  }

  @EventListener
  public void handleTextAnswersChanged(final BulkChangeEvent<Answer> event) {
    for (final Answer answer : event.getEntities()) {
      if (!(answer instanceof TextAnswer)) {
        continue;
      }
      final TextAnswerCreated createdMessage = new TextAnswerCreated((TextAnswer) answer);
      messagingTemplate.convertAndSend(
          "amq.topic",
          answer.getRoomId() + ".content-" + answer.getContentId() + ".text-answer-created.stream",
          createdMessage
      );
    }
  }
}
