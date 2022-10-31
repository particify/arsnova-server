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

package net.particify.arsnova.core.event;

import java.util.List;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * This dispatcher publishes events for migration steps which also involve
 * external services. This is usually the case when responsibility for certain
 * data changes and is moved from the backend to an external service.
 *
 * @author Daniel Gerhardt
 */
@Component
public class AmqpMigrationEventDispatcher {
  public static final String PARTICIPANT_ACCESS_MIGRATION_QUEUE_NAME = "backend.event.migration.access.participant";

  private final RabbitTemplate rabbitTemplate;

  public AmqpMigrationEventDispatcher(final RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @EventListener
  public void dispatchRoomHistoryMigrationEvent(final RoomHistoryMigrationEvent event) {
    rabbitTemplate.convertAndSend(
        PARTICIPANT_ACCESS_MIGRATION_QUEUE_NAME,
        new ParticipantAccessMigrationMessage(event.getUserId(), event.getRoomIds()));
  }

  private static class ParticipantAccessMigrationMessage {
    private String userId;
    private List<String> roomIds;

    private ParticipantAccessMigrationMessage(final String userId, final List<String> roomIds) {
      this.userId = userId;
      this.roomIds = roomIds;
    }

    public String getUserId() {
      return userId;
    }

    public List<String> getRoomIds() {
      return roomIds;
    }
  }
}
