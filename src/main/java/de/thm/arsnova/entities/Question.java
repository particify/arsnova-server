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
	private String questionVariant;
	private String subject;
	private String text;
	private boolean active;
	private String releasedFor;
	private List<PossibleAnswer> possibleAnswers;
	private boolean noCorrect;
	// TODO: We currently need both sessionId and sessionKeyword, but sessionKeyword will not be persisted.
	private String sessionId;
	private String sessionKeyword;
	private long timestamp;
	private int number;
	private int duration;
	private int piRound;
	private boolean showStatistic; // sic
	private boolean showAnswer;
	private boolean abstention;
	private String _id;
	private String _rev;

	private String image;
	private int gridSize;
	private int offsetX;
	private int offsetY;
	private int zoomLvl;
	private int gridOffsetX;
	private int gridOffsetY;
	private int gridZoomLvl;
	private int gridSizeX;
	private int gridSizeY;
	private boolean gridIsHidden;
	private int imgRotation;
	private boolean toggleFieldsLeft;
	private int numClickableFields;
	private int thresholdCorrectAnswers;

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

	public final String getQuestionVariant() {
		return questionVariant;
	}

	public final void setQuestionVariant(final String questionVariant) {
		this.questionVariant = questionVariant;
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
		return sessionId;
	}

	public final void setSessionId(final String sessionId) {
		this.sessionId = sessionId;
	}

	public final String getSession() {
		return sessionId;
	}

	public final void setSession(final String session) {
		sessionId = session;
	}

	public final String getSessionKeyword() {
		return sessionKeyword;
	}

	public final void setSessionKeyword(final String keyword) {
		sessionKeyword = keyword;
	}

	public final long getTimestamp() {
		return timestamp;
	}

	public final void setTimestamp(final long timestamp) {
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

	public int getPiRound() {
		return piRound;
	}

	public void setPiRound(final int piRound) {
		this.piRound = piRound;
	}

	public boolean isShowStatistic() {
		return showStatistic;
	}

	public void setShowStatistic(final boolean showStatistic) {
		this.showStatistic = showStatistic;
	}

	public boolean isShowAnswer() {
		return showAnswer;
	}

	public void setShowAnswer(final boolean showAnswer) {
		this.showAnswer = showAnswer;
	}

	public boolean isAbstention() {
		return abstention;
	}

	public void setAbstention(final boolean abstention) {
		this.abstention = abstention;
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

	public String getImage() {
		return image;
	}

	public void setImage(final String image) {
		this.image = image;
	}

	public int getGridSize() {
		return gridSize;
	}

	public void setGridSize(final int gridSize) {
		this.gridSize = gridSize;
	}

	public int getOffsetX() {
		return offsetX;
	}

	public void setOffsetX(final int offsetX) {
		this.offsetX = offsetX;
	}

	public int getOffsetY() {
		return offsetY;
	}

	public void setOffsetY(final int offsetY) {
		this.offsetY = offsetY;
	}

	public int getZoomLvl() {
		return zoomLvl;
	}

	public void setZoomLvl(final int zoomLvl) {
		this.zoomLvl = zoomLvl;
	}
	
	public int getGridOffsetX() {
		return gridOffsetX;
	}

	public void setGridOffsetX(int gridOffsetX) {
		this.gridOffsetX = gridOffsetX;
	}
	
	public int getGridOffsetY() {
		return gridOffsetY;
	}

	public void setGridOffsetY(int gridOffsetY) {
		this.gridOffsetY = gridOffsetY;
	}
	
	public int getGridZoomLvl() {
		return gridZoomLvl;
	}

	public void setGridZoomLvl(int gridZoomLvl) {
		this.gridZoomLvl = gridZoomLvl;
	}
	
	public int getGridSizeX() {
		return gridSizeX;
	}

	public void setGridSizeX(int gridSizeX) {
		this.gridSizeX = gridSizeX;
	}
	
	public int getGridSizeY() {
		return gridSizeY;
	}

	public void setGridSizeY(int gridSizeY) {
		this.gridSizeY = gridSizeY;
	}
	
	public boolean getGridIsHidden() {
		return gridIsHidden;
	}

	public void setGridIsHidden(boolean gridIsHidden) {
		this.gridIsHidden = gridIsHidden;
	}
	
	public int getImgRotation() {
		return imgRotation;
	}

	public void setImgRotation(int imgRotation) {
		this.imgRotation = imgRotation;
	}
	
	public boolean getToggleFieldsLeft() {
		return toggleFieldsLeft;
	}

	public void setToggleFieldsLeft(boolean toggleFieldsLeft) {
		this.toggleFieldsLeft = toggleFieldsLeft;
	}
	
	public int getNumClickableFields() {
		return numClickableFields;
	}

	public void setNumClickableFields(int numClickableFields) {
		this.numClickableFields = numClickableFields;
	}
	
	public int getThresholdCorrectAnswers() {
		return thresholdCorrectAnswers;
	}
	
	public void setThresholdCorrectAnswers(int thresholdCorrectAnswers) {
		this.thresholdCorrectAnswers = thresholdCorrectAnswers;
	}

	@Override
	public final String toString() {
		return "Question type '" + type + "': " + subject + ";\n" + text + possibleAnswers;
	}
}
