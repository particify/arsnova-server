package de.thm.arsnova.services;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.migration.v2.Session;

import java.util.List;
import java.util.Map;

public interface FeedbackStorageService {
	Feedback getBySession(Session session);
	Integer getBySessionAndUser(Session session, UserAuthentication u);
	void save(Session session, int value, UserAuthentication user);
	Map<Session, List<UserAuthentication>> cleanVotes(int cleanupFeedbackDelay);
	List<UserAuthentication> cleanVotesBySession(Session session, int cleanupFeedbackDelayInMins);
}
