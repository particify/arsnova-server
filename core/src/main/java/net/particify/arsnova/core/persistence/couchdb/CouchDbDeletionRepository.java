package net.particify.arsnova.core.persistence.couchdb;

import org.ektorp.CouchDbConnector;

import net.particify.arsnova.core.model.Deletion;
import net.particify.arsnova.core.persistence.DeletionRepository;

public class CouchDbDeletionRepository extends CouchDbCrudRepository<Deletion> implements DeletionRepository {
  public CouchDbDeletionRepository(final CouchDbConnector db, final boolean createIfNotExists) {
    super(Deletion.class, db, "by_id", createIfNotExists);
  }
}
