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
package de.thm.arsnova.services.score;

import de.thm.arsnova.entities.migration.v2.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.ScoreStatistics;
import de.thm.arsnova.persistance.SessionStatisticsRepository;
import org.springframework.cache.annotation.Cacheable;

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

	@Cacheable("score")
	private Score loadProgress(final Session session) {
		return sessionStatisticsRepository.getLearningProgress(session);
	}

	private void refreshProgress(final Session session) {
		this.courseScore = sessionStatisticsRepository.getLearningProgress(session);
	}

	public void setQuestionVariant(final String variant) {
		this.questionVariant = variant;
	}

	@Override
	public ScoreStatistics getCourseProgress(Session session) {
		this.refreshProgress(session);
		this.filterVariant();
		return this.createCourseProgress();
	}

	protected abstract ScoreStatistics createCourseProgress();

	@Override
	public ScoreStatistics getMyProgress(Session session, User user) {
		this.refreshProgress(session);
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
