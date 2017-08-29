/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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
package de.thm.arsnova.entities.migration.v2;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.Entity;
import de.thm.arsnova.entities.serialization.View;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.List;

/**
 * A question the teacher is asking.
 */
@ApiModel(value = "content", description = "the content entity")
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

	@ApiModelProperty(required = true, value = "the couchDB ID")
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

	@ApiModelProperty(required = true, value = "the question type")
	@JsonView({View.Persistence.class, View.Public.class})
	public final String getQuestionType() {
		return questionType;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setQuestionType(final String questionType) {
		this.questionType = questionType;
	}

	@ApiModelProperty(required = true, value = "either lecture or preparation")
	@JsonView({View.Persistence.class, View.Public.class})
	public final String getQuestionVariant() {
		return questionVariant;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setQuestionVariant(final String questionVariant) {
		this.questionVariant = questionVariant;
	}

	@ApiModelProperty(required = true, value = "used to display subject")
	@JsonView({View.Persistence.class, View.Public.class})
	public final String getSubject() {
		return subject;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setSubject(final String subject) {
		this.subject = subject;
	}

	@ApiModelProperty(required = true, value = "the text")
	@JsonView({View.Persistence.class, View.Public.class})
	public final String getText() {
		return text;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setText(final String text) {
		this.text = text;
	}

	@ApiModelProperty(required = true, value = "true for active question")
	@JsonView({View.Persistence.class, View.Public.class})
	public final boolean isActive() {
		return active;
	}

	@JsonView({View.Persistence.class, View.Public.class})
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
	@JsonView({View.Persistence.class, View.Public.class})
	public final List<AnswerOption> getPossibleAnswers() {
		return possibleAnswers;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setPossibleAnswers(final List<AnswerOption> possibleAnswers) {
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
	@JsonView({View.Persistence.class, View.Public.class})
	public final String getSessionId() {
		return sessionId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
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
	@JsonView(View.Persistence.class)
	public final long getTimestamp() {
		return timestamp;
	}

	@JsonView(View.Persistence.class)
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
	@JsonView({View.Persistence.class, View.Public.class})
	public final int getDuration() {
		return duration;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public final void setDuration(final int duration) {
		this.duration = duration;
	}

	@ApiModelProperty(required = true, value = "true for image question")
	@JsonView({View.Persistence.class, View.Public.class})
	public final boolean isImageQuestion() {
		return imageQuestion;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setImageQuestion(boolean imageQuestion) {
		this.imageQuestion = imageQuestion;
	}

	@ApiModelProperty(required = true, value = "the peer instruction round no.")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getPiRound() {
		return piRound;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPiRound(final int piRound) {
		this.piRound = piRound;
	}

	@ApiModelProperty(required = true, value = "the peer instruction round end timestamp")
	@JsonView({View.Persistence.class, View.Public.class})
	public long getPiRoundEndTime() {
		return piRoundEndTime;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPiRoundEndTime(long piRoundEndTime) {
		this.piRoundEndTime = piRoundEndTime;
	}

	@ApiModelProperty(required = true, value = "the peer instruction round start timestamp")
	@JsonView({View.Persistence.class, View.Public.class})
	public long getPiRoundStartTime() {
		return piRoundStartTime;
	}

	@JsonView({View.Persistence.class, View.Public.class})
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
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isShowStatistic() {
		return showStatistic;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setShowStatistic(final boolean showStatistic) {
		this.showStatistic = showStatistic;
	}

	@ApiModelProperty(required = true, value = "used to display cvIsColored")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean getCvIsColored() {
		return cvIsColored;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setCvIsColored(boolean cvIsColored) {
		this.cvIsColored = cvIsColored;
	}

	@ApiModelProperty(required = true, value = "used to display showAnswer")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isShowAnswer() {
		return showAnswer;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setShowAnswer(final boolean showAnswer) {
		this.showAnswer = showAnswer;
	}

	@ApiModelProperty(required = true, value = "used to display abstention")
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

	@ApiModelProperty(required = true, value = "the image")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getImage() {
		return image;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setImage(final String image) {
		this.image = image;
	}

	@ApiModelProperty(required = true, value = "the fcImage")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getFcImage() {
		return fcImage;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setFcImage(final String fcImage) {
		this.fcImage = fcImage;
	}

	@ApiModelProperty(required = true, value = "the grid size")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getGridSize() {
		return gridSize;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridSize(final int gridSize) {
		this.gridSize = gridSize;
	}

	@ApiModelProperty(required = true, value = "the image X offset")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getOffsetX() {
		return offsetX;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setOffsetX(final int offsetX) {
		this.offsetX = offsetX;
	}

	@ApiModelProperty(required = true, value = "the image Y offset")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getOffsetY() {
		return offsetY;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setOffsetY(final int offsetY) {
		this.offsetY = offsetY;
	}

	@ApiModelProperty(required = true, value = "the image zoom level")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getZoomLvl() {
		return zoomLvl;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setZoomLvl(final int zoomLvl) {
		this.zoomLvl = zoomLvl;
	}

	@ApiModelProperty(required = true, value = "the grid X offset")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getGridOffsetX() {
		return gridOffsetX;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridOffsetX(int gridOffsetX) {
		this.gridOffsetX = gridOffsetX;
	}

	@ApiModelProperty(required = true, value = "the grid Y offset")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getGridOffsetY() {
		return gridOffsetY;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridOffsetY(int gridOffsetY) {
		this.gridOffsetY = gridOffsetY;
	}

	@ApiModelProperty(required = true, value = "the grid zoom lvl")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getGridZoomLvl() {
		return gridZoomLvl;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridZoomLvl(int gridZoomLvl) {
		this.gridZoomLvl = gridZoomLvl;
	}

	@ApiModelProperty(required = true, value = "the grid X size")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getGridSizeX() {
		return gridSizeX;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridSizeX(int gridSizeX) {
		this.gridSizeX = gridSizeX;
	}

	@ApiModelProperty(required = true, value = "the grid Y size")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getGridSizeY() {
		return gridSizeY;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridSizeY(int gridSizeY) {
		this.gridSizeY = gridSizeY;
	}

	@ApiModelProperty(required = true, value = "true for hidden grid")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean getGridIsHidden() {
		return gridIsHidden;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridIsHidden(boolean gridIsHidden) {
		this.gridIsHidden = gridIsHidden;
	}

	@ApiModelProperty(required = true, value = "the image rotation")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getImgRotation() {
		return imgRotation;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setImgRotation(int imgRotation) {
		this.imgRotation = imgRotation;
	}

	@ApiModelProperty(required = true, value = "the toggled left fields")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean getToggleFieldsLeft() {
		return toggleFieldsLeft;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setToggleFieldsLeft(boolean toggleFieldsLeft) {
		this.toggleFieldsLeft = toggleFieldsLeft;
	}

	@ApiModelProperty(required = true, value = "the number of clickable fields")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getNumClickableFields() {
		return numClickableFields;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setNumClickableFields(int numClickableFields) {
		this.numClickableFields = numClickableFields;
	}

	@ApiModelProperty(required = true, value = "the threshold of correct answers")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getThresholdCorrectAnswers() {
		return thresholdCorrectAnswers;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setThresholdCorrectAnswers(int thresholdCorrectAnswers) {
		this.thresholdCorrectAnswers = thresholdCorrectAnswers;
	}

	@ApiModelProperty(required = true, value = "the grid line color")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getGridLineColor() {
		return gridLineColor;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridLineColor(String gridLineColor) {
		this.gridLineColor = gridLineColor;
	}

	@ApiModelProperty(required = true, value = "the number of dots")
	@JsonView({View.Persistence.class, View.Public.class})
	public int getNumberOfDots() {
		return numberOfDots;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setNumberOfDots(int numberOfDots) {
		this.numberOfDots = numberOfDots;
	}

	@ApiModelProperty(required = true, value = "the grid type")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getGridType() {
		return gridType;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridType(String gridType) {
		this.gridType = gridType;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setScaleFactor(String scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	@ApiModelProperty(required = true, value = "the image scale factor")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getScaleFactor() {
		return this.scaleFactor;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setGridScaleFactor(String scaleFactor) {
		this.gridScaleFactor = scaleFactor;
	}

	@ApiModelProperty(required = true, value = "the grid scale factor")
	@JsonView({View.Persistence.class, View.Public.class})
	public String getGridScaleFactor() {
		return this.gridScaleFactor;
	}

	@ApiModelProperty(required = true, value = "true for a question that can be answered via text")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isTextAnswerEnabled() {
		return this.textAnswerEnabled;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setTextAnswerEnabled(boolean textAnswerEnabled) {
		this.textAnswerEnabled = textAnswerEnabled;
	}

	@ApiModelProperty(required = true, value = "true for disabled voting")
	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isVotingDisabled() {
		return votingDisabled;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setVotingDisabled(boolean votingDisabled) {
		this.votingDisabled = votingDisabled;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getHint() {
		return hint;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setHint(String hint) {
		this.hint = hint;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getSolution() {
		return solution;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSolution(String solution) {
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
	public boolean equals(Object obj) {
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
		Content other = (Content) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	public int calculateValue(Answer answer) {
		if (answer.isAbstention()) {
			return 0;
		} else if ("mc".equals(this.questionType)) {
			return calculateMultipleChoiceValue(answer);
		} else if ("grid".equals(this.questionType)) {
			return calculateGridValue(answer);
		} else {
			return calculateRegularValue(answer);
		}
	}

	public String checkCaseSensitive(String answerText) {
		if (this.isIgnoreCaseSensitive()) {
			this.setCorrectAnswer(this.getCorrectAnswer().toLowerCase());
			return answerText.toLowerCase();
		}
		return answerText;
	}
	public String checkWhitespaces(String answerText) {
		if (this.isIgnoreWhitespaces()) {
			this.setCorrectAnswer(this.getCorrectAnswer().replaceAll("[\\s]", ""));
			return answerText.replaceAll("[\\s]", "");
		}
		return answerText;
	}
	public String checkPunctuation(String answerText) {
		if (this.isIgnorePunctuation()) {
			this.setCorrectAnswer(this.getCorrectAnswer().replaceAll("\\p{Punct}", ""));
			return answerText.replaceAll("\\p{Punct}", "");
		}
		return answerText;
	}

	public void checkTextStrictOptions(Answer answer) {
		answer.setAnswerTextRaw(this.checkCaseSensitive(answer.getAnswerTextRaw()));
		answer.setAnswerTextRaw(this.checkPunctuation(answer.getAnswerTextRaw()));
		answer.setAnswerTextRaw(this.checkWhitespaces(answer.getAnswerTextRaw()));
	}

	public int evaluateCorrectAnswerFixedText(String answerTextRaw) {
		if (answerTextRaw != null) {
			if (answerTextRaw.equals(this.getCorrectAnswer())) {
				return this.getRating();
			}
		}
		return 0;
	}

	public boolean isSuccessfulFreeTextAnswer(String answerTextRaw) {
		return answerTextRaw != null && answerTextRaw.equals(this.getCorrectAnswer());
	}

	public void updateRoundStartVariables(Date start, Date end) {
		if (this.getPiRound() == 1 && this.isPiRoundFinished()) {
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

		if (time > this.getPiRoundEndTime() && this.isPiRoundActive()) {
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
		this.setPiRoundEndTime(0);
		this.setPiRoundStartTime(0);
		this.setPiRoundActive(false);
		this.setPiRoundFinished(false);
		this.setVotingDisabled(false);

		if ("freetext".equals(this.getQuestionType())) {
			this.setPiRound(0);
		} else {
			this.setPiRound(1);
		}
	}

	private int calculateRegularValue(Answer answer) {
		String answerText = answer.getAnswerText();
		for (AnswerOption p : this.possibleAnswers) {
			if (answerText.equals(p.getText())) {
				return p.getValue();
			}
		}
		return 0;
	}

	private int calculateGridValue(Answer answer) {
		int value = 0;
		String[] answers = answer.getAnswerText().split(",");
		for (String a : answers) {
			for (AnswerOption p : this.possibleAnswers) {
				if (a.equals(p.getText())) {
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
			if ("1".equals(answers[i])) {
				AnswerOption p = this.possibleAnswers.get(i);
				value += p.getValue();
			}
		}
		return value;
	}
}
