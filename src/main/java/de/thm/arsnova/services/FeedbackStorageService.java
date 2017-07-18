package de.thm.arsnova.services;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

import java.util.List;
import java.util.Map;

public interface FeedbackStorageService {
	Feedback getFeedback(Session session);
	Integer getMyFeedback(Session session, User u);
	void saveFeedback(Session session, int value, User user);
	Map<Session, List<User>> cleanFeedbackVotes(int cleanupFeedbackDelay);
	List<User> cleanFeedbackVotesInSession(Session session, int cleanupFeedbackDelayInMins);
}
