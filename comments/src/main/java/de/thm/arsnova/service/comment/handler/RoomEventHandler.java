package de.thm.arsnova.service.comment.handler;

import de.thm.arsnova.service.comment.model.RoomAccess;
import de.thm.arsnova.service.comment.model.event.RoomCreated;
import de.thm.arsnova.service.comment.model.event.RoomCreatedPayload;
import de.thm.arsnova.service.comment.service.RoomAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoomEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(RoomEventHandler.class);

    private final RoomAccessService service;

    @Autowired
    public RoomEventHandler(RoomAccessService roomAccessService) {
        service = roomAccessService;
    }


    @RabbitListener(queues = "comment.service.room.created")
    public void handleEvent(RoomCreated event) {
        RoomCreatedPayload p = event.getPayload();

        RoomAccess roomAccess = new RoomAccess(p.getId(), p.getOwnerId(), RoomAccess.Role.OWNER);

        service.create(roomAccess);
    }
}
