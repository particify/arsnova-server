package de.thm.arsnova.services;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.UnauthorizedException;

public interface IUserService {
	User getCurrentUser() throws UnauthorizedException;

	User getUser2SessionID(UUID sessionID);

	void putUser2SessionID(UUID sessionID, User user);

	void removeUser2SessionID(UUID sessionID);

	Set<Map.Entry<UUID, User>> users2Session();

	boolean isUserInSession(User user, String keyword);

	List<String> getUsersInSession(String keyword);

	void addCurrentUserToSessionMap(String keyword);

	String getSessionForUser(String username);
}
