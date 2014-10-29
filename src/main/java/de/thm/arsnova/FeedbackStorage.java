package de.thm.arsnova;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

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
			return user.equals(u);
		}
	}

	private final Map<Session, Map<User, FeedbackStorageObject>> data = new ConcurrentHashMap<Session, Map<User, FeedbackStorageObject>>();

	public Feedback getFeedback(final Session session) {
		int a = 0;
		int b = 0;
		int c = 0;
		int d = 0;

		if (data.get(session) == null) {
			return new Feedback(0, 0, 0, 0);
		}

		for (final FeedbackStorageObject fso : data.get(session).values()) {
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

	public Integer getMyFeedback(final Session session, final User u) {
		if (data.get(session) == null) {
			return null;
		}

		for (final FeedbackStorageObject fso : data.get(session).values()) {
			if (fso.fromUser(u)) {
				return fso.getValue();
			}
		}

		return null;
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void saveFeedback(final Session session, final int value, final User user) {
		if (data.get(session) == null) {
			data.put(session, new ConcurrentHashMap<User, FeedbackStorageObject>());
		}

		data.get(session).put(user, new FeedbackStorageObject(value, user));
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Map<Session, List<User>> cleanFeedbackVotes(final int cleanupFeedbackDelay) {
		final Map<Session, List<User>> removedFeedbackOfUsersInSession = new HashMap<Session, List<User>>();
		for (final Session session : data.keySet()) {
			List<User> affectedUsers = cleanFeedbackVotesInSession(session, cleanupFeedbackDelay);
			if (!affectedUsers.isEmpty()) {
				removedFeedbackOfUsersInSession.put(session, affectedUsers);
			}
		}
		return removedFeedbackOfUsersInSession;
	}

	private List<User> cleanFeedbackVotesInSession(final Session session, final int cleanupFeedbackDelay) {
		final long timelimitInMillis = 60000 * (long) cleanupFeedbackDelay;
		final long maxAllowedTimeInMillis = System.currentTimeMillis() - timelimitInMillis;

		final Map<User, FeedbackStorageObject> sessionFeedbacks = data.get(session);
		final List<User> affectedUsers = new ArrayList<User>();

		for (final Map.Entry<User, FeedbackStorageObject> entry : sessionFeedbacks.entrySet()) {
			final User user = entry.getKey();
			final FeedbackStorageObject feedback = entry.getValue();
			final boolean timeIsUp = feedback.getTimestamp().getTime() < maxAllowedTimeInMillis;
			if (timeIsUp) {
				sessionFeedbacks.remove(user);
				affectedUsers.add(user);
			}
		}
		return affectedUsers;
	}
}
