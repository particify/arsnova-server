package net.particify.arsnova.core.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import net.particify.arsnova.core.model.AccessToken;

public interface AccessTokenRepository extends CrudRepository<AccessToken, String> {
  List<AccessToken> findByRoomId(String roomId);

  Optional<AccessToken> findByRoomIdAndToken(String roomId, String token);

  List<AccessToken> findIdsByExpirationDateIsBefore(LocalDateTime expirationDate);
}
