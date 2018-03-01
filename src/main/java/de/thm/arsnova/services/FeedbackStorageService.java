package de.thm.arsnova.services;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;

import java.util.List;
import java.util.Map;

public interface FeedbackStorageService {
	Feedback getByRoom(Room room);
	Integer getByRoomAndUser(Room room, ClientAuthentication u);
	void save(Room room, int value, ClientAuthentication user);
	Map<Room, List<ClientAuthentication>> cleanVotes(int cleanupFeedbackDelay);
	List<ClientAuthentication> cleanVotesByRoom(Room room, int cleanupFeedbackDelayInMins);
}
