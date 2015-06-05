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

import java.util.Date;
import java.util.List;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * A question the teacher is asking.
 */
@ApiModel( value = "lecturerquestion" , description = "the Question API")
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
	private long piRoundEndTime = 0;
	private long piRoundStartTime = 0;
	private boolean piRoundFinished = false;
	private boolean piRoundActive = false;
	private boolean votingDisabled;
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
	private boolean imageQuestion;
	private boolean textAnswerEnabled;
	private String hint;
	private String solution;

	@ApiModelProperty(position = 1, required = true, value = "used to display type")
	public final String getType() {
		return type;
	}

	public final void setType(final String type) {
		this.type = type;
	}

	@ApiModelProperty(position = 2, required = true, value = "used to display the question type")
	public final String getQuestionType() {
		return questionType;
	}

	public final void setQuestionType(final String questionType) {
		this.questionType = questionType;
	}

	@ApiModelProperty(position = 3, required = true, value = "used to display question variant")
	public final String getQuestionVariant() {
		return questionVariant;
	}

	public final void setQuestionVariant(final String questionVariant) {
		this.questionVariant = questionVariant;
	}
	@ApiModelProperty(position = 4, required = true, value = "used to display subject")
	public final String getSubject() {
		return subject;
	}

	public final void setSubject(final String subject) {
		this.subject = subject;
	}

	@ApiModelProperty(position = 5, required = true, value = "used to display text")
	public final String getText() {
		return text;
	}

	public final void setText(final String text) {
		this.text = text;
	}

	@ApiModelProperty(position = 6, required = true, value = "active")
	public final boolean isActive() {
		return active;
	}

	public final void setActive(final boolean active) {
		this.active = active;
	}

	@ApiModelProperty(position = 7, required = true, value = "used to display released for")
	public final String getReleasedFor() {
		return releasedFor;
	}

	public final void setReleasedFor(final String releasedFor) {
		this.releasedFor = releasedFor;
	}

	@ApiModelProperty(position = 8, required = true, value = "used to display possible answers")
	public final List<PossibleAnswer> getPossibleAnswers() {
		return possibleAnswers;
	}

	public final void setPossibleAnswers(final List<PossibleAnswer> possibleAnswers) {
		this.possibleAnswers = possibleAnswers;
	}

	@ApiModelProperty(position = 9, required = true, value = "no correct")
	public final boolean isNoCorrect() {
		return noCorrect;
	}

	public final void setNoCorrect(final boolean noCorrect) {
		this.noCorrect = noCorrect;
	}

	@ApiModelProperty(position = 10, required = true, value = "used to display session id")
	public final String getSessionId() {
		return sessionId;
	}

	public final void setSessionId(final String sessionId) {
		this.sessionId = sessionId;
	}

	@ApiModelProperty(position = 11, required = true, value = "used to display session id")
	public final String getSession() {
		return sessionId;
	}

	public final void setSession(final String session) {
		sessionId = session;
	}

	@ApiModelProperty(position = 12, required = true, value = "used to display session keyword")
	public final String getSessionKeyword() {
		return sessionKeyword;
	}

	public final void setSessionKeyword(final String keyword) {
		sessionKeyword = keyword;
	}

	@ApiModelProperty(position = 13, required = true, value = "used to display timestamp")
	public final long getTimestamp() {
		return timestamp;
	}

	public final void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

	@ApiModelProperty(position = 14, required = true, value = "used to display number")
	public final int getNumber() {
		return number;
	}

	public final void setNumber(final int number) {
		this.number = number;
	}

	@ApiModelProperty(position = 15, required = true, value = "used to display duration")
	public final int getDuration() {
		return duration;
	}

	@ApiModelProperty(position = 16, required = true, value = "used to display image Question")
	public final boolean isImageQuestion() {
		return imageQuestion;
	}

	public void setImageQuestion(boolean imageQuestion) {
		this.imageQuestion = imageQuestion;
	}

	public final void setDuration(final int duration) {
		this.duration = duration;
	}

	@ApiModelProperty(position = 17, required = true, value = "used to display pi Round")
	public int getPiRound() {
		return piRound;
	}

	public void setPiRound(final int piRound) {
		this.piRound = piRound;
	}

	@ApiModelProperty(position = 18, required = true, value = "used to display pi round end time")
	public long getPiRoundEndTime() {
		return piRoundEndTime;
	}

	public void setPiRoundEndTime(long piRoundEndTime) {
		this.piRoundEndTime = piRoundEndTime;
	}

	@ApiModelProperty(position = 19, required = true, value = "used to display pi round start time")
	public long getPiRoundStartTime() {
		return piRoundStartTime;
	}

	public void setPiRoundStartTime(long piRoundStartTime) {
		this.piRoundStartTime = piRoundStartTime;
	}

	@ApiModelProperty(position = 20, required = true, value = "piRoundActive ")
	public boolean isPiRoundActive() {
		return piRoundActive;
	}

	public void setPiRoundActive(boolean piRoundActive) {
		this.piRoundActive = piRoundActive;
	}

	@ApiModelProperty(position = 21, required = true, value = "piRoundFinished ")
	public boolean isPiRoundFinished() {
		return piRoundFinished;
	}

	public void setPiRoundFinished(boolean piRoundFinished) {
		this.piRoundFinished = piRoundFinished;
	}

	@ApiModelProperty(position = 22, required = true, value = "used to display showStatistic")
	public boolean isShowStatistic() {
		return showStatistic;
	}

	public void setShowStatistic(final boolean showStatistic) {
		this.showStatistic = showStatistic;
	}

	@ApiModelProperty(position = 23, required = true, value = "used to display cvIsColored")
	public boolean getCvIsColored() {
		return cvIsColored;
	}

	public void setCvIsColored(boolean cvIsColored) {
		this.cvIsColored = cvIsColored;
	}

	@ApiModelProperty(position = 24, required = true, value = "used to display showAnswer")
	public boolean isShowAnswer() {
		return showAnswer;
	}

	public void setShowAnswer(final boolean showAnswer) {
		this.showAnswer = showAnswer;
	}

	@ApiModelProperty(position = 25, required = true, value = "used to display abstention")
	public boolean isAbstention() {
		return abstention;
	}

	public void setAbstention(final boolean abstention) {
		this.abstention = abstention;
	}

	@ApiModelProperty(position = 26, required = true, value = "used to display _id")
	public final String get_id() {
		return _id;
	}

	public final void set_id(final String _id) {
		this._id = _id;
	}

	@ApiModelProperty(position = 27, required = true, value = "used to display _rev")
	public final String get_rev() {
		return _rev;
	}

	public final void set_rev(final String _rev) {
		this._rev = _rev;
	}

	@ApiModelProperty(position = 28, required = true, value = "used to display image")
	public String getImage() {
		return image;
	}

	public void setImage(final String image) {
		this.image = image;
	}

	@ApiModelProperty(position = 29, required = true, value = "used to display fcImage")
	public String getFcImage() {
		return fcImage;
	}

	public void setFcImage(final String fcImage) {
		this.fcImage = fcImage;
	}

	@ApiModelProperty(position = 30, required = true, value = "used to display gridSize")
	public int getGridSize() {
		return gridSize;
	}

	public void setGridSize(final int gridSize) {
		this.gridSize = gridSize;
	}

	@ApiModelProperty(position = 31, required = true, value = "used to display offsetX")
	public int getOffsetX() {
		return offsetX;
	}

	public void setOffsetX(final int offsetX) {
		this.offsetX = offsetX;
	}

	@ApiModelProperty(position = 32, required = true, value = "used to display offsetY")
	public int getOffsetY() {
		return offsetY;
	}

	public void setOffsetY(final int offsetY) {
		this.offsetY = offsetY;
	}

	@ApiModelProperty(position = 33, required = true, value = "used to display zoomLvl")
	public int getZoomLvl() {
		return zoomLvl;
	}

	public void setZoomLvl(final int zoomLvl) {
		this.zoomLvl = zoomLvl;
	}

	@ApiModelProperty(position = 34, required = true, value = "used to display gridOffsetX")
	public int getGridOffsetX() {
		return gridOffsetX;
	}

	public void setGridOffsetX(int gridOffsetX) {
		this.gridOffsetX = gridOffsetX;
	}

	@ApiModelProperty(position = 35, required = true, value = "used to display gridOffsetY")
	public int getGridOffsetY() {
		return gridOffsetY;
	}

	public void setGridOffsetY(int gridOffsetY) {
		this.gridOffsetY = gridOffsetY;
	}

	@ApiModelProperty(position = 36, required = true, value = "used to display  grid zoom lvl")
	public int getGridZoomLvl() {
		return gridZoomLvl;
	}

	public void setGridZoomLvl(int gridZoomLvl) {
		this.gridZoomLvl = gridZoomLvl;
	}

	@ApiModelProperty(position = 37, required = true, value = "used to display grid size X")
	public int getGridSizeX() {
		return gridSizeX;
	}

	public void setGridSizeX(int gridSizeX) {
		this.gridSizeX = gridSizeX;
	}

	@ApiModelProperty(position = 38, required = true, value = "used to display grid size Y ")
	public int getGridSizeY() {
		return gridSizeY;
	}

	public void setGridSizeY(int gridSizeY) {
		this.gridSizeY = gridSizeY;
	}

	@ApiModelProperty(position = 39, required = true, value = "grid is hidden")
	public boolean getGridIsHidden() {
		return gridIsHidden;
	}

	public void setGridIsHidden(boolean gridIsHidden) {
		this.gridIsHidden = gridIsHidden;
	}

	@ApiModelProperty(position = 40, required = true, value = "used to display image rotation")
	public int getImgRotation() {
		return imgRotation;
	}

	public void setImgRotation(int imgRotation) {
		this.imgRotation = imgRotation;
	}

	@ApiModelProperty(position = 41, required = true, value = "used to display toggel fields left")
	public boolean getToggleFieldsLeft() {
		return toggleFieldsLeft;
	}

	public void setToggleFieldsLeft(boolean toggleFieldsLeft) {
		this.toggleFieldsLeft = toggleFieldsLeft;
	}

	@ApiModelProperty(position = 42, required = true, value = "used to display number clickable fields")
	public int getNumClickableFields() {
		return numClickableFields;
	}

	public void setNumClickableFields(int numClickableFields) {
		this.numClickableFields = numClickableFields;
	}

	@ApiModelProperty(position = 43, required = true, value = "used to display threshold correct answers")
	public int getThresholdCorrectAnswers() {
		return thresholdCorrectAnswers;
	}

	public void setThresholdCorrectAnswers(int thresholdCorrectAnswers) {
		this.thresholdCorrectAnswers = thresholdCorrectAnswers;
	}

	@ApiModelProperty(position = 44, required = true, value = "used to display  grid line color")
	public String getGridLineColor() {
		return gridLineColor;
	}

	public void setGridLineColor(String gridLineColor) {
		this.gridLineColor = gridLineColor;
	}

	@ApiModelProperty(position = 45, required = true, value = "used to display  number of dots")
	public int getNumberOfDots() {
		return numberOfDots;
	}

	public void setNumberOfDots(int numberOfDots) {
		this.numberOfDots = numberOfDots;
	}

	@ApiModelProperty(position = 46, required = true, value = "used to display the grid type")
	public String getGridType() {
		return gridType;
	}

	public void setGridType(String gridType) {
		this.gridType = gridType;
	}

	public void setScaleFactor(String scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	@ApiModelProperty(position = 47, required = true, value = "used to display scale factor")
	public String getScaleFactor() {
		return this.scaleFactor;
	}

	public void setGridScaleFactor(String scaleFactor) {
		this.gridScaleFactor = scaleFactor;
	}

	@ApiModelProperty(position = 48, required = true, value = "used to display grid scale factor")
	public String getGridScaleFactor() {
		return this.gridScaleFactor;
	}

	@ApiModelProperty(position = 49, required = true, value = "enabled text nswer")
	public boolean isTextAnswerEnabled() {
		return this.textAnswerEnabled;
	}

	public void setTextAnswerEnabled(boolean textAnswerEnabled) {
		this.textAnswerEnabled = textAnswerEnabled;
	}

	@ApiModelProperty(position = 50, required = true, value = "voting disabled")
	public boolean isVotingDisabled() {
		return votingDisabled;
	}

	public void setVotingDisabled(boolean votingDisabled) {
		this.votingDisabled = votingDisabled;
	}

	public String getHint() {
		return hint;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	public String getSolution() {
		return solution;
	}

	public void setSolution(String solution) {
		this.solution = solution;
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

	public void updateRoundStartVariables(Date start, Date end) {
		if(this.getPiRound() == 1 && this.isPiRoundFinished()) {
			this.setPiRound(2);
		}

		this.setActive(true);
		this.setShowAnswer(false);
		this.setPiRoundActive(true);
		this.setShowStatistic(false);
		this.setVotingDisabled(false);
		this.setPiRoundFinished(false);
		this.setPiRoundStartTime(start.getTime());
		this.setPiRoundEndTime(end.getTime());
	}

	public void updateRoundManagementState() {
		final long time = new Date().getTime();

		if(time > this.getPiRoundEndTime() && this.isPiRoundActive()) {
			this.setPiRoundEndTime(0);
			this.setPiRoundStartTime(0);
			this.setPiRoundActive(false);
			this.setPiRoundFinished(true);
		}
	}

	public void resetRoundManagementState() {
		this.setPiRoundEndTime(0);
		this.setPiRoundStartTime(0);
		this.setVotingDisabled(true);
		this.setPiRoundActive(false);
		this.setPiRoundFinished(false);
		this.setShowStatistic(false);
		this.setShowAnswer(false);
	}

	public void resetQuestionState() {
		this.setPiRound(1);
		this.setPiRoundEndTime(0);
		this.setPiRoundStartTime(0);
		this.setPiRoundActive(false);
		this.setPiRoundFinished(false);
		this.setVotingDisabled(false);
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
