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
package de.thm.arsnova.entities.transport;

import java.util.Date;
import java.util.List;

import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

/**
 * This class is used to allow the import and export of a session.
 */
public class ImportExportSession {

	private ImportExportSesssion session;

	private List<ImportExportQuestion> questions;

	private List<InterposedQuestion> feedbackQuestions;

	public ImportExportSesssion getSession() {
		return session;
	}

	public void setSession(ImportExportSesssion session) {
		this.session = session;
	}

	public List<ImportExportQuestion> getQuestions() {
		return questions;
	}

	public void setQuestions(List<ImportExportQuestion> questions) {
		this.questions = questions;
	}

	public List<InterposedQuestion> getFeedbackQuestions() {
		return feedbackQuestions;
	}

	public void setFeedbackQuestions(List<InterposedQuestion> feedbackQuestions) {
		this.feedbackQuestions = feedbackQuestions;
	}

	public Session generateSessionEntity(User user) {
		final Session s = new Session();
		// import fields
		s.setActive(session.isActive());
		s.setName(session.getName());
		s.setShortName(session.getShortName());
		// public pool
		if (session.getPublicPool() != null) {
			// overwrite name and shortname
			s.setName(session.getPublicPool().getName());
			s.setShortName(session.getPublicPool().getShortName());
			// set pool fields
			s.setPpAuthorMail(session.getPublicPool().getPpAuthorMail());
			s.setPpAuthorName(session.getPublicPool().getPpAuthorName());
			s.setPpDescription(session.getPublicPool().getPpDescription());
			s.setPpFaculty(session.getPublicPool().getPpFaculty());
			s.setPpLevel(session.getPublicPool().getPpLevel());
			s.setPpLicense(session.getPublicPool().getPpLicense());
			s.setPpLogo(session.getPublicPool().getPpLogo());
			s.setPpSubject(session.getPublicPool().getPpSubject());
			s.setPpUniversity(session.getPublicPool().getPpUniversity());
			// mark as public pool session
			s.setSessionType("public_pool");
		}
		// other fields
		s.setType("session");
		s.setCreator(user.getUsername());
		s.setCreationTime(new Date().getTime());
		return s;
	}

	public static class ImportExportQuestion extends Question {

		private List<Answer> answers;

		public List<Answer> getAnswers() {
			return answers;
		}

		public void setAnswers(List<Answer> answers) {
			this.answers = answers;
		}
	}

	public static class ImportExportSesssion {

		private String name;

		private String shortName;

		private boolean active;

		private PublicPool publicPool;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getShortName() {
			return shortName;
		}

		public void setShortName(String shortName) {
			this.shortName = shortName;
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

		public PublicPool getPublicPool() {
			return publicPool;
		}

		public void setPublicPool(PublicPool publicPool) {
			this.publicPool = publicPool;
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

		public String getPpAuthorName() {
			return ppAuthorName;
		}

		public void setPpAuthorName(String ppAuthorName) {
			this.ppAuthorName = ppAuthorName;
		}

		public String getPpAuthorMail() {
			return ppAuthorMail;
		}

		public void setPpAuthorMail(String ppAuthorMail) {
			this.ppAuthorMail = ppAuthorMail;
		}

		public String getPpUniversity() {
			return ppUniversity;
		}

		public void setPpUniversity(String ppUniversity) {
			this.ppUniversity = ppUniversity;
		}

		public String getPpLogo() {
			return ppLogo;
		}

		public void setPpLogo(String ppLogo) {
			this.ppLogo = ppLogo;
		}

		public String getPpSubject() {
			return ppSubject;
		}

		public void setPpSubject(String ppSubject) {
			this.ppSubject = ppSubject;
		}

		public String getPpLicense() {
			return ppLicense;
		}

		public void setPpLicense(String ppLicense) {
			this.ppLicense = ppLicense;
		}

		public String getPpLevel() {
			return ppLevel;
		}

		public void setPpLevel(String ppLevel) {
			this.ppLevel = ppLevel;
		}

		public String getPpDescription() {
			return ppDescription;
		}

		public void setPpDescription(String ppDescription) {
			this.ppDescription = ppDescription;
		}

		public String getPpFaculty() {
			return ppFaculty;
		}

		public void setPpFaculty(String ppFaculty) {
			this.ppFaculty = ppFaculty;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getShortName() {
			return shortName;
		}

		public void setShortName(String shortName) {
			this.shortName = shortName;
		}
	}
}
