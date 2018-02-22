/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Wrapper class for counting read and unread comments for a session or a single user.
 */
@ApiModel(value = "audiencequestion/readcount", description = "the comment reading count entity")
public class CommentReadingCount {

	private int read;
	private int unread;

	public CommentReadingCount(int readCount, int unreadCount) {
		this.read = readCount;
		this.unread = unreadCount;
	}

	public CommentReadingCount() {
		this.read = 0;
		this.unread = 0;
	}

	@ApiModelProperty(required = true, value = "the number of read comments")
	@JsonView(View.Public.class)
	public int getRead() {
		return read;
	}

	public void setRead(int read) {
		this.read = read;
	}

	@ApiModelProperty(required = true, value = "the number of unread comments")
	@JsonView(View.Public.class)
	public int getUnread() {
		return unread;
	}

	public void setUnread(int unread) {
		this.unread = unread;
	}

	@ApiModelProperty(required = true, value = "the number of total comments")
	@JsonView(View.Public.class)
	public int getTotal() {
		return getRead() + getUnread();
	}
}
