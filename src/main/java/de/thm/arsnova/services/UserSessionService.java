package de.thm.arsnova.services;

import java.util.UUID;

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.events.ARSnovaEvent;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

public interface UserSessionService {

	void setUser(User user);
	User getUser();
	
	void setSession(Session session);
	Session getSession();
	
	void setSocketId(UUID socketId);
	UUID getSocketId();

	void sendEventViaWebSocket(ARSnovaSocketIOServer server, ARSnovaEvent event);
}