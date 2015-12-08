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
package de.thm.arsnova.services;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.User;

/**
 * The functionality the feedback service should provide.
 */
public interface IFeedbackService {
	void cleanFeedbackVotes();

	void cleanFeedbackVotesInSession(String keyword, int cleanupFeedbackDelayInMins);

	Feedback getFeedback(String keyword);

	int getFeedbackCount(String keyword);

	double getAverageFeedback(String sessionkey);

	long getAverageFeedbackRounded(String sessionkey);

	boolean saveFeedback(String keyword, int value, User user);

	Integer getMyFeedback(String keyword, User user);
}
