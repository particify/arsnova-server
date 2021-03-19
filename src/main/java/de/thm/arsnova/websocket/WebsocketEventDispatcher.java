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

import de.thm.arsnova.event.AfterUpdateEvent;
import de.thm.arsnova.model.Entity;
import de.thm.arsnova.model.RoomIdAware;

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

	public WebsocketEventDispatcher(final RabbitTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
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

	private String extractRoomId(final Entity entity) {
		if (entity instanceof RoomIdAware) {
			return ((RoomIdAware) entity).getRoomId();
		}
		return "";
	}
}
