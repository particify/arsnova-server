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
	}
}
