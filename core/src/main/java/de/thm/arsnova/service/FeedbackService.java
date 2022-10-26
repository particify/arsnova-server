/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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

package de.thm.arsnova.service;

import de.thm.arsnova.model.Feedback;

/**
 * The functionality the feedback service should provide.
 */
public interface FeedbackService {
	void cleanFeedbackVotes();

	void cleanFeedbackVotesByRoomId(String roomId, int cleanupFeedbackDelayInMins);

	Feedback getByRoomId(String roomId);

	int countFeedbackByRoomId(String roomId);

	double calculateAverageFeedback(String roomId);

	long calculateRoundedAverageFeedback(String roomId);

	boolean save(String roomId, int value, String userId);

	Integer getByRoomIdAndUserId(String roomId, String userId);
}
