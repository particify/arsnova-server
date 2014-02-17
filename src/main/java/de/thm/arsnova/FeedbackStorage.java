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
		private int value;
		private Date timestamp;
		private User user;

		public FeedbackStorageObject(int initValue, User u) {
			this.value = initValue;
			this.timestamp = new Date();
			this.user = u;
		}

		public int getValue() {
			return value;
		}
		public Date getTimestamp() {
			return timestamp;
		}
		public boolean fromUser(User u) {
			return u.getUsername().equals(user.getUsername());
		}
	}
	
	private Map<String, Map<String, FeedbackStorageObject>> data;
	
	private IDatabaseDao dao;
	
	public FeedbackStorage(IDatabaseDao newDao) {
		this.data = new ConcurrentHashMap<String, Map<String, FeedbackStorageObject>>();
		this.dao = newDao;
	}

	public Feedback getFeedback(String keyword) {
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

		for (FeedbackStorageObject fso : data.get(keyword).values()) {
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

	public Integer getMyFeedback(String keyword, User u) {
		if (data.get(keyword) == null) {
			return null;
		}
		
		for (FeedbackStorageObject fso : data.get(keyword).values()) {
			if (fso.fromUser(u)) {
				return fso.getValue();
			}
		}
		
		return null;
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public boolean saveFeedback(String keyword, int value, User user) {
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
	public void cleanFeedbackVotes(int cleanupFeedbackDelay) {
		for (String keyword : data.keySet()) {
			this.cleanSessionFeedbackVotes(keyword, cleanupFeedbackDelay);
		}
	}

	private void cleanSessionFeedbackVotes(String keyword, int cleanupFeedbackDelay) {
		final long timelimitInMillis = 60000 * (long) cleanupFeedbackDelay;
		final long maxAllowedTimeInMillis = System.currentTimeMillis() - timelimitInMillis;

		Map<String, FeedbackStorageObject> sessionFeedbacks = data.get(keyword);

		for (Map.Entry<String, FeedbackStorageObject> entry : sessionFeedbacks.entrySet()) {
			if (
				entry.getValue().getTimestamp().getTime() < maxAllowedTimeInMillis
			) {
				sessionFeedbacks.remove(entry.getKey());
			}
		}
	}
}
