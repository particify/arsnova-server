package net.particify.arsnova.comments.service.persistence;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

import net.particify.arsnova.comments.model.BonusToken;
import net.particify.arsnova.comments.model.BonusTokenPK;

public interface BonusTokenRepository extends CrudRepository<BonusToken, BonusTokenPK> {
  List<BonusToken> findByRoomId(String roomId);
  List<BonusToken> findByUserId(String userId);
}
