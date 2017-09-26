package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.Attachment;
import org.springframework.data.repository.CrudRepository;

public interface AttachmentRepository extends CrudRepository<Attachment, String> {
}
