package de.thm.arsnova.services;

import de.thm.arsnova.entities.Attachment;
import de.thm.arsnova.persistance.AttachmentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.multipart.MultipartFile;

public class AttachmentServiceImpl extends DefaultEntityServiceImpl<Attachment> implements AttachmentService {
	private AttachmentRepository attachmentRepository;

	public AttachmentServiceImpl(
			final AttachmentRepository repository,
			@Qualifier("defaultJsonMessageConverter") final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
		super(Attachment.class, repository, jackson2HttpMessageConverter.getObjectMapper());
		this.attachmentRepository = repository;
	}

	public void upload(Attachment attachment, MultipartFile file) {
		/* TODO: implement file upload to storage */
		create(attachment);
	}

	public void download(Attachment attachment) {
		/* TODO: implement file download from external URL to storage */
		create(attachment);
	}
}
