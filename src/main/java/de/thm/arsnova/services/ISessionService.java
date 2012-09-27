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

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

import de.thm.arsnova.socket.message.Question;


public interface ISessionService {

	public void cleanFeedbackVotes();
	public Session getSession(String keyword);
	public Session saveSession(Session session);
	public Feedback getFeedback(String keyword);
	public boolean saveFeedback(String keyword, int value, User user);
	public boolean sessionKeyAvailable(String keyword);
	public String generateKeyword();
	public void addUserToSessionMap(String username, String keyword);
	public boolean isUserInSession(User user, String keyword);
	public List<String> getUsersInSession(String keyword);
	public void broadcastFeedbackChanges(Map<String, Set<String>> affectedUsers, Set<String> allAffectedSessions);
	
	public boolean saveQuestion(Question question);
}