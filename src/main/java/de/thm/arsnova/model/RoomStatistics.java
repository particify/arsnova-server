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

package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.View;

public class RoomStatistics {
	private int contentCount = 0;
	private int unansweredContentCount = 0;
	private int answerCount = 0;
	private int unreadAnswerCount = 0;
	private int commentCount = 0;
	private int unreadCommentCount = 0;

	@JsonView(View.Public.class)
	public int getUnansweredContentCount() {
		return unansweredContentCount;
	}

	@JsonView(View.Public.class)
	public void setUnansweredContentCount(final int unansweredContentCount) {
		this.unansweredContentCount = unansweredContentCount;
	}

	@JsonView(View.Public.class)
	public int getContentCount() {
		return contentCount;
	}

	public void setContentCount(final int contentCount) {
		this.contentCount = contentCount;
	}

	@JsonView(View.Public.class)
	public int getAnswerCount() {
		return answerCount;
	}

	public void setAnswerCount(final int answerCount) {
		this.answerCount = answerCount;
	}

	@JsonView(View.Public.class)
	public int getUnreadAnswerCount() {
		return unreadAnswerCount;
	}

	public void setUnreadAnswerCount(final int unreadAnswerCount) {
		this.unreadAnswerCount = unreadAnswerCount;
	}

	@JsonView(View.Public.class)
	public int getCommentCount() {
		return commentCount;
	}

	public void setCommentCount(final int commentCount) {
		this.commentCount = commentCount;
	}

	@JsonView(View.Public.class)
	public int getUnreadCommentCount() {
		return unreadCommentCount;
	}

	public void setUnreadCommentCount(final int unreadCommentCount) {
		this.unreadCommentCount = unreadCommentCount;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("contentCount", contentCount)
				.append("unansweredContentCount", unansweredContentCount)
				.append("answerCount", answerCount)
				.append("unreadAnswerCount", unreadAnswerCount)
				.append("commentCount", commentCount)
				.append("unreadCommentCount", unreadCommentCount)
				.toString();
	}
}
