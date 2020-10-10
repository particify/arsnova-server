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
import java.util.HashMap;
import java.util.Map;

import de.thm.arsnova.model.serialization.View;

@JsonView(View.Admin.class)
public class Statistics {
	@JsonView(View.Admin.class)
	public static class UserProfileStats {
		private int totalCount;
		private Map<String, Integer> countByAuthProvider = new HashMap<>();
		private int activationsPending;

		public int getTotalCount() {
			return totalCount;
		}

		public void setTotalCount(final int totalCount) {
			this.totalCount = totalCount;
		}

		public Map<String, Integer> getCountByAuthProvider() {
			return countByAuthProvider;
		}

		public void setCountByAuthProvider(final Map<String, Integer> countByAuthProvider) {
			this.countByAuthProvider = countByAuthProvider;
		}

		public int getActivationsPending() {
			return activationsPending;
		}

		public void setActivationsPending(final int activationsPending) {
			this.activationsPending = activationsPending;
		}
	}

	@JsonView(View.Admin.class)
	public static class RoomStats {
		private int totalCount;
		private int closed;
		private int moderated;
		private int moderators;

		public int getTotalCount() {
			return totalCount;
		}

		public void setTotalCount(final int totalCount) {
			this.totalCount = totalCount;
		}

		public int getClosed() {
			return closed;
		}

		public void setClosed(final int closed) {
			this.closed = closed;
		}

		public int getModerated() {
			return moderated;
		}

		public void setModerated(final int moderated) {
			this.moderated = moderated;
		}

		public int getModerators() {
			return moderators;
		}

		public void setModerators(final int moderators) {
			this.moderators = moderators;
		}
	}

	@JsonView(View.Admin.class)
	public static class ContentStats {
		private int totalCount;
		private Map<String, Integer> countByFormat = new HashMap<>();

		public int getTotalCount() {
			return totalCount;
		}

		public void setTotalCount(final int totalCount) {
			this.totalCount = totalCount;
		}

		public Map<String, Integer> getCountByFormat() {
			return countByFormat;
		}

		public void setCountByFormat(final Map<String, Integer> countByFormat) {
			this.countByFormat = countByFormat;
		}
	}

	@JsonView(View.Admin.class)
	public static class AnswerStats {
		private int totalCount;
		private Map<String, Integer> countByFormat = new HashMap<>();

		public int getTotalCount() {
			return totalCount;
		}

		public void setTotalCount(final int totalCount) {
			this.totalCount = totalCount;
		}

		public Map<String, Integer> getCountByFormat() {
			return countByFormat;
		}

		public void setCountByFormat(final Map<String, Integer> countByFormat) {
			this.countByFormat = countByFormat;
		}
	}

	@JsonView(View.Admin.class)
	public static class CommentStats {
		private int totalCount;

		public int getTotalCount() {
			return totalCount;
		}

		public void setTotalCount(final int totalCount) {
			this.totalCount = totalCount;
		}
	}

	private UserProfileStats userProfile;
	private RoomStats room;
	private ContentStats content;
	private AnswerStats answer;
	private CommentStats comment;

	public Statistics() {
		this.userProfile = new UserProfileStats();
		this.room = new RoomStats();
		this.content = new ContentStats();
		this.answer = new AnswerStats();
		this.comment = new CommentStats();
	}

	public Statistics(
			final UserProfileStats userProfile,
			final RoomStats room,
			final ContentStats content,
			final AnswerStats answer, final CommentStats comment) {
		this.userProfile = userProfile;
		this.room = room;
		this.content = content;
		this.answer = answer;
		this.comment = comment;
	}

	public UserProfileStats getUserProfile() {
		return userProfile;
	}

	public void setUserProfile(final UserProfileStats userProfile) {
		this.userProfile = userProfile;
	}

	public RoomStats getRoom() {
		return room;
	}

	public void setRoom(final RoomStats room) {
		this.room = room;
	}

	public ContentStats getContent() {
		return content;
	}

	public void setContent(final ContentStats content) {
		this.content = content;
	}

	public AnswerStats getAnswer() {
		return answer;
	}

	public void setAnswer(final AnswerStats answer) {
		this.answer = answer;
	}

	public CommentStats getComment() {
		return comment;
	}

	public void setComment(final CommentStats comment) {
		this.comment = comment;
	}
}
