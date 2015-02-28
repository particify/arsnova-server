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

public class QuestionBasedLearningProgress implements LearningProgress {

	private IDatabaseDao databaseDao;

	public QuestionBasedLearningProgress(IDatabaseDao dao) {
		this.databaseDao = dao;
	}

	@Override
	public LearningProgressValues getCourseProgress(Session session) {
		CourseScore courseScore = databaseDao.getLearningProgress(session);
		LearningProgressValues lpv = new LearningProgressValues();
		lpv.setCourseProgress(calculateCourseProgress(courseScore));
		lpv.setNumQuestions(courseScore.getQuestionCount());
		lpv.setNumUsers(courseScore.getTotalUserCount());
		return lpv;
	}

	private int calculateCourseProgress(CourseScore courseScore) {
		int numQuestionsCorrect = numQuestionsCorrectForCourse(courseScore);
		final double correctQuestionsOnAverage = (double)numQuestionsCorrect / (double)(courseScore.getQuestionCount());
		// calculate percent, cap results to 100
		return (int) Math.min(100, Math.round(correctQuestionsOnAverage*100));
	}

	private int numQuestionsCorrectForCourse(CourseScore courseScore) {
		// a question is seen as "correct" if and only if all participants have answered it correctly
		int numQuestionsCorrect = 0;
		for (QuestionScore questionScore : courseScore) {
			int requiredScore = questionScore.getMaximum();
			if (!questionScore.hasScores()) {
				continue;
			}
			boolean allCorrect = true;
			for (UserScore userScore : questionScore) {
				if (!userScore.hasScore(requiredScore)) {
					allCorrect = false;
					break;
				}
			}
			if (allCorrect) {
				numQuestionsCorrect++;
			}
		}
		return numQuestionsCorrect;
	}

	@Override
	public LearningProgressValues getMyProgress(Session session, User user) {
		CourseScore courseScore = databaseDao.getLearningProgress(session);

		int courseProgress = calculateCourseProgress(courseScore);

		int numQuestionsCorrect = numQuestionsCorrectForUser(user, courseScore);
		final double myLearningProgress = numQuestionsCorrect / (double)(courseScore.getQuestionCount());
		// calculate percent, cap results to 100
		final int percentage = (int) Math.min(100, Math.round(myLearningProgress*100));
		LearningProgressValues lpv = new LearningProgressValues();
		lpv.setCourseProgress(courseProgress);
		lpv.setMyProgress(percentage);
		lpv.setNumQuestions(courseScore.getQuestionCount());
		lpv.setNumUsers(courseScore.getTotalUserCount());
		return lpv;
	}

	private int numQuestionsCorrectForUser(User user, CourseScore courseScore) {
		// compare user's values to the maximum number for each question to determine the answers' correctness
		// mapping (questionId -> 1 if correct, 0 if incorrect)
		int numQuestionsCorrect = 0;
		for (QuestionScore questionScore : courseScore) {
			int requiredScore = questionScore.getMaximum();
			for (UserScore userScore : questionScore) {
				if (!userScore.isUser(user)) {
					continue;
				}
				if (userScore.hasScore(requiredScore)) {
					numQuestionsCorrect++;
				}
			}
		}
		return numQuestionsCorrect;
	}

}
