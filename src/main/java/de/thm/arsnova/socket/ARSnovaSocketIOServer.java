package de.thm.arsnova.socket;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

import de.thm.arsnova.entities.User;
import de.thm.arsnova.events.ARSnovaEvent;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.services.IFeedbackService;
import de.thm.arsnova.services.IQuestionService;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.socket.message.Feedback;
import de.thm.arsnova.socket.message.Session;

public class ARSnovaSocketIOServer {

	@Autowired
	private IFeedbackService feedbackService;

	@Autowired
	private IQuestionService questionService;

	@Autowired
	private IUserService userService;

	@Autowired
	private ISessionService sessionService;

	private static final Logger LOGGER = LoggerFactory.getLogger(ARSnovaSocketIOServer.class);

	private int portNumber;
	private String hostIp;
	private boolean useSSL = false;
	private String keystore;
	private String storepass;
	private final Configuration config;
	private SocketIOServer server;

	private int lastActiveUserCount = 0;

	public ARSnovaSocketIOServer() {
		config = new Configuration();
	}
	
	@PreDestroy
	public void closeAllSessions() {
		LOGGER.info("Close all websockets due to @PreDestroy");
		for (SocketIOClient c : server.getAllClients()) {
			c.disconnect();
		}
		
		int clientCount = 0;
		for (SocketIOClient c : server.getAllClients()) {
			c.send(new Packet(PacketType.DISCONNECT));
			clientCount++;
		}
		LOGGER.info("Pending websockets at @PreDestroy: {}", clientCount);
		server.stop();
	}

	public void startServer() throws Exception {
		/**
		 * hack: listen to ipv4 adresses
		 */
		System.setProperty("java.net.preferIPv4Stack", "true");

		config.setPort(portNumber);
		config.setHostname(hostIp);
		if (useSSL) {
			try {
				InputStream stream = new FileInputStream(keystore);
				config.setKeyStore(stream);
				config.setKeyStorePassword(storepass);
			} catch (FileNotFoundException e) {
				LOGGER.error("Keystore {} not found on filesystem", keystore);
			}
		}
		server = new SocketIOServer(config);

		server.addEventListener("setFeedback", Feedback.class, new DataListener<Feedback>() {
			@Override
			public void onData(SocketIOClient client, Feedback data, AckRequest ackSender) {
				/**
				 * do a check if user is in the session, for which he would give
				 * a feedback
				 */
				User u = userService.getUser2SocketId(client.getSessionId());
				if (u == null || !userService.isUserInSession(u, data.getSessionkey())) {
					return;
				}
				feedbackService.saveFeedback(data.getSessionkey(), data.getValue(), u);
			}
		});

		server.addEventListener("setSession", Session.class, new DataListener<Session>() {
			@Override
			public void onData(SocketIOClient client, Session session, AckRequest ackSender) {
				sessionService.joinSession(session.getKeyword(), client.getSessionId());
				/* active user count has to be sent to the client since the broadcast is
				 * not always sent as long as the polling solution is active simultaneously */
				reportActiveUserCountForSession(session.getKeyword());
				reportSessionDataToClient(session.getKeyword(), client);
			}
		});

		server.addConnectListener(new ConnectListener() {
			@Override
			public void onConnect(SocketIOClient client) { }
		});

		server.addDisconnectListener(new DisconnectListener() {
			@Override
			public void onDisconnect(SocketIOClient client) {
				if (userService == null || client.getSessionId() == null || userService.getUser2SocketId(client.getSessionId()) == null) {
					LOGGER.warn("NullPointer in ARSnovaSocketIOServer DisconnectListener");
					return;
				}
				String username = userService.getUser2SocketId(client.getSessionId()).getUsername();
				String sessionKey = userService.getSessionForUser(username);
				userService.removeUserFromSessionBySocketId(client.getSessionId());
				userService.removeUser2SocketId(client.getSessionId());
				if (null != sessionKey) {
					/* user disconnected before joining a session */
					reportActiveUserCountForSession(sessionKey);
				}
			}
		});

		server.start();
	}

	public void stopServer() throws Exception {
		LOGGER.trace("In stopServer method of class: {}", getClass().getName());
		try {
			for (SocketIOClient client : server.getAllClients()) {
				client.disconnect();
			}
		} catch (Exception e) {
			/* If exceptions are not caught they could prevent the Socket.IO server from shutting down. */
			LOGGER.error("Exception caught on Socket.IO shutdown: {}", e.getStackTrace());
		}
		server.stop();

	}

	public int getPortNumber() {
		return portNumber;
	}

	@Required
	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	public String getHostIp() {
		return hostIp;
	}

	public void setHostIp(String hostIp) {
		this.hostIp = hostIp;
	}

	public String getStorepass() {
		return storepass;
	}

	@Required
	public void setStorepass(String storepass) {
		this.storepass = storepass;
	}

	public String getKeystore() {
		return keystore;
	}

