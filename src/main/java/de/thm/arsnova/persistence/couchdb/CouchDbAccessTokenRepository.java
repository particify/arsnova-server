package de.thm.arsnova.persistence.couchdb;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;

import de.thm.arsnova.model.AccessToken;
import de.thm.arsnova.persistence.AccessTokenRepository;

public class CouchDbAccessTokenRepository extends CouchDbCrudRepository<AccessToken>
		implements AccessTokenRepository {
	public CouchDbAccessTokenRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(AccessToken.class, db, "by_id", createIfNotExists);
	}

	@Override
	public List<AccessToken> findByRoomId(final String roomId) {
		return db.queryView(createQuery("by_roomid_token")
						.includeDocs(true)
						.reduce(false)
						.startKey(ComplexKey.of(roomId))
						.endKey(ComplexKey.of(roomId, ComplexKey.emptyObject())),
				AccessToken.class);
	}

	@Override
	public Optional<AccessToken> findByRoomIdAndToken(final String roomId, final String token) {
		final List<AccessToken> accessTokenList = db.queryView(createQuery("by_roomid_token")
						.includeDocs(true)
						.reduce(false)
						.key(ComplexKey.of(roomId, token)),
				AccessToken.class);

		return !accessTokenList.isEmpty() ? Optional.of(accessTokenList.get(0)) : Optional.empty();
	}

	@Override
	public List<AccessToken> findIdsByExpirationDateIsBefore(final LocalDateTime expirationDate) {
		return db.queryView(createQuery("by_expirationdate")
						.includeDocs(false)
						.reduce(false)
						.endKey(expirationDate),
				AccessToken.class);
	}
}
