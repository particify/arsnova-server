/*
 * Copyright (C) 2012 THM webMedia
 *
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.services;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.UUID;

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.SessionInfo;

public interface ISessionService {
	Session getSession(String keyword);

	Session saveSession(Session session);

	boolean sessionKeyAvailable(String keyword);

	String generateKeyword();

	List<Session> getMySessions();
	
	List<Session> getPublicPoolSessions();

	List<Session> getMyVisitedSessions();

	int countSessions(List<Course> courses);

	int activeUsers(String sessionkey);

	Session setActive(String sessionkey, Boolean lock);

	Session joinSession(String keyword, UUID socketId);

	Session updateSession(String sessionkey, Session session);

	void deleteSession(String sessionkey);

	int getLearningProgress(String sessionkey);

	SimpleEntry<Integer, Integer> getMyLearningProgress(String sessionkey);

	List<SessionInfo> getMySessionsInfo();

	List<SessionInfo> getMyVisitedSessionsInfo();
}
