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
package de.thm.arsnova.dao;

import de.thm.arsnova.domain.CourseScore;
import de.thm.arsnova.entities.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Profile("test")
@Service("databaseDao")
public class StubDatabaseDao implements IDatabaseDao {

	private static Map<String, Session> stubSessions = new ConcurrentHashMap<>();
	private static Map<String, Feedback> stubFeedbacks = new ConcurrentHashMap<>();
	private static Map<String, List<Content>> stubQuestions = new ConcurrentHashMap<>();
	private static Map<String, User> stubUsers = new ConcurrentHashMap<>();

	public Comment comment;

	public StubDatabaseDao() {
		fillWithDummySessions();
		fillWithDummyFeedbacks();
		fillWithDummyQuestions();
	}

	public void cleanupTestData() {
		stubSessions.clear();
		stubFeedbacks.clear();
		stubQuestions.clear();
		stubUsers.clear();

		fillWithDummySessions();
		fillWithDummyFeedbacks();
		fillWithDummyQuestions();
	}

	private void fillWithDummySessions() {
		Session session = new Session();
		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("12345678");
		session.setName("TestSession1");
		session.setShortName("TS1");

		stubSessions.put("12345678", session);

		session = new Session();
		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("87654321");
		session.setName("TestSession2");
		session.setShortName("TS2");

		stubSessions.put("87654321", session);

		session = new Session();
		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("18273645");
		session.setName("TestSession2");
		session.setShortName("TS3");

		stubSessions.put("18273645", session);
	}

	private void fillWithDummyFeedbacks() {
		stubFeedbacks.put("12345678", new Feedback(0, 0, 0, 0));
		stubFeedbacks.put("87654321", new Feedback(2, 3, 5, 7));
		stubFeedbacks.put("18273645", new Feedback(2, 3, 5, 11));
	}

	private void fillWithDummyQuestions() {
		List<Content> contents = new ArrayList<>();
		contents.add(new Content());
		stubQuestions.put("12345678", contents);
	}

	@Override
	public int deleteInactiveGuestVisitedSessionLists(long lastActivityBefore) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public CourseScore getLearningProgress(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statistics getStatistics() {
		final Statistics stats = new Statistics();
		stats.setOpenSessions(3);
		stats.setClosedSessions(0);
		stats.setLectureQuestions(0);
		stats.setAnswers(0);
		stats.setInterposedQuestions(0);
		return stats;
	}

	@Override
	public <T> T getObjectFromId(String documentId, Class<T> klass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MotdList getMotdListForUser(final String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MotdList createOrUpdateMotdList(MotdList motdlist) {
		// TODO Auto-generated method stub
		return null;
	}
}
