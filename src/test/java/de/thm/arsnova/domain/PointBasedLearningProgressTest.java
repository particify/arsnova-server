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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.TestUser;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.LearningProgressValues;

public class PointBasedLearningProgressTest {

	private CourseScore courseScore;
	private VariantLearningProgress lp;

	@Before
	public void setUp() {
		this.courseScore = new CourseScore();
		IDatabaseDao db = mock(IDatabaseDao.class);
		when(db.getLearningProgress(null)).thenReturn(courseScore);
		this.lp = new PointBasedLearningProgress(db);
	}

	@Test
	public void shouldFilterBasedOnQuestionVariant() {
		courseScore.addQuestion("question1", "lecture", 100);
		courseScore.addQuestion("question2", "preparation", 100);
		User u1 = new TestUser("user1");
		User u2 = new TestUser("user2");
		// first question is answered correctly, second one is not
		courseScore.addAnswer("question1", u1.getUsername(), 100);
		courseScore.addAnswer("question1", u2.getUsername(), 100);
		courseScore.addAnswer("question2", u1.getUsername(), 0);
		courseScore.addAnswer("question2", u2.getUsername(), 0);

		lp.setQuestionVariant("lecture");
		LearningProgressValues lectureProgress = lp.getCourseProgress(null);
		LearningProgressValues myLectureProgress = lp.getMyProgress(null, u1);
		lp.setQuestionVariant("preparation");
		LearningProgressValues prepProgress = lp.getCourseProgress(null);
		LearningProgressValues myPrepProgress = lp.getMyProgress(null, u1);

		assertEquals(100, lectureProgress.getCourseProgress());
		assertEquals(100, myLectureProgress.getMyProgress());
		assertEquals(0, prepProgress.getCourseProgress());
		assertEquals(0, myPrepProgress.getMyProgress());
	}
}
