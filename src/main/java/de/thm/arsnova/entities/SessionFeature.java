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

/**
 * Contains fields that describe which specific feature is activated for a session.
 */
public class SessionFeature {

	private boolean jitt = true;
	private boolean lecture = true;
	private boolean feedback = true;
	private boolean interposed = true;
	private boolean pi = true;
	private boolean learningProgress = true;

	public SessionFeature(SessionFeature features) {
		this();
		if (features != null) {
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

	public boolean isJitt() {
		return jitt;
	}

	public void setJitt(boolean jitt) {
		this.jitt = jitt;
	}

	public boolean isFeedback() {
		return feedback;
	}

	public void setFeedback(boolean feedback) {
		this.feedback = feedback;
	}

	public boolean isInterposed() {
		return interposed;
	}

	public void setInterposed(boolean interposed) {
		this.interposed = interposed;
	}

	public boolean isPi() {
		return pi;
	}

	public void setPi(boolean pi) {
		this.pi = pi;
	}

	public boolean isLearningProgress() {
		return learningProgress;
	}

	public void setLearningProgress(boolean learningProgress) {
		this.learningProgress = learningProgress;
	}

}