	@Required
	public void setKeystore(String keystore) {
		this.keystore = keystore;
	}

	public boolean isUseSSL() {
		return useSSL;
	}

	@Required
	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

	public void reportDeletedFeedback(String username, Set<String> arsSessions) {
		List<UUID> connectionIds = findConnectionIdForUser(username);
		if (connectionIds.isEmpty()) {
			return;
		}

		for (SocketIOClient client : server.getAllClients()) {
			// Find the client whose feedback has been deleted and send a
			// message.
			if (connectionIds.contains(client.getSessionId())) {
				client.sendEvent("feedbackReset", arsSessions);
			}
		}
	}

	private List<UUID> findConnectionIdForUser(String username) {
		List<UUID> result = new ArrayList<UUID>();
		for (Entry<UUID, User> e : userService.socketId2User()) {
			if (e.getValue().getUsername().equals(username)) {
				result.add(e.getKey());
			}
		}
		return result;
	}

	/**
	 * Currently only sends the feedback data to the client. Should be used for all
	 * relevant Socket.IO data, the client needs to know after joining a session.
	 *
	 * @param sessionKey
	 * @param client
	 */
	public void reportSessionDataToClient(String sessionKey, SocketIOClient client) {
		client.sendEvent("activeUserCountData", userService.getUsersInSessionCount(sessionKey));
		de.thm.arsnova.entities.Feedback fb = feedbackService.getFeedback(sessionKey);
		client.sendEvent("feedbackData", fb.getValues());
	}

	public void reportUpdatedFeedbackForSession(String sessionKey) {
		de.thm.arsnova.entities.Feedback fb = feedbackService.getFeedback(sessionKey);
		broadcastInSession(sessionKey, "feedbackData", fb.getValues());
		try {
			long averageFeedback = feedbackService.getAverageFeedbackRounded(sessionKey);
			broadcastInSession(sessionKey, "feedbackDataRoundedAverage", averageFeedback);
		} catch (NoContentException e) {
			broadcastInSession(sessionKey, "feedbackDataRoundedAverage", null);
		}
	}

	public void reportFeedbackForUserInSession(de.thm.arsnova.entities.Session session, User user) {
		de.thm.arsnova.entities.Feedback fb = feedbackService.getFeedback(session.getKeyword());
		Long averageFeedback;
		try {
			averageFeedback = feedbackService.getAverageFeedbackRounded(session.getKeyword());
		} catch (NoContentException e) {
			averageFeedback = null;
		}
		List<UUID> connectionIds = findConnectionIdForUser(user.getUsername());
		if (connectionIds.isEmpty()) {
			return;
		}

		for (SocketIOClient client : server.getAllClients()) {
			if (connectionIds.contains(client.getSessionId())) {
				client.sendEvent("feedbackData", fb.getValues());
				client.sendEvent("feedbackDataRoundedAverage", averageFeedback);
			}
		}
	}

	public void reportActiveUserCountForSession(String sessionKey) {
		/* This check is needed as long as the HTTP polling solution is active simultaneously. */
		int count = userService.getUsersInSessionCount(sessionKey);
		if (count == lastActiveUserCount) {
			return;
		}
		lastActiveUserCount = count;

		broadcastInSession(sessionKey, "activeUserCountData", count);
	}

	public void reportAnswersToLecturerQuestionAvailable(String sessionKey, String lecturerQuestionId) {
		broadcastInSession(sessionKey, "answersToLecQuestionAvail", lecturerQuestionId);
	}

	public void reportAudienceQuestionAvailable(String sessionKey, String audienceQuestionId) {
		/* TODO role handling implementation, send this only to users with role lecturer */
		broadcastInSession(sessionKey, "audQuestionAvail", audienceQuestionId);
	}

	public void reportLecturerQuestionAvailable(String sessionKey, String lecturerQuestionId) {
		/* TODO role handling implementation, send this only to users with role audience */
		broadcastInSession(sessionKey, "lecQuestionAvail", lecturerQuestionId);
	}

	/** Sends event to a websocket connection identified by UUID 
	 * 
	 * @param sessionId The UUID of the websocket ID
	 * @param event The event to be send to client
	 */
	public void sendToClient(UUID sessionId, ARSnovaEvent event) {
		for (SocketIOClient c : server.getAllClients()) {
			if (c.getSessionId().equals(sessionId)) {
				System.out.println(sessionId);
				break;
			}
		}
	}

	public void broadcastInSession(String sessionKey, String eventName, Object data) {
		/**
		 * collect a list of users which are in the current session iterate over
		 * all connected clients and if send feedback, if user is in current
		 * session
		 */
		Set<User> users = userService.getUsersInSession(sessionKey);

		for (SocketIOClient c : server.getAllClients()) {
			User u = userService.getUser2SocketId(c.getSessionId());
			if (u != null && users.contains(u)) {
				c.sendEvent(eventName, data);
			}
		}
	}
}
