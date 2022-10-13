package de.thm.arsnova.service.comment.service.persistence;

import de.thm.arsnova.service.comment.model.BonusToken;
import de.thm.arsnova.service.comment.model.BonusTokenPK;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface BonusTokenRepository extends CrudRepository<BonusToken, BonusTokenPK> {
  List<BonusToken> findByRoomId(String roomId);
  List<BonusToken> findByUserId(String userId);
}
