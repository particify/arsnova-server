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

package de.thm.arsnova.service.score;

import org.springframework.cache.annotation.Cacheable;

import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.transport.ScoreStatistics;
import de.thm.arsnova.persistence.SessionStatisticsRepository;

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
	private Score loadProgress(final Room room) {
		return sessionStatisticsRepository.getLearningProgress(room);
	}

	private void refreshProgress(final Room room) {
		this.courseScore = sessionStatisticsRepository.getLearningProgress(room);
	}

	public void setQuestionVariant(final String variant) {
		this.questionVariant = variant;
	}

	@Override
	public ScoreStatistics getCourseProgress(final Room room) {
		this.refreshProgress(room);
		this.filterVariant();
		return this.createCourseProgress();
	}

	protected abstract ScoreStatistics createCourseProgress();

	@Override
	public ScoreStatistics getMyProgress(final Room room, final String userId) {
		this.refreshProgress(room);
		this.filterVariant();
		return this.createMyProgress(userId);
	}

	private void filterVariant() {
		if (questionVariant != null && !questionVariant.isEmpty()) {
			this.courseScore = this.courseScore.filterVariant(questionVariant);
		}
	}

	protected abstract ScoreStatistics createMyProgress(String userId);

}
