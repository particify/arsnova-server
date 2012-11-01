package de.thm.arsnova.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;

@Component
@Scope("singleton")
public class StubDatabaseDao implements IDatabaseDao {

	private static Map<String, Session> stubSessions = new ConcurrentHashMap<String, Session>();
	private static Map<String, Feedback> stubFeedbacks = new ConcurrentHashMap<String, Feedback>();
	private static Map<String, List<Question>> stubQuestions = new ConcurrentHashMap<String, List<Question>>();
	private static Map<String, User> stubUsers = new ConcurrentHashMap<String, User>();

	public StubDatabaseDao() {
		fillWithDummySessions();
		fillWithDummyFeedbacks();
		fillWithDummyQuestions();
	}

	public void cleanupTestData() {
		stubSessions.clear();
		stubFeedbacks.clear();
		stubQuestions.clear();
		stubUsers.clear();
		
		fillWithDummySessions();
		fillWithDummyFeedbacks();
		fillWithDummyQuestions();
	}
	
	private void fillWithDummySessions() {
		Session session = new Session();
		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("12345678");
		session.setName("TestSession1");
		session.setShortName("TS1");

		stubSessions.put("12345678", session);

		session.setActive(true);
		session.setCreator("ptsr00");
		session.setKeyword("87654321");
		session.setName("TestSession2");
		session.setShortName("TS2");

		stubSessions.put("87654321", session);
	}

	private void fillWithDummyFeedbacks() {
		stubFeedbacks.put("12345678", new Feedback(0, 0, 0, 0));
		stubFeedbacks.put("87654321", new Feedback(2, 3, 5, 7));
		stubFeedbacks.put("18273645", new Feedback(2, 3, 5, 11));
	}

	private void fillWithDummyQuestions() {
		List<Question> questions = new ArrayList<Question>();
		questions.add(new Question());
		stubQuestions.put("12345678", questions);
	}

	@Override
	public void cleanFeedbackVotes(int cleanupFeedbackDelay) {
		stubSessions.clear();
	}

	@Override
	public Session getSession(String keyword) {
		// Magic keyword for forbidden session
		if (keyword.equals("99999999"))
			throw new ForbiddenException();

		Session session = stubSessions.get(keyword);
		if (session == null)
			throw new NotFoundException();

		return session;
	}

	@Override
	public Session saveSession(Session session) {
		stubSessions.put(session.getKeyword(), session);
		return session;
	}

	@Override
	public Feedback getFeedback(String keyword) {
		// Magic keyword for forbidden session
		if (keyword.equals("99999999"))
			throw new ForbiddenException();

		Feedback feedback = stubFeedbacks.get(keyword);
		if (feedback == null)
			throw new NotFoundException();

		return feedback;
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

		stubFeedbacks.put(keyword, sessionFeedback);

		return true;
	}

	@Override
	public boolean sessionKeyAvailable(String keyword) {
		return (stubSessions.get(keyword) == null);
	}

	@Override
	public Session getSessionFromKeyword(String keyword) {
		return stubSessions.get(keyword);
	}

	@Override
	public boolean saveQuestion(Session session, Question question) {
		List<Question> questions = stubQuestions.get(session.get_id());
		questions.add(question);
		stubQuestions.put(session.get_id(), questions);
		return stubQuestions.get(session) != null;
	}

	@Override
	public Question getQuestion(String id, String sesseionKey) {
		// Simply ... no such question ;-)
		return null;
	}

	@Override
	public List<Question> getSkillQuestions(String session) {
		if (getSession(session) == null)
			throw new NotFoundException();
		List<Question> questions = stubQuestions.get(session);
		if (questions == null)
			throw new NoContentException();
		return questions;
	}

	@Override
	public int getSkillQuestionCount(String sessionkey) {
		return stubQuestions.get(sessionkey).size();
	}

	@Override
	public List<Session> getMySessions(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LoggedIn registerAsOnlineUser(User u, Session s) {
		stubUsers.put(s.getKeyword(), u);
		return new LoggedIn();
	}

	@Override
	public void updateSessionOwnerActivity(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public Integer getMyFeedback(String keyword, User user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getQuestionIds(String sessionKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteQuestion(String sessionKey, String questionId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<String> getUnAnsweredQuestions(String sessionKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Answer getMyAnswer(String sessionKey, String questionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Answer> getAnswers(String sessionKey, String questionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getAnswerCount(String sessionKey, String questionId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Answer> getFreetextAnswers(String sessionKey, String questionId) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int getActiveUsers(long since) {
		return stubUsers.size();
	}
	
	@Override
	public List<Answer> getMyAnswers(String sessionKey) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int getTotalAnswerCount(String sessionKey) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int getInterposedCount(String sessionKey) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public List<Question> getInterposedQuestions(String sessionKey) {
		// TODO Auto-generated method stub
		return null;
	}

}
