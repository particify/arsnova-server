package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

public class SessionStatistics {
	private int contentCount = 0;
	private int answerCount = 0;
	private int unreadAnswerCount = 0;
	private int commentCount = 0;
	private int unreadCommentCount = 0;

	public SessionStatistics() {

	}

	@JsonView(View.Public.class)
	public int getContentCount() {
		return contentCount;
	}

	public void setContentCount(int contentCount) {
		this.contentCount = contentCount;
	}

	@JsonView(View.Public.class)
	public int getAnswerCount() {
		return answerCount;
	}

	public void setAnswerCount(int answerCount) {
		this.answerCount = answerCount;
	}

	@JsonView(View.Public.class)
	public int getUnreadAnswerCount() {
		return unreadAnswerCount;
	}

	public void setUnreadAnswerCount(int unreadAnswerCount) {
		this.unreadAnswerCount = unreadAnswerCount;
	}

	@JsonView(View.Public.class)
	public int getCommentCount() {
		return commentCount;
	}

	public void setCommentCount(int commentCount) {
		this.commentCount = commentCount;
	}

	@JsonView(View.Public.class)
	public int getUnreadCommentCount() {
		return unreadCommentCount;
	}

	public void setUnreadCommentCount(int unreadCommentCount) {
		this.unreadCommentCount = unreadCommentCount;
	}
}
