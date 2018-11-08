package de.thm.arsnova.service;

import de.thm.arsnova.model.Feedback;
import de.thm.arsnova.model.Room;

import java.util.List;
import java.util.Map;

public interface FeedbackStorageService {
	Feedback getByRoom(Room room);
	Integer getByRoomAndUserId(Room room, String userId);
	void save(Room room, int value, String userId);
	Map<Room, List<String>> cleanVotes(int cleanupFeedbackDelay);
	List<String> cleanVotesByRoom(Room room, int cleanupFeedbackDelayInMins);
}
