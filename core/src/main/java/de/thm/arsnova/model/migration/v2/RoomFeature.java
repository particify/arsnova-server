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
import java.io.Serializable;

import de.thm.arsnova.model.serialization.View;

/**
 * Contains fields that describe which specific Feature is activated for a Room.
 */
@ApiModel(value = "RoomFeature",
		description = "Room (Session) Feature entity - Represents feature/use case settings of a Room")
public class RoomFeature implements Serializable {

	private boolean custom = false;
	private boolean clicker = false;
	private boolean peerGrading = false;
	private boolean twitterWall = false;
	private boolean liveFeedback = false;
	private boolean interposedFeedback = false;
	private boolean liveClicker = false;
	private boolean flashcard = false;
	private boolean total = false;

	private boolean jitt = false;
	private boolean lecture = false;
	private boolean feedback = false;
	private boolean interposed = false;
	private boolean pi = false;
	private boolean learningProgress = false;
	private boolean flashcardFeature = false;
	private boolean slides = false;

	public RoomFeature(final RoomFeature features) {
		this();
		if (features != null) {
			this.custom = features.custom;
			this.clicker = features.clicker;
			this.peerGrading = features.peerGrading;
			this.twitterWall = features.twitterWall;
			this.liveFeedback = features.liveFeedback;
			this.interposedFeedback = features.interposedFeedback;
			this.liveClicker = features.liveClicker;
			this.flashcardFeature = features.flashcardFeature;
			this.flashcard = features.flashcard;
			this.total = features.total;
			this.lecture = features.lecture;
			this.jitt = features.jitt;
			this.feedback = features.feedback;
			this.interposed = features.interposed;
			this.pi = features.pi;
			this.learningProgress = features.learningProgress;
			this.slides = features.slides;
		}
	}

	public RoomFeature() {

	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isLecture() {
		return lecture;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setLecture(final boolean lecture) {
		this.lecture = lecture;
	}

	@ApiModelProperty(required = true, value = "jitt")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isJitt() {
		return jitt;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setJitt(final boolean jitt) {
		this.jitt = jitt;
	}

	@ApiModelProperty(required = true, value = "feedback")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isFeedback() {
		return feedback;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setFeedback(final boolean feedback) {
		this.feedback = feedback;
	}

	@ApiModelProperty(required = true, value = "interposed")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isInterposed() {
		return interposed;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setInterposed(final boolean interposed) {
		this.interposed = interposed;
	}

	@ApiModelProperty(required = true, value = "peer instruction")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isPi() {
		return pi;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPi(final boolean pi) {
		this.pi = pi;
	}

	@ApiModelProperty(required = true, value = "score")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isLearningProgress() {
		return learningProgress;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setLearningProgress(final boolean learningProgress) {
		this.learningProgress = learningProgress;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isCustom() {
		return custom;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setCustom(final boolean custom) {
		this.custom = custom;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isClicker() {
		return clicker;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setClicker(final boolean clicker) {
		this.clicker = clicker;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isPeerGrading() {
		return peerGrading;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPeerGrading(final boolean peerGrading) {
		this.peerGrading = peerGrading;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isFlashcardFeature() {
		return flashcardFeature;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setFlashcardFeature(final boolean flashcardFeature) {
		this.flashcardFeature = flashcardFeature;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isFlashcard() {
		return flashcard;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setFlashcard(final boolean flashcard) {
		this.flashcard = flashcard;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isTotal() {
		return total;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setTotal(final boolean total) {
		this.total = total;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isLiveFeedback() {
		return liveFeedback;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setLiveFeedback(final boolean liveFeedback) {
		this.liveFeedback = liveFeedback;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isInterposedFeedback() {
		return interposedFeedback;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setInterposedFeedback(final boolean interposedFeedback) {
		this.interposedFeedback = interposedFeedback;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isLiveClicker() {
		return liveClicker;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setLiveClicker(final boolean liveClicker) {
		this.liveClicker = liveClicker;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isTwitterWall() {
		return twitterWall;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setTwitterWall(final boolean twitterWall) {
		this.twitterWall = twitterWall;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isSlides() {
		return slides;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSlides(final boolean slides) {
		this.slides = slides;
	}

}
