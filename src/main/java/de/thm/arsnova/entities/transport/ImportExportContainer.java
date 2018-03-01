/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
package de.thm.arsnova.entities.transport;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import de.thm.arsnova.entities.migration.v2.Answer;
import de.thm.arsnova.entities.migration.v2.Comment;
import de.thm.arsnova.entities.migration.v2.Content;
import de.thm.arsnova.entities.migration.v2.Motd;
import de.thm.arsnova.entities.migration.v2.Room;
import de.thm.arsnova.entities.migration.v2.RoomFeature;
import de.thm.arsnova.entities.migration.v2.RoomInfo;
import de.thm.arsnova.entities.serialization.View;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class is used to allow the import and export of a session.
 */
@ApiModel(value = "session/import and export", description = "the import export session API")
public class ImportExportContainer {

	private ImportExportRoom session;

	private List<ImportExportContent> questions;

	private List<Comment> feedbackQuestions;

	private List<Motd> motds;

	private RoomFeature sessionFeature = new RoomFeature();

	private RoomInfo sessionInfo;

	public ImportExportContainer() {
		questions = new ArrayList<>();
		feedbackQuestions = new ArrayList<>();
		motds = new ArrayList<>();
		sessionInfo = null;
	}

	@ApiModelProperty(required = true, value = "used to display session")
	@JsonView(View.Public.class)
	public ImportExportRoom getSession() {
		return session;
	}

	public void setSession(ImportExportRoom session) {
		this.session = session;
	}

	@ApiModelProperty(required = true, value = "used to display questions")
	@JsonView(View.Public.class)
	public List<ImportExportContent> getQuestions() {
		return questions;
	}

	public void setQuestions(List<ImportExportContent> questions) {
		this.questions = questions;
	}

	@ApiModelProperty(required = true, value = "used to display questions feedback")
	@JsonView(View.Public.class)
	public List<Comment> getFeedbackQuestions() {
		return feedbackQuestions;
	}

	public void setFeedbackQuestions(List<Comment> feedbackQuestions) {
		this.feedbackQuestions = feedbackQuestions;
	}

	@JsonView(View.Public.class)
	public List<Motd> getMotds() {
		return motds;
	}

	public void setMotds(List<Motd> mL) {
		this.motds = mL;
	}

	@JsonView(View.Public.class)
	public RoomFeature getSessionFeature() {
		return sessionFeature;
	}

	public void setSessionFeature(RoomFeature sF) {
		sessionFeature = sF;
	}

	@JsonView(View.Public.class)
	public RoomInfo getSessionInfo() {
		return sessionInfo;
	}

	public void setSessionInfo(RoomInfo si) {
		sessionInfo = si;
	}

	public void setSessionFromSessionObject(Room s) {
		ImportExportRoom iesession = new ImportExportRoom();
		iesession.setName(s.getName());
		iesession.setShortName(s.getShortName());
		iesession.setActive(s.isActive());
		PublicPool p = new PublicPool();
		p.setPpFromSession(s);
		iesession.setPublicPool(p);
		sessionFeature = s.getFeatures();
		session = iesession;
	}

	public void addQuestionWithAnswers(Content q, List<Answer> aL) {
		ImportExportContent ieq = new ImportExportContent(q);
		ieq.setAnswers(aL);
		questions.add(ieq);
	}

	public Room generateSessionEntity(ClientAuthentication user) {
		final Room s = new Room();
		// import fields
		s.setActive(session.isActive());
		// overwrite name and shortname
		s.setName(session.getName());
		s.setShortName(session.getShortName());
		// mark as public pool session
		s.setSessionType(session.getSessionType());
		s.setFeatures(session.getSessionFeature());
		if (session.getPublicPool() != null) {
			// set pool fields (which are also used as a session info)
			s.setPpAuthorMail(session.getPublicPool().getPpAuthorMail());
			s.setPpAuthorName(session.getPublicPool().getPpAuthorName());
			s.setPpDescription(session.getPublicPool().getPpDescription());
			s.setPpFaculty(session.getPublicPool().getPpFaculty());
			s.setPpLevel(session.getPublicPool().getPpLevel());
			s.setPpLicense(session.getPublicPool().getPpLicense());
			s.setPpLogo(session.getPublicPool().getPpLogo());
			s.setPpSubject(session.getPublicPool().getPpSubject());
			s.setPpUniversity(session.getPublicPool().getPpUniversity());
		}
		// other fields
		s.setCreator(user.getUsername());
		s.setCreationTime(new Date().getTime());
		return s;
	}

