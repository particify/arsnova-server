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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A question the teacher is asking.
 */
@ApiModel(value = "lecturerquestion", description = "the question entity")
public class Question implements Serializable {

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

	@ApiModelProperty(required = true, value = "the type")
	public final String getType() {
		return type;
	}

	public final void setType(final String type) {
		this.type = type;
	}

	@ApiModelProperty(required = true, value = "the question type")
	public final String getQuestionType() {
		return questionType;
	}

	public final void setQuestionType(final String questionType) {
		this.questionType = questionType;
	}

	@ApiModelProperty(required = true, value = "either lecture or preparation")
	public final String getQuestionVariant() {
		return questionVariant;
	}

	public final void setQuestionVariant(final String questionVariant) {
		this.questionVariant = questionVariant;
	}

	@ApiModelProperty(required = true, value = "used to display subject")
	public final String getSubject() {
		return subject;
	}

	public final void setSubject(final String subject) {
		this.subject = subject;
	}

	@ApiModelProperty(required = true, value = "the text")
	public final String getText() {
		return text;
	}

	public final void setText(final String text) {
		this.text = text;
	}

	@ApiModelProperty(required = true, value = "true for active question")
	public final boolean isActive() {
		return active;
	}

	public final void setActive(final boolean active) {
		this.active = active;
	}

	@ApiModelProperty(required = true, value = "deprecated - previously used to limitate the audience")
	public final String getReleasedFor() {
		return releasedFor;
	}

	public final void setReleasedFor(final String releasedFor) {
		this.releasedFor = releasedFor;
	}

	@ApiModelProperty(required = true, value = "list of possible answers")
	public final List<PossibleAnswer> getPossibleAnswers() {
		return possibleAnswers;
	}

	public final void setPossibleAnswers(final List<PossibleAnswer> possibleAnswers) {
		this.possibleAnswers = possibleAnswers;
	}

	@ApiModelProperty(required = true, value = "if true, no answer is marked correct")
	public final boolean isNoCorrect() {
		return noCorrect;
	}

	public final void setNoCorrect(final boolean noCorrect) {
		this.noCorrect = noCorrect;
	}

	@ApiModelProperty(required = true, value = "couchDB ID of the session, the question is assigned to")
	public final String getSessionId() {
		return sessionId;
	}

	public final void setSessionId(final String sessionId) {
		this.sessionId = sessionId;
	}

	@ApiModelProperty(required = true, value = "couchDB ID of the session, the question is assigned to")
	public final String getSession() {
		return sessionId;
	}

	public final void setSession(final String session) {
		sessionId = session;
	}

	@ApiModelProperty(required = true, value = "the session keyword, the question is assigned to")
	public final String getSessionKeyword() {
		return sessionKeyword;
	}

	public final void setSessionKeyword(final String keyword) {
		sessionKeyword = keyword;
	}

	@ApiModelProperty(required = true, value = "creation date timestamp")
	public final long getTimestamp() {
		return timestamp;
	}

	public final void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

	@ApiModelProperty(required = true, value = "used to display number")
	public final int getNumber() {
		return number;
	}

	public final void setNumber(final int number) {
		this.number = number;
	}

	@ApiModelProperty(required = true, value = "used to display duration")
	public final int getDuration() {
		return duration;
	}

	@ApiModelProperty(required = true, value = "true for image question")
	public final boolean isImageQuestion() {
		return imageQuestion;
	}

	public void setImageQuestion(boolean imageQuestion) {
		this.imageQuestion = imageQuestion;
	}

	public final void setDuration(final int duration) {
		this.duration = duration;
	}

	@ApiModelProperty(required = true, value = "the peer instruction round no.")
	public int getPiRound() {
		return piRound;
	}

	public void setPiRound(final int piRound) {
		this.piRound = piRound;
	}

	@ApiModelProperty(required = true, value = "the peer instruction round end timestamp")
	public long getPiRoundEndTime() {
		return piRoundEndTime;
	}

	public void setPiRoundEndTime(long piRoundEndTime) {
		this.piRoundEndTime = piRoundEndTime;
	}

	@ApiModelProperty(required = true, value = "the peer instruction round start timestamp")
	public long getPiRoundStartTime() {
		return piRoundStartTime;
	}

	public void setPiRoundStartTime(long piRoundStartTime) {
		this.piRoundStartTime = piRoundStartTime;
	}

	@ApiModelProperty(required = true, value = "true for active peer instruction round")
	public boolean isPiRoundActive() {
		return piRoundActive;
	}

	public void setPiRoundActive(boolean piRoundActive) {
		this.piRoundActive = piRoundActive;
	}

	@ApiModelProperty(required = true, value = "true for finished peer instruction round")
	public boolean isPiRoundFinished() {
		return piRoundFinished;
	}

	public void setPiRoundFinished(boolean piRoundFinished) {
		this.piRoundFinished = piRoundFinished;
	}

	@ApiModelProperty(required = true, value = "used to display showStatistic")
	public boolean isShowStatistic() {
		return showStatistic;
	}

	public void setShowStatistic(final boolean showStatistic) {
		this.showStatistic = showStatistic;
	}

	@ApiModelProperty(required = true, value = "used to display cvIsColored")
	public boolean getCvIsColored() {
		return cvIsColored;
	}

	public void setCvIsColored(boolean cvIsColored) {
		this.cvIsColored = cvIsColored;
	}

	@ApiModelProperty(required = true, value = "used to display showAnswer")
	public boolean isShowAnswer() {
		return showAnswer;
	}

	public void setShowAnswer(final boolean showAnswer) {
		this.showAnswer = showAnswer;
	}

