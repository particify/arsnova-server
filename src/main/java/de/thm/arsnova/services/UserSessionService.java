package de.thm.arsnova.services;

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

public interface UserSessionService {

	void setUser(User user);
	User getUser();
	
	void setSession(Session session);
	Session getSession();
	
}