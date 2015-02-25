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

public class PointBasedLearningProgress implements LearningProgress {

	private IDatabaseDao databaseDao;

	public PointBasedLearningProgress(IDatabaseDao dao) {
		this.databaseDao = dao;
	}

	@Override
	public LearningProgressValues getCourseProgress(Session session) {
		CourseScore courseScore = databaseDao.getLearningProgress(session);
		LearningProgressValues lpv = new LearningProgressValues();
		lpv.setCourseProgress(calculateCourseScore(courseScore));
		lpv.setNumQuestions(courseScore.getQuestionCount());
		return lpv;
	}

	private int calculateCourseScore(CourseScore courseScore) {
		final double courseMaximumValue = courseScore.getMaximumScore();
		final double userTotalValue = courseScore.getTotalUserScore();
		final double numUsers = courseScore.getTotalUserCount();
		if (courseMaximumValue == 0 || numUsers == 0) {
			return 0;
		}
		final double courseAverageValue = userTotalValue / numUsers;
		final double courseProgress = courseAverageValue / courseMaximumValue;
		return (int)Math.min(100, Math.round(courseProgress * 100));
	}

	@Override
	public LearningProgressValues getMyProgress(Session session, User user) {
		CourseScore courseScore = databaseDao.getLearningProgress(session);
		int courseProgress = calculateCourseScore(courseScore);

		final double courseMaximumValue = courseScore.getMaximumScore();
		final double userTotalValue = courseScore.getTotalUserScore(user);

		LearningProgressValues lpv = new LearningProgressValues();
		lpv.setCourseProgress(courseProgress);
		lpv.setNumQuestions(courseScore.getQuestionCount());
		if (courseMaximumValue == 0) {
			return lpv;
		}
		final double myProgress = userTotalValue / courseMaximumValue;
		final int myLearningProgress = (int)Math.min(100, Math.round(myProgress*100));
		lpv.setMyProgress(myLearningProgress);

		return lpv;
	}

}
