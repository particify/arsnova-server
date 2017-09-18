package de.thm.arsnova.services;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.migration.v2.Room;

import java.util.List;
import java.util.Map;

public interface FeedbackStorageService {
	Feedback getByRoom(Room room);
	Integer getByRoomAndUser(Room room, UserAuthentication u);
	void save(Room room, int value, UserAuthentication user);
	Map<Room, List<UserAuthentication>> cleanVotes(int cleanupFeedbackDelay);
	List<UserAuthentication> cleanVotesByRoom(Room room, int cleanupFeedbackDelayInMins);
}
