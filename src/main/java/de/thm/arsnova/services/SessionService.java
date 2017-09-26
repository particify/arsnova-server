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
package de.thm.arsnova.services;

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.SessionFeature;
import de.thm.arsnova.entities.SessionInfo;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.ImportExportSession;
import de.thm.arsnova.entities.transport.ScoreStatistics;

import java.util.List;
import java.util.UUID;

/**
 * The functionality the session service should provide.
 */
public interface SessionService extends EntityService<Session> {
	Session getByKey(String keyword);

	Session getForAdmin(final String keyword);

	Session getInternal(String keyword, User user);

	Session save(Session session);

	boolean isKeyAvailable(String keyword);

	String generateKey();

	List<Session> getUserSessions(String username);

	List<Session> getUserVisitedSessions(String username);

	List<Session> getMySessions(int offset, int limit);

	List<Session> getMyVisitedSessions(int offset, int limit);

	int countSessionsByCourses(List<Course> courses);

	int activeUsers(String sessionkey);

	Session setActive(String sessionkey, Boolean lock);

	Session join(String keyword, UUID socketId);

	Session update(String sessionkey, Session session);

	Session updateCreator(String sessionkey, String newCreator);

	Session updateInternal(Session session, User user);

	int[] deleteCascading(Session session);

	ScoreStatistics getLearningProgress(String sessionkey, String type, String questionVariant);

	ScoreStatistics getMyLearningProgress(String sessionkey, String type, String questionVariant);

	List<SessionInfo> getMySessionsInfo(int offset, int limit);

	List<SessionInfo> getPublicPoolSessionsInfo();

	List<SessionInfo> getMyPublicPoolSessionsInfo();

	List<SessionInfo> getMyVisitedSessionsInfo(int offset, int limit);

	SessionInfo importSession(ImportExportSession session);

	ImportExportSession exportSession(String sessionkey, Boolean withAnswerStatistics, Boolean withFeedbackQuestions);

	SessionInfo copySessionToPublicPool(String sessionkey, de.thm.arsnova.entities.transport.ImportExportSession.PublicPool pp);

	SessionFeature getFeatures(String sessionkey);

	SessionFeature updateFeatures(String sessionkey, SessionFeature features);

	boolean lockFeedbackInput(String sessionkey, Boolean lock);

	boolean flipFlashcards(String sessionkey, Boolean flip);

	void deleteInactiveSessions();

	void deleteInactiveVisitedSessionLists();
}