	public static class ImportExportContent extends Content {

		private List<Answer> answers;

		public ImportExportContent() {

		}

		public ImportExportContent(Content q) {
			setQuestionType(q.getQuestionType());
			setQuestionVariant(q.getQuestionVariant());
			setSubject(q.getSubject());
			setText(q.getText());
			setActive(q.isActive());
			setReleasedFor(q.getReleasedFor());
			setPossibleAnswers(q.getPossibleAnswers());
			setNoCorrect(q.isNoCorrect());
			setSessionId(q.getSessionId());
			setSessionKeyword(q.getSessionKeyword());
			setTimestamp(q.getTimestamp());
			setNumber(q.getNumber());
			setDuration(q.getDuration());
			setPiRound(q.getPiRound());
			setPiRoundEndTime(q.getPiRoundEndTime());
			setPiRoundStartTime(q.getPiRoundStartTime());
			setPiRoundFinished(q.isPiRoundFinished());
			setVotingDisabled(q.isVotingDisabled());
			setShowStatistic(q.isShowStatistic());
			setShowAnswer(q.isShowAnswer());
			setAbstention(q.isAbstention());
			setImage(q.getImage());
			setFcImage(q.getFcImage());
			setGridSize(q.getGridSize());
			setOffsetX(q.getOffsetX());
			setOffsetY(q.getOffsetY());
			setZoomLvl(q.getZoomLvl());
			setGridOffsetX(q.getGridOffsetX());
			setGridOffsetY(q.getGridOffsetY());
			setGridZoomLvl(q.getGridZoomLvl());
			setGridSizeX(q.getGridSizeX());
			setGridSizeY(q.getGridSizeY());
			setGridIsHidden(q.getGridIsHidden());
			setImgRotation(q.getImgRotation());
			setToggleFieldsLeft(q.getToggleFieldsLeft());
			setNumClickableFields(q.getNumClickableFields());
			setThresholdCorrectAnswers(q.getThresholdCorrectAnswers());
			setCvIsColored(q.getCvIsColored());
			setGridLineColor(q.getGridLineColor());
			setNumberOfDots(q.getNumberOfDots());
			setGridType(q.getGridType());
			setScaleFactor(q.getScaleFactor());
			setGridScaleFactor(q.getGridScaleFactor());
			setImageQuestion(q.isImageQuestion());
			setTextAnswerEnabled(q.isTextAnswerEnabled());
			setHint(q.getHint());
			setSolution(q.getSolution());
			setCorrectAnswer(q.getCorrectAnswer());
			setFixedAnswer(q.isFixedAnswer());
			setIgnoreCaseSensitive(q.isIgnoreCaseSensitive());
			setIgnorePunctuation(q.isIgnorePunctuation());
			setIgnoreWhitespaces(q.isIgnoreWhitespaces());
			setRating(q.getRating());
		}

		@ApiModelProperty(required = true, value = " used to display answers")
		@JsonView(View.Public.class)
		public List<Answer> getAnswers() {
			return answers;
		}

		public void setAnswers(List<Answer> answers) {
			this.answers = answers;
		}
	}

	public static class ImportExportRoom {

		private String name;

		private String shortName;

		private String sessionType;

		private boolean active;

		private PublicPool publicPool;

		private RoomFeature sessionFeature;

		@ApiModelProperty(required = true, value = "used to display short name")
		@JsonView(View.Public.class)
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ApiModelProperty(required = false, value = "used to identify public pool sessions")
		@JsonView(View.Public.class)
		public String getSessionType() {
			return sessionType;
		}

		public void setSessionType(String sessionType) {
			this.sessionType = sessionType;
		}

		@ApiModelProperty(required = true, value = "used to display short name")
		@JsonView(View.Public.class)
		public String getShortName() {
			return shortName;
		}

		public void setShortName(String shortName) {
			this.shortName = shortName;
		}

