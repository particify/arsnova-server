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

package de.thm.arsnova.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.thm.arsnova.event.AfterCreationEvent;
import de.thm.arsnova.event.AfterDeletionEvent;
import de.thm.arsnova.event.AfterUpdateEvent;
import de.thm.arsnova.event.CrudEvent;
import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.ContentGroup;
import de.thm.arsnova.model.Entity;
import de.thm.arsnova.model.RoomIdAware;
import de.thm.arsnova.service.RoomStatisticsService;

/**
 * WebsocketEventDispatcher publishes events for changes of an entity to a
 * WS/STOMP topic when corresponding {@link de.thm.arsnova.event.CrudEvent}s are
 * received.
 *
 * @author Daniel Gerhardt
 */
@Component
@Profile("!test")
public class WebsocketEventDispatcher {
	private static final Logger logger = LoggerFactory.getLogger(WebsocketEventDispatcher.class);
	private final RabbitTemplate messagingTemplate;
	private final RoomStatisticsService roomStatisticsService;

	public WebsocketEventDispatcher(
			final RabbitTemplate messagingTemplate,
			final RoomStatisticsService roomStatisticsService) {
		this.messagingTemplate = messagingTemplate;
		this.roomStatisticsService = roomStatisticsService;

	}

	@EventListener
	public <T extends Entity> void dispatchAfterUpdateEvent(final AfterUpdateEvent<T> event) {
		logger.debug("Dispatching update event for {}: {}", event.getEntity().getType().getSimpleName(), event);
		final String roomId = extractRoomId(event.getEntity());
		if (roomId.isEmpty()) {
			logger.debug("Update event is not room related.");
			return;
		}
		final String topic = String.format("%s.%s-%s.changes.stream",
				roomId,
				event.getEntity().getType().getSimpleName().toLowerCase(),
				event.getEntity().getId());
		messagingTemplate.convertAndSend("amq.topic", topic, event.getChanges());
	}

	@EventListener
	public <T extends Entity> void handleCrudEvent(final CrudEvent<T> event) {
		if (event.getResolvableType().isAssignableFrom(Answer.class)) {
			// Answer events are skipped here for multiple reasons:
			// * Events for individual answer changes are not relevant.
			// * Answers are stored in bulk which would lead to spikes of events.
			// * AnswersChanged events are sent instead.
			return;
		}
		if (event instanceof AfterCreationEvent) {
			dispatchCrudEvent(event, ChangeEvent.ChangeType.CREATE);
		} else if (event instanceof AfterUpdateEvent) {
			dispatchCrudEvent(event, ChangeEvent.ChangeType.UPDATE);
		} else if (event instanceof AfterDeletionEvent) {
			dispatchCrudEvent(event, ChangeEvent.ChangeType.DELETE);
		}
	}

	public <T extends Entity> void dispatchCrudEvent(final CrudEvent<T> event, final ChangeEvent.ChangeType changeType) {
		logger.debug("Dispatching update event for {}: {}", event.getEntity().getType().getSimpleName(), event);
		final String roomId = extractRoomId(event.getEntity());
		if (roomId.isEmpty()) {
			logger.debug("Update event is not room related.");
			return;
		}
		final ChangeEvent changeEvent = new ChangeEvent(
				changeType,
				event.getEntity().getClass().getSimpleName(),
				event.getEntity().getId());
		final String topic = String.format("%s.changes-meta.stream", roomId);
		messagingTemplate.convertAndSend("amq.topic", topic, changeEvent);
	}

	@EventListener
	public <T extends Entity> void handleContentGroupCrudEvent(final CrudEvent<ContentGroup> event) {
		if (event instanceof AfterCreationEvent
				|| event instanceof AfterUpdateEvent
				|| event instanceof AfterDeletionEvent) {
			dispatchRoomStatsEvent(event);
		}
	}

	public <T extends Entity> void dispatchRoomStatsEvent(final CrudEvent<ContentGroup> event) {
		logger.debug("Dispatching room stats event for {}: {}", event.getEntity().getType().getSimpleName(), event);
		final String roomId = extractRoomId(event.getEntity());
		final String publicTopic = String.format("%s.changes.stream", roomId);
		final String moderatorTopic = String.format("%s.moderator.changes.stream", roomId);
		messagingTemplate.convertAndSend("amq.topic", publicTopic,
				roomStatisticsService.getPublicRoomStatistics(roomId));
		messagingTemplate.convertAndSend("amq.topic", moderatorTopic,
				roomStatisticsService.getAllRoomStatistics(roomId));
	}

	private String extractRoomId(final Entity entity) {
		if (entity instanceof RoomIdAware) {
			return ((RoomIdAware) entity).getRoomId();
		}
		return "";
	}

	private static class ChangeEvent {
		public ChangeType changeType;
		public String entityType;
		public String entityId;

		private ChangeEvent(final ChangeType changeType, final String entityType, final String entityId) {
			this.changeType = changeType;
			this.entityType = entityType;
			this.entityId = entityId;
		}

		private enum ChangeType {
			CREATE,
			UPDATE,
			DELETE
		}
	}
}
