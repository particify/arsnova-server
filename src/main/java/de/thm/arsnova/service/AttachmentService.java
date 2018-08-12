package de.thm.arsnova.service;

import de.thm.arsnova.model.Attachment;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService extends EntityService<Attachment> {
	void upload(Attachment attachment, MultipartFile file);
	void download(Attachment attachment);
}
