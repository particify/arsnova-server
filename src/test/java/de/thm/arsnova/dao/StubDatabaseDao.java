package de.thm.arsnova.dao;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.socket.message.Question;

@Component
@Scope("singleton")
public class StubDatabaseDao implements IDatabaseDao {

	private Map<String, Session> stubSessions = new ConcurrentHashMap<String, Session>();
	private Map<String, Feedback> stubFeedbacks = new ConcurrentHashMap<String, Feedback>();
	private Map<Session, Question> stubQuestions = new ConcurrentHashMap<Session, Question>();
	
	public StubDatabaseDao() {
		fillWithDummySessions();
		fillWithDummyFeedbacks();
	}
	
	private void fillWithDummySessions() {
		Session session = new Session();
		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("12345678");
		session.setName("TestSession1");
		session.setShortName("TS1");
		
		this.stubSessions.put("12345678", session);
		
		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("87654321");
		session.setName("TestSession2");
		session.setShortName("TS2");
		
		this.stubSessions.put("87654321", session);
	}
	
	private void fillWithDummyFeedbacks() {
		stubFeedbacks.put("12345678", new Feedback(0, 0, 0, 0));
		stubFeedbacks.put("87654321", new Feedback(2, 3, 5, 7));
	}
	
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
		System.out.println(stubSessions.get(keyword));
		return (stubSessions.get(keyword) == null);
	}

	@Override
	public Session getSessionFromKeyword(String keyword) {
		return stubSessions.get(keyword);
	}

	@Override
	public boolean saveQuestion(Session session, Question question) {
		stubQuestions.put(session, question);
		return stubQuestions.get(session) != null;
	}

	@Override
	public List<Session> getMySessions(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Question> getSkillQuestions(String session, String sort) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getSkillQuestionCount(String sessionkey) {
		// TODO Auto-generated method stub
		return 0;
	}

}
