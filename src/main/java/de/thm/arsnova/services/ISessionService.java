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
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;


public interface ISessionService {

	public void cleanFeedbackVotes();
	public Session joinSession(String keyword);
	public Session saveSession(Session session);
	public Feedback getFeedback(String keyword);
	public int getFeedbackCount(String keyword);
	public int getAverageFeedback(String sessionkey);
	public boolean saveFeedback(String keyword, int value, User user);
	public boolean sessionKeyAvailable(String keyword);
	public String generateKeyword();
	public void broadcastFeedbackChanges(Map<String, Set<String>> affectedUsers, Set<String> allAffectedSessions);
	
	public List<Session> getMySessions(String username);
	public boolean saveQuestion(Question question);
	public Question getQuestion(String id);
	public LoggedIn registerAsOnlineUser(User user, String sessionkey);
	public List<Question> getSkillQuestions(String sessionkey, String sort);
	public int getSkillQuestionCount(String sessionkey);
}