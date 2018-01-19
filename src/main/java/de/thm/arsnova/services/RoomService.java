/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.transport.ImportExportSession;
import de.thm.arsnova.entities.transport.ScoreStatistics;

import java.util.List;
import java.util.UUID;

/**
 * The functionality the session service should provide.
 */
public interface RoomService extends EntityService<Room> {
	Room getByKey(String keyword);

	Room getForAdmin(final String keyword);

	Room getInternal(String keyword, UserAuthentication user);

	Room save(Room session);

	boolean isKeyAvailable(String keyword);

	String generateKey();

	List<Room> getUserSessions(String userId);

	List<Room> getUserVisitedSessions(String username);

	List<Room> getMySessions(int offset, int limit);

	List<Room> getMyVisitedSessions(int offset, int limit);

	int countSessionsByCourses(List<Course> courses);

	int activeUsers(String sessionkey);

	Room setActive(String sessionkey, Boolean lock);

	Room join(String keyword, UUID socketId);

	Room update(String sessionkey, Room session);

	Room updateCreator(String sessionkey, String newCreator);

	Room updateInternal(Room session, UserAuthentication user);

	int[] deleteCascading(Room session);

	ScoreStatistics getLearningProgress(String sessionkey, String type, String questionVariant);

	ScoreStatistics getMyLearningProgress(String sessionkey, String type, String questionVariant);

	List<Room> getMySessionsInfo(int offset, int limit);

	List<Room> getPublicPoolSessionsInfo();

	List<Room> getMyPublicPoolSessionsInfo();

	List<Room> getMyVisitedSessionsInfo(int offset, int limit);

	Room importSession(ImportExportSession session);

	ImportExportSession exportSession(String sessionkey, Boolean withAnswerStatistics, Boolean withFeedbackQuestions);

	Room copySessionToPublicPool(String sessionkey, de.thm.arsnova.entities.transport.ImportExportSession.PublicPool pp);

	Room.Settings getFeatures(String sessionkey);

	Room.Settings updateFeatures(String sessionkey, Room.Settings settings);

	boolean lockFeedbackInput(String sessionkey, Boolean lock);

	boolean flipFlashcards(String sessionkey, Boolean flip);

	void deleteInactiveSessions();

	void deleteInactiveVisitedSessionLists();
}
