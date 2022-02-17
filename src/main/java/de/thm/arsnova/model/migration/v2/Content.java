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
import java.util.ArrayList;
import java.util.List;

import de.thm.arsnova.model.serialization.View;

/**
 * Represents Content (Skill/Lecturer Question) in a Room.
 */
public class Content implements Entity {
	private String id;
	private String rev;
	private String questionType;
	private String questionVariant;
	private String subject;
	private String text;
	private boolean active;
	private String releasedFor;
	private List<AnswerOption> possibleAnswers;
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
	private boolean ignoreCaseSensitive;
	private boolean ignoreWhitespaces;
	private boolean ignorePunctuation;
	private boolean fixedAnswer;
	private boolean strictMode;
	private int rating;
	private String correctAnswer;

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

	@JsonView({View.Persistence.class, View.Public.class})
	public String getId() {
		return id;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setId(final String id) {
		this.id = id;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setRevision(final String rev) {
		this.rev = rev;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getRevision() {
		return rev;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final String getQuestionType() {
		return questionType;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setQuestionType(final String questionType) {
		this.questionType = questionType;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final String getQuestionVariant() {
		return questionVariant;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setQuestionVariant(final String questionVariant) {
		this.questionVariant = questionVariant;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final String getSubject() {
		return subject;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setSubject(final String subject) {
		this.subject = subject;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final String getText() {
		return text;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setText(final String text) {
		this.text = text;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final boolean isActive() {
		return active;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setActive(final boolean active) {
		this.active = active;
	}

	public final String getReleasedFor() {
		return releasedFor;
	}

	public final void setReleasedFor(final String releasedFor) {
		this.releasedFor = releasedFor;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final List<AnswerOption> getPossibleAnswers() {
		return possibleAnswers != null ? possibleAnswers : new ArrayList<>();
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setPossibleAnswers(final List<AnswerOption> possibleAnswers) {
		this.possibleAnswers = possibleAnswers;
	}

	public final boolean isNoCorrect() {
		return noCorrect;
	}

	public final void setNoCorrect(final boolean noCorrect) {
		this.noCorrect = noCorrect;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final String getSessionId() {
		return sessionId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
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

	//@JsonView(View.Public.class)
	public final void setSessionKeyword(final String keyword) {
		sessionKeyword = keyword;
	}

	@JsonView(View.Persistence.class)
	public final long getTimestamp() {
		return timestamp;
	}

	@JsonView(View.Persistence.class)
	public final void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

	public final int getNumber() {
		return number;
	}

	public final void setNumber(final int number) {
		this.number = number;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final int getDuration() {
		return duration;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setDuration(final int duration) {
		this.duration = duration;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final boolean isImageQuestion() {
		return imageQuestion;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setImageQuestion(final boolean imageQuestion) {
		this.imageQuestion = imageQuestion;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getPiRound() {
		return piRound;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPiRound(final int piRound) {
		this.piRound = piRound;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public long getPiRoundEndTime() {
		return piRoundEndTime;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPiRoundEndTime(final long piRoundEndTime) {
		this.piRoundEndTime = piRoundEndTime;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public long getPiRoundStartTime() {
		return piRoundStartTime;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPiRoundStartTime(final long piRoundStartTime) {
		this.piRoundStartTime = piRoundStartTime;
	}

	public boolean isPiRoundActive() {
		return piRoundActive;
	}

	public void setPiRoundActive(final boolean piRoundActive) {
		this.piRoundActive = piRoundActive;
	}

	public boolean isPiRoundFinished() {
		return piRoundFinished;
	}

	public void setPiRoundFinished(final boolean piRoundFinished) {
		this.piRoundFinished = piRoundFinished;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isShowStatistic() {
		return showStatistic;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setShowStatistic(final boolean showStatistic) {
		this.showStatistic = showStatistic;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean getCvIsColored() {
		return cvIsColored;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setCvIsColored(final boolean cvIsColored) {
		this.cvIsColored = cvIsColored;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isShowAnswer() {
		return showAnswer;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setShowAnswer(final boolean showAnswer) {
		this.showAnswer = showAnswer;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isAbstention() {
		return abstention;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAbstention(final boolean abstention) {
		this.abstention = abstention;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isIgnoreCaseSensitive() {
		return ignoreCaseSensitive;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setIgnoreCaseSensitive(final boolean ignoreCaseSensitive) {
		this.ignoreCaseSensitive = ignoreCaseSensitive;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isIgnoreWhitespaces() {
		return ignoreWhitespaces;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setIgnoreWhitespaces(final boolean ignoreWhitespaces) {
		this.ignoreWhitespaces = ignoreWhitespaces;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isIgnorePunctuation() {
		return ignorePunctuation;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setIgnorePunctuation(final boolean ignorePunctuation) {
		this.ignorePunctuation = ignorePunctuation;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isFixedAnswer() {
		return this.fixedAnswer;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setFixedAnswer(final boolean fixedAnswer) {
		this.fixedAnswer = fixedAnswer;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isStrictMode() {
		return this.strictMode;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setStrictMode(final boolean strictMode) {
		this.strictMode = strictMode;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final int getRating() {
		return this.rating;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setRating(final int rating) {
		this.rating = rating;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final String getCorrectAnswer() {
		return correctAnswer;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setCorrectAnswer(final String correctAnswer) {
		this.correctAnswer = correctAnswer;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getImage() {
		return image;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setImage(final String image) {
		this.image = image;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getFcImage() {
		return fcImage;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setFcImage(final String fcImage) {
		this.fcImage = fcImage;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getGridSize() {
		return gridSize;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridSize(final int gridSize) {
		this.gridSize = gridSize;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getOffsetX() {
		return offsetX;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setOffsetX(final int offsetX) {
		this.offsetX = offsetX;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getOffsetY() {
		return offsetY;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setOffsetY(final int offsetY) {
		this.offsetY = offsetY;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getZoomLvl() {
		return zoomLvl;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setZoomLvl(final int zoomLvl) {
		this.zoomLvl = zoomLvl;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getGridOffsetX() {
		return gridOffsetX;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridOffsetX(final int gridOffsetX) {
		this.gridOffsetX = gridOffsetX;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getGridOffsetY() {
		return gridOffsetY;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridOffsetY(final int gridOffsetY) {
		this.gridOffsetY = gridOffsetY;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getGridZoomLvl() {
		return gridZoomLvl;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridZoomLvl(final int gridZoomLvl) {
		this.gridZoomLvl = gridZoomLvl;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getGridSizeX() {
		return gridSizeX;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridSizeX(final int gridSizeX) {
		this.gridSizeX = gridSizeX;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getGridSizeY() {
		return gridSizeY;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridSizeY(final int gridSizeY) {
		this.gridSizeY = gridSizeY;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean getGridIsHidden() {
		return gridIsHidden;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridIsHidden(final boolean gridIsHidden) {
		this.gridIsHidden = gridIsHidden;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getImgRotation() {
		return imgRotation;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setImgRotation(final int imgRotation) {
		this.imgRotation = imgRotation;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean getToggleFieldsLeft() {
		return toggleFieldsLeft;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setToggleFieldsLeft(final boolean toggleFieldsLeft) {
		this.toggleFieldsLeft = toggleFieldsLeft;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getNumClickableFields() {
		return numClickableFields;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setNumClickableFields(final int numClickableFields) {
		this.numClickableFields = numClickableFields;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getThresholdCorrectAnswers() {
		return thresholdCorrectAnswers;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setThresholdCorrectAnswers(final int thresholdCorrectAnswers) {
		this.thresholdCorrectAnswers = thresholdCorrectAnswers;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getGridLineColor() {
		return gridLineColor;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridLineColor(final String gridLineColor) {
		this.gridLineColor = gridLineColor;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getNumberOfDots() {
		return numberOfDots;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setNumberOfDots(final int numberOfDots) {
		this.numberOfDots = numberOfDots;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getGridType() {
		return gridType;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridType(final String gridType) {
		this.gridType = gridType;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setScaleFactor(final String scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getScaleFactor() {
		return this.scaleFactor;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridScaleFactor(final String scaleFactor) {
		this.gridScaleFactor = scaleFactor;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getGridScaleFactor() {
		return this.gridScaleFactor;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isTextAnswerEnabled() {
		return this.textAnswerEnabled;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setTextAnswerEnabled(final boolean textAnswerEnabled) {
		this.textAnswerEnabled = textAnswerEnabled;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isVotingDisabled() {
		return votingDisabled;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setVotingDisabled(final boolean votingDisabled) {
		this.votingDisabled = votingDisabled;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getHint() {
		return hint;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setHint(final String hint) {
		this.hint = hint;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getSolution() {
		return solution;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSolution(final String solution) {
		this.solution = solution;
	}

	@Override
	public final String toString() {
		return "Content type '" + questionType + "': " + subject + ";\n" + text + possibleAnswers;
	}

	@Override
	public int hashCode() {
		// auto generated!
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		// auto generated!
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Content other = (Content) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}
}