		@ApiModelProperty(required = true, value = "active")
		@JsonView(View.Public.class)
		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

		@ApiModelProperty(required = true, value = "used to display public pool")
		@JsonView(View.Public.class)
		public PublicPool getPublicPool() {
			return publicPool;
		}

		public void setPublicPool(PublicPool publicPool) {
			this.publicPool = publicPool;
		}

		@JsonView(View.Public.class)
		public RoomFeature getSessionFeature() {
			return this.sessionFeature;
		}

		public void setSessionFeature(RoomFeature sF) {
			this.sessionFeature = sF;
		}
	}

	public static class PublicPool {

		private String ppAuthorName;

		private String ppAuthorMail;

		private String ppUniversity;

		private String ppLogo;

		private String ppSubject;

		private String ppLicense;

		private String ppLevel;

		private String ppDescription;

		private String ppFaculty;

		private String name;

		private String shortName;

		public void setPpFromSession(Room s) {
			ppAuthorName = s.getPpAuthorName();
			ppAuthorMail = s.getPpAuthorMail();
			ppUniversity = s.getPpUniversity();
			ppLogo = s.getPpLogo();
			ppSubject = s.getPpSubject();
			ppLicense = s.getPpLicense();
			ppLevel = s.getPpLevel();
			ppDescription = s.getPpDescription();
			ppFaculty = s.getPpFaculty();
			name = s.getName();
			shortName = s.getShortName();
		}

		@ApiModelProperty(required = true, value = "used to display author name")
		@JsonView(View.Public.class)
		public String getPpAuthorName() {
			return ppAuthorName;
		}

		public void setPpAuthorName(String ppAuthorName) {
			this.ppAuthorName = ppAuthorName;
		}

		@ApiModelProperty(required = true, value = "used to display author mail")
		@JsonView(View.Public.class)
		public String getPpAuthorMail() {
			return ppAuthorMail;
		}

		public void setPpAuthorMail(String ppAuthorMail) {
			this.ppAuthorMail = ppAuthorMail;
		}

		@ApiModelProperty(required = true, value = "used to display university")
		@JsonView(View.Public.class)
		public String getPpUniversity() {
			return ppUniversity;
		}

		public void setPpUniversity(String ppUniversity) {
			this.ppUniversity = ppUniversity;
		}

		@ApiModelProperty(required = true, value = "used to display logo")
		@JsonView(View.Public.class)
		public String getPpLogo() {
			return ppLogo;
		}

		public void setPpLogo(String ppLogo) {
			this.ppLogo = ppLogo;
		}

		@ApiModelProperty(required = true, value = "used to display subject")
		@JsonView(View.Public.class)
		public String getPpSubject() {
			return ppSubject;
		}

		public void setPpSubject(String ppSubject) {
			this.ppSubject = ppSubject;
		}

		@ApiModelProperty(required = true, value = "used to display license")
		@JsonView(View.Public.class)
		public String getPpLicense() {
			return ppLicense;
		}

		public void setPpLicense(String ppLicense) {
			this.ppLicense = ppLicense;
		}

		@ApiModelProperty(required = true, value = "used to display level")
		@JsonView(View.Public.class)
		public String getPpLevel() {
			return ppLevel;
		}

		public void setPpLevel(String ppLevel) {
			this.ppLevel = ppLevel;
		}

		@ApiModelProperty(required = true, value = "used to display description")
		@JsonView(View.Public.class)
		public String getPpDescription() {
			return ppDescription;
		}

		public void setPpDescription(String ppDescription) {
			this.ppDescription = ppDescription;
		}

		@ApiModelProperty(required = true, value = "used to display faculty")
		@JsonView(View.Public.class)
		public String getPpFaculty() {
			return ppFaculty;
		}

		public void setPpFaculty(String ppFaculty) {
			this.ppFaculty = ppFaculty;
		}

		@ApiModelProperty(required = true, value = "used to display name")
		@JsonView(View.Public.class)
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ApiModelProperty(required = true, value = "used to display short name")
		@JsonView(View.Public.class)
		public String getShortName() {
			return shortName;
		}

		public void setShortName(String shortName) {
			this.shortName = shortName;
		}
	}
}
