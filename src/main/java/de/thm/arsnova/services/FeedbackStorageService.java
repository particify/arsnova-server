package de.thm.arsnova.services;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.migration.v2.Session;
import de.thm.arsnova.entities.User;

import java.util.List;
import java.util.Map;

public interface FeedbackStorageService {
	Feedback getBySession(Session session);
	Integer getBySessionAndUser(Session session, User u);
	void save(Session session, int value, User user);
	Map<Session, List<User>> cleanVotes(int cleanupFeedbackDelay);
	List<User> cleanVotesBySession(Session session, int cleanupFeedbackDelayInMins);
}
