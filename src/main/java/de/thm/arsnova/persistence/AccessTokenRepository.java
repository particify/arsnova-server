package de.thm.arsnova.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import de.thm.arsnova.model.AccessToken;

public interface AccessTokenRepository extends CrudRepository<AccessToken, String> {
	List<AccessToken> findByRoomId(String roomId);

	Optional<AccessToken> findByRoomIdAndToken(String roomId, String token);

	List<AccessToken> findIdsByExpirationDateIsBefore(LocalDateTime expirationDate);
}
