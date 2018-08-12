package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.model.serialization.View;
import org.springframework.core.style.ToStringCreator;

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