	@ApiModelProperty(required = true, value = "used to display abstention")
	public boolean isAbstention() {
		return abstention;
	}

	public void setAbstention(final boolean abstention) {
		this.abstention = abstention;
	}

	@ApiModelProperty(required = true, value = "the couchDB ID")
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

	@ApiModelProperty(required = true, value = "the image")
	public String getImage() {
		return image;
	}

	public void setImage(final String image) {
		this.image = image;
	}

	@ApiModelProperty(required = true, value = "the fcImage")
	public String getFcImage() {
		return fcImage;
	}

	public void setFcImage(final String fcImage) {
		this.fcImage = fcImage;
	}

	@ApiModelProperty(required = true, value = "the grid size")
	public int getGridSize() {
		return gridSize;
	}

	public void setGridSize(final int gridSize) {
		this.gridSize = gridSize;
	}

	@ApiModelProperty(required = true, value = "the image X offset")
	public int getOffsetX() {
		return offsetX;
	}

	public void setOffsetX(final int offsetX) {
		this.offsetX = offsetX;
	}

	@ApiModelProperty(required = true, value = "the image Y offset")
	public int getOffsetY() {
		return offsetY;
	}

	public void setOffsetY(final int offsetY) {
		this.offsetY = offsetY;
	}

	@ApiModelProperty(required = true, value = "the image zoom level")
	public int getZoomLvl() {
		return zoomLvl;
	}

	public void setZoomLvl(final int zoomLvl) {
		this.zoomLvl = zoomLvl;
	}

	@ApiModelProperty(required = true, value = "the grid X offset")
	public int getGridOffsetX() {
		return gridOffsetX;
	}

	public void setGridOffsetX(int gridOffsetX) {
		this.gridOffsetX = gridOffsetX;
	}

	@ApiModelProperty(required = true, value = "the grid Y offset")
	public int getGridOffsetY() {
		return gridOffsetY;
	}

	public void setGridOffsetY(int gridOffsetY) {
		this.gridOffsetY = gridOffsetY;
	}

	@ApiModelProperty(required = true, value = "the grid zoom lvl")
	public int getGridZoomLvl() {
		return gridZoomLvl;
	}

	public void setGridZoomLvl(int gridZoomLvl) {
		this.gridZoomLvl = gridZoomLvl;
	}

	@ApiModelProperty(required = true, value = "the grid X size")
	public int getGridSizeX() {
		return gridSizeX;
	}

	public void setGridSizeX(int gridSizeX) {
		this.gridSizeX = gridSizeX;
	}

	@ApiModelProperty(required = true, value = "the grid Y size")
	public int getGridSizeY() {
		return gridSizeY;
	}

	public void setGridSizeY(int gridSizeY) {
		this.gridSizeY = gridSizeY;
	}

	@ApiModelProperty(required = true, value = "true for hidden grid")
	public boolean getGridIsHidden() {
		return gridIsHidden;
	}

	public void setGridIsHidden(boolean gridIsHidden) {
		this.gridIsHidden = gridIsHidden;
	}

	@ApiModelProperty(required = true, value = "the image rotation")
	public int getImgRotation() {
		return imgRotation;
	}

	public void setImgRotation(int imgRotation) {
		this.imgRotation = imgRotation;
	}

	@ApiModelProperty(required = true, value = "the toggled left fields")
	public boolean getToggleFieldsLeft() {
		return toggleFieldsLeft;
	}

	public void setToggleFieldsLeft(boolean toggleFieldsLeft) {
		this.toggleFieldsLeft = toggleFieldsLeft;
	}

	@ApiModelProperty(required = true, value = "the number of clickable fields")
	public int getNumClickableFields() {
		return numClickableFields;
	}

	public void setNumClickableFields(int numClickableFields) {
		this.numClickableFields = numClickableFields;
	}

	@ApiModelProperty(required = true, value = "the threshold of correct answers")
	public int getThresholdCorrectAnswers() {
		return thresholdCorrectAnswers;
	}

	public void setThresholdCorrectAnswers(int thresholdCorrectAnswers) {
		this.thresholdCorrectAnswers = thresholdCorrectAnswers;
	}

	@ApiModelProperty(required = true, value = "the grid line color")
	public String getGridLineColor() {
		return gridLineColor;
	}

	public void setGridLineColor(String gridLineColor) {
		this.gridLineColor = gridLineColor;
	}

	@ApiModelProperty(required = true, value = "the number of dots")
	public int getNumberOfDots() {
		return numberOfDots;
	}

	public void setNumberOfDots(int numberOfDots) {
		this.numberOfDots = numberOfDots;
	}

	@ApiModelProperty(required = true, value = "the grid type")
	public String getGridType() {
		return gridType;
	}

	public void setGridType(String gridType) {
		this.gridType = gridType;
	}

	public void setScaleFactor(String scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	@ApiModelProperty(required = true, value = "the image scale factor")
	public String getScaleFactor() {
		return this.scaleFactor;
	}

	public void setGridScaleFactor(String scaleFactor) {
		this.gridScaleFactor = scaleFactor;
	}

	@ApiModelProperty(required = true, value = "the grid scale factor")
	public String getGridScaleFactor() {
		return this.gridScaleFactor;
	}

	@ApiModelProperty(required = true, value = "true for a question that can be answered via text")
	public boolean isTextAnswerEnabled() {
		return this.textAnswerEnabled;
	}

	public void setTextAnswerEnabled(boolean textAnswerEnabled) {
		this.textAnswerEnabled = textAnswerEnabled;
	}

	@ApiModelProperty(required = true, value = "true for disabled voting")
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
