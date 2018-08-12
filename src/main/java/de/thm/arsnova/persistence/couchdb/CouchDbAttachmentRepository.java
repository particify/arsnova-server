package de.thm.arsnova.persistence.couchdb;

import de.thm.arsnova.model.Attachment;
import de.thm.arsnova.persistence.AttachmentRepository;
import org.ektorp.CouchDbConnector;

public class CouchDbAttachmentRepository extends CouchDbCrudRepository<Attachment> implements AttachmentRepository {
	public CouchDbAttachmentRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Attachment.class, db, "by_id", createIfNotExists);
	}
}
