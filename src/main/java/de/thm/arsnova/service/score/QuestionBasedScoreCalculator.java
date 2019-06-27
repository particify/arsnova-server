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
 * Calculates learning progress based on overall correctness of an answer. A question is answered correctly if and
 * only if the maximum question value possible has been achieved.
 */
public class QuestionBasedScoreCalculator extends VariantScoreCalculator {

	public QuestionBasedScoreCalculator(final SessionStatisticsRepository sessionStatisticsRepository) {
		super(sessionStatisticsRepository);
	}

	@Override
	protected ScoreStatistics createCourseProgress() {
		final int courseProgress = calculateCourseProgress();
		final int numerator = courseScore.getQuestionCount() * courseProgress / 100;
		final int denominator = courseScore.getQuestionCount();
		final ScoreStatistics lpv = new ScoreStatistics();
		lpv.setCourseProgress(courseProgress);
		lpv.setNumQuestions(courseScore.getQuestionCount());
		lpv.setNumUsers(courseScore.getTotalUserCount());
		lpv.setNumerator(numerator);
		lpv.setDenominator(denominator);
		return lpv;
	}

	private int calculateCourseProgress() {
		double ratio = 0;
		for (final QuestionScore questionScore : courseScore) {
			if (!questionScore.hasScores()) {
				continue;
			}
			final int numAnswers = questionScore.getUserCount();
			if (numAnswers != 0) {
				ratio += (double) countCorrectAnswers(questionScore) / (numAnswers * courseScore.getQuestionCount());
			}
		}
		return (int) Math.min(100, Math.round(ratio * 100));
	}

	private int countCorrectAnswers(final QuestionScore questionScore) {
		final int requiredScore = questionScore.getMaximum();
		int numAnswersCorrect = 0;
		for (final UserScore userScore : questionScore) {
			if (userScore.hasScore(requiredScore)) {
				numAnswersCorrect++;
			}
		}
		return numAnswersCorrect;
	}

	@Override
	protected ScoreStatistics createMyProgress(final String userId) {
		final int numerator = numQuestionsCorrectForUser(userId);
		final int denominator = courseScore.getQuestionCount();
		final ScoreStatistics lpv = new ScoreStatistics();
		lpv.setCourseProgress(calculateCourseProgress());
		lpv.setMyProgress(myPercentage(numerator, denominator));
		lpv.setNumQuestions(courseScore.getQuestionCount());
		lpv.setNumUsers(courseScore.getTotalUserCount());
		lpv.setNumerator(numerator);
		lpv.setDenominator(denominator);
		return lpv;
	}

	private int numQuestionsCorrectForUser(final String userId) {
		int numQuestionsCorrect = 0;
		for (final QuestionScore questionScore : courseScore) {
			numQuestionsCorrect += countCorrectAnswersForUser(userId, questionScore);
		}
		return numQuestionsCorrect;
	}

	private int countCorrectAnswersForUser(final String userId, final QuestionScore questionScore) {
		int numQuestionsCorrect = 0;
		final int requiredScore = questionScore.getMaximum();
		for (final UserScore userScore : questionScore) {
			if (!userScore.isUser(userId)) {
				continue;
			}
			if (userScore.hasScore(requiredScore)) {
				numQuestionsCorrect++;
			}
		}
		return numQuestionsCorrect;
	}

	private int myPercentage(final int numQuestionsCorrect, final int questionCount) {
		final double myLearningProgress = numQuestionsCorrect / (double) questionCount;
		return (int) Math.min(100, Math.round(myLearningProgress * 100));
	}


}
