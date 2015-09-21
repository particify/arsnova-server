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
package de.thm.arsnova.entities;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Contains fields that describe which specific feature is activated for a session.
 */
@ApiModel(value = "session feature", description = "the session feature entity")
public class SessionFeature {

	private boolean custom = true;
	private boolean clicker = false;
	private boolean peerGrading = false;
	private boolean liveFeedback = false;
	private boolean flashcard = false;
	private boolean total = false;

	private boolean jitt = true;
	private boolean lecture = true;
	private boolean feedback = true;
	private boolean interposed = true;
	private boolean pi = true;
	private boolean learningProgress = true;

	public SessionFeature(SessionFeature features) {
		this();
		if (features != null) {
			this.custom = features.custom;
			this.clicker = features.clicker;
			this.peerGrading = features.peerGrading;
			this.liveFeedback = features.liveFeedback;
			this.flashcard = features.flashcard;
			this.total = features.total;
			this.lecture = features.lecture;
			this.jitt = features.jitt;
			this.feedback = features.feedback;
			this.interposed = features.interposed;
			this.pi = features.pi;
			this.learningProgress = features.learningProgress;
		}
	}

	public SessionFeature() {}

	public boolean isLecture() {
		return lecture;
	}

	public void setLecture(boolean lecture) {
		this.lecture = lecture;
	}

	@ApiModelProperty(required = true, value = "jitt")
	public boolean isJitt() {
		return jitt;
	}

	public void setJitt(boolean jitt) {
		this.jitt = jitt;
	}

	@ApiModelProperty(required = true, value = "feedback")
	public boolean isFeedback() {
		return feedback;
	}

	public void setFeedback(boolean feedback) {
		this.feedback = feedback;
	}

	@ApiModelProperty(required = true, value = "interposed")
	public boolean isInterposed() {
		return interposed;
	}

	public void setInterposed(boolean interposed) {
		this.interposed = interposed;
	}

	@ApiModelProperty(required = true, value = "peer instruction")
	public boolean isPi() {
		return pi;
	}

	public void setPi(boolean pi) {
		this.pi = pi;
	}

	@ApiModelProperty(required = true, value = "learning progress")
	public boolean isLearningProgress() {
		return learningProgress;
	}

	public void setLearningProgress(boolean learningProgress) {
		this.learningProgress = learningProgress;
	}

	public boolean isCustom() {
		return custom;
	}

	public void setCustom(boolean custom) {
		this.custom = custom;
	}

	public boolean isClicker() {
		return clicker;
	}

	public void setClicker(boolean clicker) {
		this.clicker = clicker;
	}

	public boolean isPeerGrading() {
		return peerGrading;
	}

	public void setPeerGrading(boolean peerGrading) {
		this.peerGrading = peerGrading;
	}

	public boolean isFlashcard() {
		return flashcard;
	}

	public void setFlashcard(boolean flashcard) {
		this.flashcard = flashcard;
	}

	public boolean isTotal() {
		return total;
	}

	public void setTotal(boolean total) {
		this.total = total;
	}

	public boolean isLiveFeedback() {
		return liveFeedback;
	}

	public void setLiveFeedback(boolean liveFeedback) {
		this.liveFeedback = liveFeedback;
	}

}
