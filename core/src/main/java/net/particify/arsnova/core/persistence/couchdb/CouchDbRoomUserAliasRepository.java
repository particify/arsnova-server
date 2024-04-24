package net.particify.arsnova.core.persistence.couchdb;

import java.util.List;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;

import net.particify.arsnova.core.model.RoomUserAlias;
import net.particify.arsnova.core.persistence.RoomUserAliasRepository;

public class CouchDbRoomUserAliasRepository
    extends CouchDbCrudRepository<RoomUserAlias>
    implements RoomUserAliasRepository {
  public CouchDbRoomUserAliasRepository(final CouchDbConnector db, final boolean createIfNotExists) {
    super(RoomUserAlias.class, db, "by_id", createIfNotExists);
  }

  @Override
  public List<RoomUserAlias> findByRoomId(final String roomId) {
    return db.queryView(createQuery("by_roomid_userid")
            .includeDocs(true)
            .reduce(false)
            .startKey(ComplexKey.of(roomId))
            .endKey(ComplexKey.of(roomId, ComplexKey.emptyObject())),
        RoomUserAlias.class);
  }

  @Override
  public RoomUserAlias findByRoomIdAndUserId(final String roomId, final String userId) {
    final List<RoomUserAlias> roomUserAliasList = db.queryView(createQuery("by_roomid_userid")
            .includeDocs(true)
            .reduce(false)
            .key(ComplexKey.of(roomId, userId)),
        RoomUserAlias.class);
    return !roomUserAliasList.isEmpty() ? roomUserAliasList.get(0) : null;
  }

  @Override
  public List<RoomUserAlias> findByUserId(final String userId) {
    return db.queryView(createQuery("by_userid")
            .includeDocs(true)
            .reduce(false)
            .key(userId),
        RoomUserAlias.class);
  }
}
