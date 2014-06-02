package de.thm.arsnova;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.NotFoundException;

public class FeedbackStorage {
	private static class FeedbackStorageObject {
		private final int value;
		private final Date timestamp;
		private final User user;

		public FeedbackStorageObject(final int initValue, final User u) {
			value = initValue;
			timestamp = new Date();
			user = u;
		}

		public int getValue() {
			return value;
		}
		public Date getTimestamp() {
			return timestamp;
		}
		public boolean fromUser(final User u) {
			return u.getUsername().equals(user.getUsername());
		}
	}

	private final IDatabaseDao dao;

	private final Map<String, Map<String, FeedbackStorageObject>> data;

	public FeedbackStorage(final IDatabaseDao newDao) {
		data = new ConcurrentHashMap<String, Map<String, FeedbackStorageObject>>();
		dao = newDao;
	}

	public Feedback getFeedback(final String keyword) {
		int a = 0;
		int b = 0;
		int c = 0;
		int d = 0;

		if (dao.getSession(keyword) == null) {
			throw new NotFoundException();
		}

		if (data.get(keyword) == null) {
			return new Feedback(0, 0, 0, 0);
		}

		for (final FeedbackStorageObject fso : data.get(keyword).values()) {
			switch (fso.getValue()) {
			case Feedback.FEEDBACK_FASTER:
				a++;
				break;
			case Feedback.FEEDBACK_OK:
				b++;
				break;
			case Feedback.FEEDBACK_SLOWER:
				c++;
				break;
			case Feedback.FEEDBACK_AWAY:
				d++;
				break;
			default:
				break;
			}
		}
		return new Feedback(a, b, c, d);
	}

	public Integer getMyFeedback(final String keyword, final User u) {
		if (data.get(keyword) == null) {
			return null;
		}

		for (final FeedbackStorageObject fso : data.get(keyword).values()) {
			if (fso.fromUser(u)) {
				return fso.getValue();
			}
		}

		return null;
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public boolean saveFeedback(final String keyword, final int value, final User user) {
		if (dao.getSession(keyword) == null) {
			throw new NotFoundException();
		}

		if (data.get(keyword) == null) {
			data.put(keyword, new ConcurrentHashMap<String, FeedbackStorageObject>());
		}

		data.get(keyword).put(user.getUsername(), new FeedbackStorageObject(value, user));
		return true;
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void cleanFeedbackVotes(final int cleanupFeedbackDelay) {
		for (final String keyword : data.keySet()) {
			cleanSessionFeedbackVotes(keyword, cleanupFeedbackDelay);
		}
	}

	private void cleanSessionFeedbackVotes(final String keyword, final int cleanupFeedbackDelay) {
		final long timelimitInMillis = 60000 * (long) cleanupFeedbackDelay;
		final long maxAllowedTimeInMillis = System.currentTimeMillis() - timelimitInMillis;

		final Map<String, FeedbackStorageObject> sessionFeedbacks = data.get(keyword);

		for (final Map.Entry<String, FeedbackStorageObject> entry : sessionFeedbacks.entrySet()) {
			if (
					entry.getValue().getTimestamp().getTime() < maxAllowedTimeInMillis
					) {
				sessionFeedbacks.remove(entry.getKey());
			}
		}
	}
}
