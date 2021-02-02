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

package de.thm.arsnova.model.transport;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.thm.arsnova.model.migration.v2.Answer;
import de.thm.arsnova.model.migration.v2.Comment;
import de.thm.arsnova.model.migration.v2.Content;
import de.thm.arsnova.model.migration.v2.Motd;
import de.thm.arsnova.model.migration.v2.Room;
import de.thm.arsnova.model.migration.v2.RoomFeature;
import de.thm.arsnova.model.serialization.View;

/**
 * This class is used to allow the import and export of a session.
 */
public class ImportExportContainer {

	private ImportExportRoom session;

	private List<ImportExportContent> questions;

	private List<Comment> feedbackQuestions;

	private List<Motd> motds;

	public ImportExportContainer() {
		questions = new ArrayList<>();
		feedbackQuestions = new ArrayList<>();
		motds = new ArrayList<>();
	}

	@JsonView(View.Public.class)
	public ImportExportRoom getSession() {
		return session;
	}

	public void setSession(final ImportExportRoom session) {
		this.session = session;
	}

	@JsonView(View.Public.class)
	public List<ImportExportContent> getQuestions() {
		return questions;
	}

	public void setQuestions(final List<ImportExportContent> questions) {
		this.questions = questions;
	}

	@JsonView(View.Public.class)
	public List<Comment> getFeedbackQuestions() {
		return feedbackQuestions;
	}

	public void setFeedbackQuestions(final List<Comment> feedbackQuestions) {
		this.feedbackQuestions = feedbackQuestions;
	}

	@JsonView(View.Public.class)
	public List<Motd> getMotds() {
		return motds;
	}

	public void setMotds(final List<Motd> motdList) {
		this.motds = motdList;
	}

	@JsonView(View.Public.class)
	public RoomFeature getSessionFeature() {
		return session.sessionFeature;
	}

	public void setSessionFeature(final RoomFeature roomFeature) {
		session.sessionFeature = roomFeature;
	}

	public void setSessionFromSessionObject(final Room s) {
		final ImportExportRoom importExportRoom = new ImportExportRoom();
		importExportRoom.setName(s.getName());
		importExportRoom.setShortName(s.getShortName());
		importExportRoom.setActive(s.isActive());
		final PublicPool p = new PublicPool();
		p.setPpFromSession(s);
		importExportRoom.setPublicPool(p);
		importExportRoom.sessionFeature = s.getFeatures();
		session = importExportRoom;
	}

	public void addQuestionWithAnswers(final Content content, final List<Answer> answerList) {
		final ImportExportContent importExportContent = new ImportExportContent(content);
		importExportContent.setAnswers(answerList);
		questions.add(importExportContent);
	}

	public Room generateSessionEntity() {
		final Room room = new Room();
		// import fields
		room.setActive(session.isActive());
		// overwrite name and shortname
		room.setName(session.getName());
		room.setShortName(session.getShortName());
		// mark as public pool session
		room.setSessionType(session.getSessionType());
		room.setFeatures(session.getSessionFeature());
		if (session.getPublicPool() != null) {
			// set pool fields (which are also used as a session info)
			room.setPpAuthorMail(session.getPublicPool().getPpAuthorMail());
			room.setPpAuthorName(session.getPublicPool().getPpAuthorName());
			room.setPpDescription(session.getPublicPool().getPpDescription());
			room.setPpFaculty(session.getPublicPool().getPpFaculty());
			room.setPpLevel(session.getPublicPool().getPpLevel());
			room.setPpLicense(session.getPublicPool().getPpLicense());
			room.setPpLogo(session.getPublicPool().getPpLogo());
			room.setPpSubject(session.getPublicPool().getPpSubject());
			room.setPpUniversity(session.getPublicPool().getPpUniversity());
		}
		// other fields
		room.setCreationTime(new Date().getTime());
		return room;
	}

	public static class ImportExportContent extends Content {

		private List<Answer> answers;

		public ImportExportContent() {

		}

		public ImportExportContent(final Content content) {
			setQuestionType(content.getQuestionType());
			setQuestionVariant(content.getQuestionVariant());
			setSubject(content.getSubject());
			setText(content.getText());
			setActive(content.isActive());
			setReleasedFor(content.getReleasedFor());
			setPossibleAnswers(content.getPossibleAnswers());
			setNoCorrect(content.isNoCorrect());
			setSessionId(content.getSessionId());
			setSessionKeyword(content.getSessionKeyword());
			setTimestamp(content.getTimestamp());
			setNumber(content.getNumber());
			setDuration(content.getDuration());
			setPiRound(content.getPiRound());
			setPiRoundEndTime(content.getPiRoundEndTime());
			setPiRoundStartTime(content.getPiRoundStartTime());
			setPiRoundFinished(content.isPiRoundFinished());
			setVotingDisabled(content.isVotingDisabled());
			setShowStatistic(content.isShowStatistic());
			setShowAnswer(content.isShowAnswer());
			setAbstention(content.isAbstention());
			setImage(content.getImage());
			setFcImage(content.getFcImage());
			setGridSize(content.getGridSize());
			setOffsetX(content.getOffsetX());
			setOffsetY(content.getOffsetY());
			setZoomLvl(content.getZoomLvl());
			setGridOffsetX(content.getGridOffsetX());
			setGridOffsetY(content.getGridOffsetY());
			setGridZoomLvl(content.getGridZoomLvl());
			setGridSizeX(content.getGridSizeX());
			setGridSizeY(content.getGridSizeY());
			setGridIsHidden(content.getGridIsHidden());
			setImgRotation(content.getImgRotation());
			setToggleFieldsLeft(content.getToggleFieldsLeft());
			setNumClickableFields(content.getNumClickableFields());
			setThresholdCorrectAnswers(content.getThresholdCorrectAnswers());
			setCvIsColored(content.getCvIsColored());
			setGridLineColor(content.getGridLineColor());
			setNumberOfDots(content.getNumberOfDots());
			setGridType(content.getGridType());
			setScaleFactor(content.getScaleFactor());
			setGridScaleFactor(content.getGridScaleFactor());
			setImageQuestion(content.isImageQuestion());
			setTextAnswerEnabled(content.isTextAnswerEnabled());
			setHint(content.getHint());
			setSolution(content.getSolution());
			setCorrectAnswer(content.getCorrectAnswer());
			setFixedAnswer(content.isFixedAnswer());
			setIgnoreCaseSensitive(content.isIgnoreCaseSensitive());
			setIgnorePunctuation(content.isIgnorePunctuation());
			setIgnoreWhitespaces(content.isIgnoreWhitespaces());
			setRating(content.getRating());
		}

