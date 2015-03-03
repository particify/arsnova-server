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
	// This property is needed because the client does not have the session ID, only the keyword.
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
	private String fcImage;
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
	private boolean cvIsColored;
	private String gridLineColor;
	private int numberOfDots;
	private String gridType;
	private String scaleFactor;
	private String gridScaleFactor;
    private int sequenceNr;

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

	public boolean getCvIsColored() {
		return cvIsColored;
	}

	public void setCvIsColored(boolean cvIsColored) {
		this.cvIsColored = cvIsColored;
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

	public String getFcImage() {
		return fcImage;
	}

	public void setFcImage(final String fcImage) {
		this.fcImage = fcImage;
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

	public String getGridLineColor() {
		return gridLineColor;
	}

	public void setGridLineColor(String gridLineColor) {
		this.gridLineColor = gridLineColor;
	}

	public int getNumberOfDots() {
		return numberOfDots;
	}

	public void setNumberOfDots(int numberOfDots) {
		this.numberOfDots = numberOfDots;
	}

	public String getGridType() {
		return gridType;
	}

	public void setGridType(String gridType) {
		this.gridType = gridType;
	}

	public void setScaleFactor(String scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	public String getScaleFactor() {
		return this.scaleFactor;
	}

	public void setGridScaleFactor(String scaleFactor) {
		this.gridScaleFactor = scaleFactor;
	}

	public String getGridScaleFactor() {
		return this.gridScaleFactor;
	}
    
    public void setSequenceNr(int sequenceNr) {
        this.sequenceNr = sequenceNr;
    }
    
    public int getSequenceNr() {
        return this.sequenceNr;
    }

	@Override
	public final String toString() {
		return "Question type '" + type + "': " + subject + ";\n" + text + possibleAnswers;
	}

	@Override
	public int hashCode() {
		// auto generated!
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_id == null) ? 0 : _id.hashCode());
		result = prime * result + ((_rev == null) ? 0 : _rev.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		// auto generated!
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) {
			return false;
		}
		Question other = (Question) obj;
		if (_id == null) {
			if (other._id != null) {
				return false;
			}
		} else if (!_id.equals(other._id)) {
			return false;
		}
		if (_rev == null) {
			if (other._rev != null) {
				return false;
			}
		} else if (!_rev.equals(other._rev)) {
			return false;
		}
		return true;
	}

	public int calculateValue(Answer answer) {
		if (answer.isAbstention()) {
			return 0;
		} else if (this.questionType.equals("mc")) {
			return calculateMultipleChoiceValue(answer);
		} else if (this.questionType.equals("grid")) {
			return calculateGridValue(answer);
		} else {
			return calculateRegularValue(answer);
		}
	}

	private int calculateRegularValue(Answer answer) {
		String answerText = answer.getAnswerText();
		for (PossibleAnswer p : this.possibleAnswers) {
			if (answerText.equals(p.getText())) {
				return p.getValue();
			}
		}
		return 0;
	}

	private int calculateGridValue(Answer answer) {
		int value = 0;
		String[] answers = answer.getAnswerText().split(",");
		for (int i = 0; i < answers.length; i++) {
			for (PossibleAnswer p : this.possibleAnswers) {
				if (answers[i].equals(p.getText())) {
					value += p.getValue();
				}
			}
		}
		return value;
	}

	private int calculateMultipleChoiceValue(Answer answer) {
		int value = 0;
		String[] answers = answer.getAnswerText().split(",");
		for (int i = 0; i < this.possibleAnswers.size() && i < answers.length; i++) {
			if (answers[i].equals("1")) {
				PossibleAnswer p = this.possibleAnswers.get(i);
				value += p.getValue();
			}
		}
		return value;
	}
}
