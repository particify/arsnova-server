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

import de.thm.arsnova.model.transport.ScoreStatistics;
import de.thm.arsnova.persistence.SessionStatisticsRepository;

/**
 * Calculates score based on a question's value.
 */
public class ScoreBasedScoreCalculator extends VariantScoreCalculator {

	public ScoreBasedScoreCalculator(final SessionStatisticsRepository sessionStatisticsRepository) {
		super(sessionStatisticsRepository);
	}

	@Override
	protected ScoreStatistics createCourseProgress() {
		final ScoreStatistics lpv = new ScoreStatistics();
		lpv.setCourseProgress(coursePercentage());
		lpv.setNumQuestions(courseScore.getQuestionCount());
		lpv.setNumUsers(courseScore.getTotalUserCount());
		lpv.setNumerator(courseScore.getTotalUserScore() / courseScore.getTotalUserCount());
		lpv.setDenominator(courseScore.getMaximumScore());
		return lpv;
	}

	private int coursePercentage() {
		final int courseMaximumValue = courseScore.getMaximumScore();
		final int userTotalValue = courseScore.getTotalUserScore();
		final int numUsers = courseScore.getTotalUserCount();
		if (courseMaximumValue == 0 || numUsers == 0) {
			return 0;
		}
		final double courseAverageValue = (double) userTotalValue / numUsers;
		final double courseProgress = courseAverageValue / courseMaximumValue;
		return (int) Math.min(100, Math.round(courseProgress * 100));
	}

	@Override
	protected ScoreStatistics createMyProgress(final String userId) {
		final ScoreStatistics lpv = new ScoreStatistics();
		lpv.setCourseProgress(coursePercentage());
		lpv.setNumQuestions(courseScore.getQuestionCount());
		lpv.setNumUsers(courseScore.getTotalUserCount());
		lpv.setMyProgress(myPercentage(userId));
		lpv.setNumerator((int) courseScore.getTotalUserScore(userId));
		lpv.setDenominator(courseScore.getMaximumScore());
		return lpv;
	}

	private int myPercentage(final String userId) {
		final int courseMaximumValue = courseScore.getMaximumScore();
		final double userTotalValue = courseScore.getTotalUserScore(userId);
		if (courseMaximumValue == 0) {
			return 0;
		}
		final double myProgress = userTotalValue / courseMaximumValue;
		return (int) Math.min(100, Math.round(myProgress * 100));
	}
}
