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

package de.thm.arsnova.dao;

import java.util.List;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.socket.message.Question;

public interface IDatabaseDao {
	public void cleanFeedbackVotes(int cleanupFeedbackDelay);
	public Session getSessionFromKeyword(String keyword);
	public Session getSession(String keyword);
	public List<Session> getMySessions(String username);
	public Session saveSession(Session session);
	public Feedback getFeedback(String keyword);
	public boolean saveFeedback(String keyword, int value, User user);
	public boolean sessionKeyAvailable(String keyword);
	
	public boolean saveQuestion(Session session, Question question);
	List<Question> getSkillQuestions(String session);
}