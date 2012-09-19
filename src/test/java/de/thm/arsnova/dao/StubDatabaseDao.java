package de.thm.arsnova.dao;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

public class StubDatabaseDao implements IDatabaseDao {

	private Map<String, Session> stubSessions = new ConcurrentHashMap<String, Session>();
	private Map<String, Feedback> stubFeedbacks = new ConcurrentHashMap<String, Feedback>();
	
	@Override
	public void cleanFeedbackVotes(int cleanupFeedbackDelay) {
		stubSessions.clear();		
	}

	@Override
	public Session getSession(String keyword) {
		return stubSessions.get(keyword);
	}

	@Override
	public Session saveSession(Session session) {
		stubSessions.put(session.getKeyword(), session);
		return session;
	}

	@Override
	public Feedback getFeedback(String keyword) {
		return stubFeedbacks.get(keyword);
	}

	@Override
	public boolean saveFeedback(String keyword, int value, User user) {
		if (stubFeedbacks.get(keyword) == null) {
			stubFeedbacks.put(keyword, new Feedback(0, 0, 0, 0));
		}
		
		Feedback sessionFeedback = stubFeedbacks.get(keyword);
		
		List<Integer> values = sessionFeedback.getValues();
		values.set(value, values.get(value) + 1);
		
		sessionFeedback = new Feedback(values.get(0), values.get(1), values.get(2), values.get(3));
		
		stubFeedbacks.put(
			keyword,
			sessionFeedback
		);
		
		return true;
	}

	@Override
	public boolean sessionKeyAvailable(String keyword) {
		return (stubSessions.get(keyword) != null);
	}

}
