package net.particify.arsnova.core.event;

import java.util.UUID;

public record AmqpRoomDuplicationEvent(UUID originalRoomId, UUID duplicatedRoomId) {
}
