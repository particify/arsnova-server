/*
 * Copyright (C) 2012 THM webMedia
 *
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.entities;

import java.util.List;

public class Question {

	private String type;
	private String questionType;
	private String subject;
	private String text;
	private boolean active;
	private String releasedFor;
	private List<PossibleAnswer> possibleAnswers;
	private boolean noCorrect;
	private String session;
	private long timestamp;
	private int number;
	private int duration;
	private String _id;
	private String _rev;

	public final String getType() {
		return type;
	}

	public final void setType(final String type) {
		this.type = type;
	}

	public final String getQuestionType() {
		return questionType;
	}

	public final void setQuestionType(final String questionType) {
		this.questionType = questionType;
	}

	public final String getSubject() {
		return subject;
	}

	public final void setSubject(final String subject) {
		this.subject = subject;
	}

	public final String getText() {
		return text;
	}

	public final void setText(final String text) {
		this.text = text;
	}

	public final boolean isActive() {
		return active;
	}

	public final void setActive(final boolean active) {
		this.active = active;
	}

	public final String getReleasedFor() {
		return releasedFor;
	}

	public final void setReleasedFor(final String releasedFor) {
		this.releasedFor = releasedFor;
	}

	public final List<PossibleAnswer> getPossibleAnswers() {
		return possibleAnswers;
	}

	public final void setPossibleAnswers(final List<PossibleAnswer> possibleAnswers) {
		this.possibleAnswers = possibleAnswers;
	}

	public final boolean isNoCorrect() {
		return noCorrect;
	}

	public final void setNoCorrect(final boolean noCorrect) {
		this.noCorrect = noCorrect;
	}

	public final String getSessionId() {
		return session;
	}

	public final void setSessionId(final String session) {
		this.session = session;
	}

	public final String getSession() {
		return session;
	}

	public final void setSession(final String session) {
		this.session = session;
	}

	public final long getTimestamp() {
		return timestamp;
	}

	public final void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public final int getNumber() {
		return number;
	}

	public final void setNumber(final int number) {
		this.number = number;
	}

	public final int getDuration() {
		return duration;
	}

	public final void setDuration(final int duration) {
		this.duration = duration;
	}

	public final String get_id() {
		return _id;
	}

	public final void set_id(final String _id) {
		this._id = _id;
	}

	public final String get_rev() {
		return _rev;
	}

	public final void set_rev(final String _rev) {
		this._rev = _rev;
	}

	@Override
	public final String toString() {
		return "Question type '" + this.type + "': " + this.subject + ";\n" 
				+ this.text	+ this.possibleAnswers;
	}
}
