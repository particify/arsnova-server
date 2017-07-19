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
package de.thm.arsnova.persistance;

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.SessionInfo;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.ImportExportSession;

import java.util.List;

public interface SessionRepository {
	Session getSessionFromId(String sessionId);
	Session getSessionFromKeyword(String keyword);
	Session saveSession(User user, Session session);
	Session updateSession(Session session);

	/**
	 * Deletes a session and related data.
	 *
	 * @param session the session for deletion
	 */
	int[] deleteSession(Session session);

	Session changeSessionCreator(Session session, String newCreator);
	int[] deleteInactiveGuestSessions(long lastActivityBefore);
	List<Session> getMySessions(User user, int start, int limit);
	List<Session> getSessionsForUsername(String username, int start, int limit);
	List<Session> getPublicPoolSessions();
	List<Session> getMyPublicPoolSessions(User user);
	boolean sessionKeyAvailable(String keyword);
	Session updateSessionOwnerActivity(Session session);
	List<Session> getVisitedSessionsForUsername(String username, int start, int limit);
	List<SessionInfo> getMySessionsInfo(User user, int start, int limit);
	List<SessionInfo> getPublicPoolSessionsInfo();
	List<SessionInfo> getMyPublicPoolSessionsInfo(User user);
	List<SessionInfo> getMyVisitedSessionsInfo(User currentUser, int start, int limit);
	List<Session> getCourseSessions(List<Course> courses);
	SessionInfo importSession(User user, ImportExportSession importSession);
	ImportExportSession exportSession(String sessionkey, Boolean withAnswer, Boolean withFeedbackQuestions);
	LoggedIn registerAsOnlineUser(User user, Session session);
}
