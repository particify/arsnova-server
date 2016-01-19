/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2016 The ARSnova Team
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
package de.thm.arsnova.entities;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Wrapper class for counting read and unread interposed questions for a session or a single user.
 */
@ApiModel(value = "audiencequestion/readcount", description = "the interposed reading count entity")
public class InterposedReadingCount {

	private long read;
	private long unread;

	public InterposedReadingCount(long readCount, long unreadCount) {
		this.read = readCount;
		this.unread = unreadCount;
	}

	public InterposedReadingCount() {
		this.read = 0;
		this.unread = 0;
	}

	@ApiModelProperty(required = true, value = "the number of read interposed questions")
	public long getRead() {
		return read;
	}

	public void setRead(long read) {
		this.read = read;
	}

	@ApiModelProperty(required = true, value = "the number of unread interposed questions")
	public long getUnread() {
		return unread;
	}

	public void setUnread(int unread) {
		this.unread = unread;
	}

	@ApiModelProperty(required = true, value = "the number of total interposed questions")
	public long getTotal() {
		return getRead() + getUnread();
	}
}
