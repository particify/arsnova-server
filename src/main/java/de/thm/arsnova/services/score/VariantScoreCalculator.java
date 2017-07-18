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
package de.thm.arsnova.services.score;

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.ScoreStatistics;
import de.thm.arsnova.persistance.SessionStatisticsRepository;

/**
 * Base class for the score feature that allows filtering on the question variant.
 */
abstract class VariantScoreCalculator implements ScoreCalculator {

	protected Score courseScore;

	private String questionVariant;

	private final SessionStatisticsRepository sessionStatisticsRepository;

	public VariantScoreCalculator(final SessionStatisticsRepository sessionStatisticsRepository) {
		this.sessionStatisticsRepository = sessionStatisticsRepository;
	}

	private void loadProgress(final Session session) {
		this.courseScore = sessionStatisticsRepository.getLearningProgress(session);
	}

	public void setQuestionVariant(final String variant) {
		this.questionVariant = variant;
	}

	@Override
	public ScoreStatistics getCourseProgress(Session session) {
		this.loadProgress(session);
		this.filterVariant();
		return this.createCourseProgress();
	}

	protected abstract ScoreStatistics createCourseProgress();

	@Override
	public ScoreStatistics getMyProgress(Session session, User user) {
		this.loadProgress(session);
		this.filterVariant();
		return this.createMyProgress(user);
	}

	private void filterVariant() {
		if (questionVariant != null && !questionVariant.isEmpty()) {
			this.courseScore = this.courseScore.filterVariant(questionVariant);
		}
	}

	protected abstract ScoreStatistics createMyProgress(User user);

}
