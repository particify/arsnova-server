package de.thm.arsnova.persistance.couchdb;

import de.thm.arsnova.entities.Attachment;
import de.thm.arsnova.persistance.AttachmentRepository;
import org.ektorp.CouchDbConnector;

public class CouchDbAttachmentRepository extends CouchDbCrudRepository<Attachment> implements AttachmentRepository {
	public CouchDbAttachmentRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Attachment.class, db, "by_creatorid", createIfNotExists);
	}
}
