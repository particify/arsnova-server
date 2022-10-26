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

package de.thm.arsnova.persistence.couchdb;

import org.ektorp.CouchDbConnector;

import de.thm.arsnova.model.Attachment;
import de.thm.arsnova.persistence.AttachmentRepository;

public class CouchDbAttachmentRepository extends CouchDbCrudRepository<Attachment> implements AttachmentRepository {
	public CouchDbAttachmentRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Attachment.class, db, "by_id", createIfNotExists);
	}
}
