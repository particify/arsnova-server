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
package de.thm.arsnova.domain;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.LearningProgressValues;

/**
 * Base class for the Learning Progress feature that allows filtering on the question variant.
 */
abstract class VariantLearningProgress implements LearningProgress {

	protected CourseScore courseScore;

	private String questionVariant;

	private final IDatabaseDao databaseDao;

	public VariantLearningProgress(final IDatabaseDao dao) {
		this.databaseDao = dao;
	}

	private void loadProgress(final Session session) {
		this.courseScore = databaseDao.getLearningProgress(session);
	}

	public void setQuestionVariant(final String variant) {
		this.questionVariant = variant;
	}

	@Override
	public LearningProgressValues getCourseProgress(Session session) {
		this.loadProgress(session);
		this.filterVariant();
		return this.createCourseProgress();
	}

	protected abstract LearningProgressValues createCourseProgress();

	@Override
	public LearningProgressValues getMyProgress(Session session, User user) {
		this.loadProgress(session);
		this.filterVariant();
		return this.createMyProgress(user);
	}

	private void filterVariant() {
		if (questionVariant != null && !questionVariant.isEmpty()) {
			this.courseScore = this.courseScore.filterVariant(questionVariant);
		}
	}

	protected abstract LearningProgressValues createMyProgress(User user);

}
