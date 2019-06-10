package de.thm.arsnova.service.comment.service.persistence;

import de.thm.arsnova.service.comment.model.RoomAccess;
import de.thm.arsnova.service.comment.model.RoomAccessPK;
import org.springframework.data.repository.CrudRepository;

public interface RoomAccessRepository extends CrudRepository<RoomAccess, RoomAccessPK> {
}
