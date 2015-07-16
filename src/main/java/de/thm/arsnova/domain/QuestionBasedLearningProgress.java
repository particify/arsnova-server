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
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.LearningProgressValues;

/**
 * Calculates learning progress based on overall correctness of an answer. A question is answered correctly if and
 * only if the maximum question value possible has been achieved.
 */
public class QuestionBasedLearningProgress extends VariantLearningProgress {

	public QuestionBasedLearningProgress(IDatabaseDao dao) {
		super(dao);
	}

	@Override
	protected LearningProgressValues createCourseProgress() {
		LearningProgressValues lpv = new LearningProgressValues();
		lpv.setCourseProgress(calculateCourseProgress());
		lpv.setNumQuestions(courseScore.getQuestionCount());
		lpv.setNumUsers(courseScore.getTotalUserCount());
		return lpv;
	}

	private int calculateCourseProgress() {
		double ratio = 0;
		for (QuestionScore questionScore : courseScore) {
			if (!questionScore.hasScores()) {
				continue;
			}
			int numAnswers = questionScore.getUserCount();
			if (numAnswers != 0) {
				ratio += (double)countCorrectAnswers(questionScore) / (numAnswers * courseScore.getQuestionCount());
			}
		}
		return (int) Math.min(100, Math.round(ratio*100));
	}

	private int countCorrectAnswers(QuestionScore questionScore) {
		int requiredScore = questionScore.getMaximum();
		int numAnswersCorrect = 0;
		for (UserScore userScore : questionScore) {
			if (userScore.hasScore(requiredScore)) {
				numAnswersCorrect++;
			}
		}
		return numAnswersCorrect;
	}

	@Override
	protected LearningProgressValues createMyProgress(User user) {
		LearningProgressValues lpv = new LearningProgressValues();
		lpv.setCourseProgress(calculateCourseProgress());
		lpv.setMyProgress(myPercentage(numQuestionsCorrectForUser(user), courseScore.getQuestionCount()));
		lpv.setNumQuestions(courseScore.getQuestionCount());
		lpv.setNumUsers(courseScore.getTotalUserCount());
		return lpv;
	}

	private int numQuestionsCorrectForUser(User user) {
		int numQuestionsCorrect = 0;
		for (QuestionScore questionScore : courseScore) {
			numQuestionsCorrect += countCorrectAnswersForUser(user, questionScore);
		}
		return numQuestionsCorrect;
	}

	private int countCorrectAnswersForUser(User user, QuestionScore questionScore) {
		int numQuestionsCorrect = 0;
		int requiredScore = questionScore.getMaximum();
		for (UserScore userScore : questionScore) {
			if (!userScore.isUser(user)) {
				continue;
			}
			if (userScore.hasScore(requiredScore)) {
				numQuestionsCorrect++;
			}
		}
		return numQuestionsCorrect;
	}

	private int myPercentage(int numQuestionsCorrect, int questionCount) {
		final double myLearningProgress = numQuestionsCorrect / (double)questionCount;
		return (int) Math.min(100, Math.round(myLearningProgress*100));
	}


}
