package de.thm.arsnova.services;

import java.util.UUID;

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

public interface UserSessionService {

	enum Role {
		STUDENT,
		SPEAKER
	}

	void setUser(User user);
	User getUser();

	void setSession(Session session);
	Session getSession();

	void setSocketId(UUID socketId);
	UUID getSocketId();

	void setRole(Role role);
	Role getRole();

	boolean inSession();
	boolean isAuthenticated();
}
