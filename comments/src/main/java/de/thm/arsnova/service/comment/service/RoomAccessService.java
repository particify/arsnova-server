package de.thm.arsnova.service.comment.service;

import de.thm.arsnova.service.comment.model.RoomAccess;
import de.thm.arsnova.service.comment.model.RoomAccessPK;
import de.thm.arsnova.service.comment.service.persistence.RoomAccessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoomAccessService {
    private static final Logger logger = LoggerFactory.getLogger(RoomAccessService.class);

    final RoomAccessRepository repository;

    @Autowired
    public RoomAccessService(RoomAccessRepository repository) {
        this.repository = repository;
    }

    public RoomAccess get(RoomAccessPK pk) {
        return repository.findById(pk).orElse(new RoomAccess());
    }

    public RoomAccess create(RoomAccess roomAccess) {
        logger.trace("Creating RoomAccess: " + roomAccess.toString());
        return repository.save(roomAccess);
    }
}
