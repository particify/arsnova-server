package net.particify.arsnova.core.persistence.couchdb;

import net.particify.arsnova.core.model.Entity;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

public class MangoCouchDbCrudRepository<T extends Entity> extends CouchDbCrudRepository<T> {
  protected final MangoCouchDbConnector db;

  protected MangoCouchDbCrudRepository(
      final Class<T> type,
      final MangoCouchDbConnector db,
      final String countableAllViewName,
      final boolean createIfNotExists) {
    super(type, db, countableAllViewName, createIfNotExists);
    this.db = db;
  }
}
