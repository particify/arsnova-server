package de.thm.arsnova.services;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.events.ARSnovaEvent;
import de.thm.arsnova.events.ARSnovaEvent.Destination;
import de.thm.arsnova.events.Publisher;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserSessionServiceImpl implements UserSessionService, Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(UserSessionServiceImpl.class);
	private static final int MAX_USER_INACTIVE_MILLIS = 120000;
	
	private User user;
	private Session session;
	private UUID socketId;
	private Date lastActive;

	@Autowired
	private IUserService userService;

	@PreDestroy
	public void tearDown() {
		if ( socketId != null ) {
			LOGGER.info("Removing websocket session {}", socketId);
			userService.removeUser2SocketId(socketId);
		}
		LOGGER.info("TEAR DOWN SESSION");
	}
	
	@Override
	public void setUser(User u) {
		this.user = u;
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public void setSession(Session s) {
		this.session = s;
	}

	@Override
	public Session getSession() {
		return this.session;
	}

	@Override
	public void setSocketId(UUID sId) {
		this.socketId = sId;
	}

	@Override
	public UUID getSocketId() {
		return this.socketId;
	}

	@Override
	public LoggedIn keepalive() {
		if (this.user != null) {
			this.lastActive = new Date();
			userService.setLastOnlineActivity(this.user, this.lastActive);
			
			LoggedIn result = new LoggedIn();
			result.setUser(this.user.getUsername());
			result.setTimestamp(this.lastActive.getTime());
			return result;
		}
		
		return null;
	}
	
	private boolean hasConnectedWebSocket() {
		return getSocketId() != null;
	}

	@Override
	public void sendEventViaWebSocket(ARSnovaSocketIOServer server, ARSnovaEvent event) {
		if (event == null) {
			LOGGER.info("Trying to send NULL event");
			return;
		}
		
		if (
				event.getDestinationType() == Destination.SESSION
				&& hasConnectedWebSocket()
				&& session != null
				&& event.getSessionKey().equals(session.getKeyword())
		) {
			server.sendToClient(getSocketId(), event);
		}
		
		if (
				event.getDestinationType() == Destination.USER
				&& hasConnectedWebSocket()
				&& user != null
				&& event.getRecipient().getUsername().equals(user.getUsername())
		) {
			server.sendToClient(getSocketId(), event);
		}
	}
}
