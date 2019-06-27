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

package de.thm.arsnova.model.migration.v2;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import de.thm.arsnova.model.serialization.View;

/**
 * Wrapper class for counting read and unread Comments for a Room or a single user.
 */
@ApiModel(value = "Comment Reading Count", description = "Comment Reading Count statistics entity")
public class CommentReadingCount {

	private int read;
	private int unread;

	public CommentReadingCount(final int readCount, final int unreadCount) {
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

	public void setRead(final int read) {
		this.read = read;
	}

	@ApiModelProperty(required = true, value = "the number of unread comments")
	@JsonView(View.Public.class)
	public int getUnread() {
		return unread;
	}

	public void setUnread(final int unread) {
		this.unread = unread;
	}

	@ApiModelProperty(required = true, value = "the number of total comments")
	@JsonView(View.Public.class)
	public int getTotal() {
		return getRead() + getUnread();
	}
}
