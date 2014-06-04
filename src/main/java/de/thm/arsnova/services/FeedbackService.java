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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import de.thm.arsnova.FeedbackStorage;
import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Service
public class FeedbackService implements IFeedbackService {

	private static final int DEFAULT_SCHEDULER_DELAY = 5000;

	@Autowired
	private ARSnovaSocketIOServer server;

	/**
	 * minutes, after which the feedback is deleted
	 */
	@Value("${feedback.cleanup}")
	private int cleanupFeedbackDelay;

	@Autowired
	private IDatabaseDao databaseDao;

	private FeedbackStorage feedbackStorage;

	public final void setDatabaseDao(final IDatabaseDao newDatabaseDao) {
		databaseDao = newDatabaseDao;
	}

	@PostConstruct
	public void init() {
		feedbackStorage = new FeedbackStorage(databaseDao);
	}

	@Override
	@Scheduled(fixedDelay = DEFAULT_SCHEDULER_DELAY)
	public final void cleanFeedbackVotes() {
		feedbackStorage.cleanFeedbackVotes(cleanupFeedbackDelay);
	}

	@Override
	public final Feedback getFeedback(final String keyword) {
		return feedbackStorage.getFeedback(keyword);
	}

	@Override
	public final int getFeedbackCount(final String keyword) {
		final Feedback feedback = feedbackStorage.getFeedback(keyword);
		final List<Integer> values = feedback.getValues();
		return values.get(Feedback.FEEDBACK_FASTER) + values.get(Feedback.FEEDBACK_OK)
				+ values.get(Feedback.FEEDBACK_SLOWER) + values.get(Feedback.FEEDBACK_AWAY);
	}

	@Override
	public final double getAverageFeedback(final String sessionkey) {
		final Feedback feedback = feedbackStorage.getFeedback(sessionkey);
		final List<Integer> values = feedback.getValues();
		final double count = values.get(Feedback.FEEDBACK_FASTER) + values.get(Feedback.FEEDBACK_OK)
				+ values.get(Feedback.FEEDBACK_SLOWER) + values.get(Feedback.FEEDBACK_AWAY);
		final double sum = values.get(Feedback.FEEDBACK_OK) + values.get(Feedback.FEEDBACK_SLOWER) * 2
				+ values.get(Feedback.FEEDBACK_AWAY) * 3;

		if (count == 0) {
			throw new NoContentException();
		}
		return sum / count;
	}

	@Override
	public final long getAverageFeedbackRounded(final String sessionkey) {
		return Math.round(getAverageFeedback(sessionkey));
	}

	@Override
	public final boolean saveFeedback(final String keyword, final int value, final User user) {
		final boolean result = feedbackStorage.saveFeedback(keyword, value, user);
		if (result) {
			server.reportUpdatedFeedbackForSession(keyword);
		}
		return result;
	}

	@Override
	public final Integer getMyFeedback(final String keyword, final User user) {
		return feedbackStorage.getMyFeedback(keyword, user);
	}
}