		@JsonView(View.Public.class)
		public List<Answer> getAnswers() {
			return answers;
		}

		public void setAnswers(final List<Answer> answers) {
			this.answers = answers;
		}
	}

	public static class ImportExportRoom {

		private String name;

		private String shortName;

		private String sessionType;

		private boolean active;

		private PublicPool publicPool;

		private RoomFeature sessionFeature = new RoomFeature();

		@JsonView(View.Public.class)
		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		@JsonView(View.Public.class)
		public String getSessionType() {
			return sessionType;
		}

		public void setSessionType(final String sessionType) {
			this.sessionType = sessionType;
		}

		@JsonView(View.Public.class)
		public String getShortName() {
			return shortName;
		}

		public void setShortName(final String shortName) {
			this.shortName = shortName;
		}

		@JsonView(View.Public.class)
		public boolean isActive() {
			return active;
		}

		public void setActive(final boolean active) {
			this.active = active;
		}

		@JsonView(View.Public.class)
		public PublicPool getPublicPool() {
			return publicPool;
		}

		public void setPublicPool(final PublicPool publicPool) {
			this.publicPool = publicPool;
		}

		/* Use getSessionFeature() of outer class for public access. */
		private RoomFeature getSessionFeature() {
			return this.sessionFeature;
		}

		public void setSessionFeature(final RoomFeature roomFeature) {
			this.sessionFeature = roomFeature;
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

		public void setPpFromSession(final Room room) {
			ppAuthorName = room.getPpAuthorName();
			ppAuthorMail = room.getPpAuthorMail();
			ppUniversity = room.getPpUniversity();
			ppLogo = room.getPpLogo();
			ppSubject = room.getPpSubject();
			ppLicense = room.getPpLicense();
			ppLevel = room.getPpLevel();
			ppDescription = room.getPpDescription();
			ppFaculty = room.getPpFaculty();
			name = room.getName();
			shortName = room.getShortName();
		}

		@JsonView(View.Public.class)
		public String getPpAuthorName() {
			return ppAuthorName;
		}

		public void setPpAuthorName(final String ppAuthorName) {
			this.ppAuthorName = ppAuthorName;
		}

		@JsonView(View.Public.class)
		public String getPpAuthorMail() {
			return ppAuthorMail;
		}

		public void setPpAuthorMail(final String ppAuthorMail) {
			this.ppAuthorMail = ppAuthorMail;
		}

		@JsonView(View.Public.class)
		public String getPpUniversity() {
			return ppUniversity;
		}

		public void setPpUniversity(final String ppUniversity) {
			this.ppUniversity = ppUniversity;
		}

		@JsonView(View.Public.class)
		public String getPpLogo() {
			return ppLogo;
		}

		public void setPpLogo(final String ppLogo) {
			this.ppLogo = ppLogo;
		}

		@JsonView(View.Public.class)
		public String getPpSubject() {
			return ppSubject;
		}

		public void setPpSubject(final String ppSubject) {
			this.ppSubject = ppSubject;
		}

		@JsonView(View.Public.class)
		public String getPpLicense() {
			return ppLicense;
		}

		public void setPpLicense(final String ppLicense) {
			this.ppLicense = ppLicense;
		}

		@JsonView(View.Public.class)
		public String getPpLevel() {
			return ppLevel;
		}

		public void setPpLevel(final String ppLevel) {
			this.ppLevel = ppLevel;
		}

		@JsonView(View.Public.class)
		public String getPpDescription() {
			return ppDescription;
		}

		public void setPpDescription(final String ppDescription) {
			this.ppDescription = ppDescription;
		}

		@JsonView(View.Public.class)
		public String getPpFaculty() {
			return ppFaculty;
		}

		public void setPpFaculty(final String ppFaculty) {
			this.ppFaculty = ppFaculty;
		}

		@JsonView(View.Public.class)
		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		@JsonView(View.Public.class)
		public String getShortName() {
			return shortName;
		}

		public void setShortName(final String shortName) {
			this.shortName = shortName;
		}
	}
}
