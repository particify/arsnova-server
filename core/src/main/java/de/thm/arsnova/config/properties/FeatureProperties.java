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

package de.thm.arsnova.config.properties;

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.boot.context.properties.ConfigurationProperties;

import de.thm.arsnova.model.serialization.View;

@ConfigurationProperties(FeatureProperties.PREFIX)
public class FeatureProperties {
	public static final String PREFIX = "features";

	public static class Contents {
		private boolean enabled;
		private int answerOptionLimit;

		@JsonView(View.Public.class)
		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(final boolean enabled) {
			this.enabled = enabled;
		}

		@JsonView(View.Public.class)
		public int getAnswerOptionLimit() {
			return answerOptionLimit;
		}

		public void setAnswerOptionLimit(final int answerOptionLimit) {
			this.answerOptionLimit = answerOptionLimit;
		}
	}

	public static class Comments {
		private boolean enabled;

		@JsonView(View.Public.class)
		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(final boolean enabled) {
			this.enabled = enabled;
		}
	}

	public static class LiveFeedback {
		private boolean enabled;
		private int resetInterval;

		@JsonView(View.Public.class)
		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(final boolean enabled) {
			this.enabled = enabled;
		}

		public int getResetInterval() {
			return resetInterval;
		}

		public void setResetInterval(final int resetInterval) {
			this.resetInterval = resetInterval;
		}
	}

	public static class ContentPool {
		private boolean enabled;

		@JsonView(View.Public.class)
		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(final boolean enabled) {
			this.enabled = enabled;
		}
	}

	private Contents contents;
	private Comments comments;
	private LiveFeedback liveFeedback;
	private ContentPool contentPool;

	@JsonView(View.Public.class)
	public Contents getContents() {
		return contents;
	}

	public void setContents(final Contents contents) {
		this.contents = contents;
	}

	@JsonView(View.Public.class)
	public Comments getComments() {
		return comments;
	}

	public void setComments(final Comments comments) {
		this.comments = comments;
	}

	@JsonView(View.Public.class)
	public LiveFeedback getLiveFeedback() {
		return liveFeedback;
	}

	public void setLiveFeedback(final LiveFeedback liveFeedback) {
		this.liveFeedback = liveFeedback;
	}

	@JsonView(View.Public.class)
	public ContentPool getContentPool() {
		return contentPool;
	}

	public void setContentPool(final ContentPool contentPool) {
		this.contentPool = contentPool;
	}
}
