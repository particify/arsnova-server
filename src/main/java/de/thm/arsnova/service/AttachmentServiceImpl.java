/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.multipart.MultipartFile;

import de.thm.arsnova.model.Attachment;
import de.thm.arsnova.persistence.AttachmentRepository;

public class AttachmentServiceImpl extends DefaultEntityServiceImpl<Attachment> implements AttachmentService {
	private AttachmentRepository attachmentRepository;

	public AttachmentServiceImpl(
			final AttachmentRepository repository,
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
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
