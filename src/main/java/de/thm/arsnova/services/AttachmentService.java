package de.thm.arsnova.services;

import de.thm.arsnova.entities.Attachment;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService extends EntityService<Attachment> {
	void upload(Attachment attachment, MultipartFile file);
	void download(Attachment attachment);
}
