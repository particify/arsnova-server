package net.particify.arsnova.core.persistence.couchdb;

import org.ektorp.CouchDbConnector;

import net.particify.arsnova.core.model.ContentTemplate;
import net.particify.arsnova.core.persistence.ContentTemplateRepository;

public class CouchDbContentTemplateRepository
    extends CouchDbCrudRepository<ContentTemplate>
    implements ContentTemplateRepository {
  public CouchDbContentTemplateRepository(
      final CouchDbConnector db,
      final boolean createIfNotExists) {
    super(ContentTemplate.class, db, "by_id", createIfNotExists);
  }
}
