package net.particify.arsnova.core.persistence.couchdb;

import java.util.List;
import java.util.Map;

import net.particify.arsnova.core.model.RoomSettings;
import net.particify.arsnova.core.persistence.RoomSettingsRepository;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

public class CouchDbRoomSettingsRepository
    extends MangoCouchDbCrudRepository<RoomSettings>
    implements RoomSettingsRepository, MangoIndexInitializer {
  private static final String ROOM_ID_INDEX_NAME = "survey-settings-roomid-index";

  public CouchDbRoomSettingsRepository(
      final MangoCouchDbConnector db,
      final boolean createIfNotExists) {
    super(RoomSettings.class, db, "by_id", createIfNotExists);
  }

  @Override
  public void createIndexes() {
    createRoomIdIndex();
  }

  private void createRoomIdIndex() {
    final List<MangoCouchDbConnector.MangoQuery.Sort> fields = List.of(
      new MangoCouchDbConnector.MangoQuery.Sort("roomId", false)
    );
    final Map<String, Object> filterSelector = Map.of(
        "type", type.getSimpleName()
    );
    db.createPartialJsonIndex(ROOM_ID_INDEX_NAME, fields, filterSelector);
  }

  @Override
  public RoomSettings findByRoomId(final String roomId) {
    final Map<String, Object> querySelector = Map.of(
        "type", type.getSimpleName(),
        "roomId", roomId
    );
    final MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(querySelector);
    query.setIndexDocument(ROOM_ID_INDEX_NAME);
    return db.query(query, type).stream().findFirst().orElse(null);
  }
}
