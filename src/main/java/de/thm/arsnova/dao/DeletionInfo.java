/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
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
package de.thm.arsnova.dao;

import java.util.ArrayList;
import java.util.List;

/**
 * Carries usable information about the documents that have been deleted from the database.
 */
public class DeletionInfo {

	private final List<String> deletedIds;

	public DeletionInfo(List<String> deletedIds) {
		this.deletedIds = deletedIds;
	}

	public DeletionInfo(String id) {
		this();
		this.deletedIds.add(id);
	}

	public DeletionInfo() {
		this.deletedIds = new ArrayList<String>();
	}

	/**
	 * @return number of deleted documents
	 */
	public int count() {
		return this.deletedIds.size();
	}

}
