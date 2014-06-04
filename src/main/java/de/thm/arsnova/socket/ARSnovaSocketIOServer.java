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
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.services.IFeedbackService;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.socket.message.Feedback;
import de.thm.arsnova.socket.message.Session;

public class ARSnovaSocketIOServer {

	@Autowired
	private IFeedbackService feedbackService;

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
		for (final SocketIOClient c : server.getAllClients()) {
			c.disconnect();
		}

		int clientCount = 0;
		for (final SocketIOClient c : server.getAllClients()) {
			c.send(new Packet(PacketType.DISCONNECT));
			clientCount++;
		}
		LOGGER.info("Pending websockets at @PreDestroy: {}", clientCount);
		server.stop();
	}

	public void startServer() {
		/**
		 * hack: listen to ipv4 adresses
		 */
		System.setProperty("java.net.preferIPv4Stack", "true");

		config.setPort(portNumber);
		config.setHostname(hostIp);
		if (useSSL) {
			try {
				final InputStream stream = new FileInputStream(keystore);
				config.setKeyStore(stream);
				config.setKeyStorePassword(storepass);
			} catch (final FileNotFoundException e) {
				LOGGER.error("Keystore {} not found on filesystem", keystore);
			}
		}
		server = new SocketIOServer(config);

		server.addEventListener("setFeedback", Feedback.class, new DataListener<Feedback>() {
			@Override
			public void onData(final SocketIOClient client, final Feedback data, final AckRequest ackSender) {
				/**
				 * do a check if user is in the session, for which he would give
				 * a feedback
				 */
				final User u = userService.getUser2SocketId(client.getSessionId());
				if (u == null || !userService.isUserInSession(u, data.getSessionkey())) {
					return;
				}
				feedbackService.saveFeedback(data.getSessionkey(), data.getValue(), u);
			}
		});

		server.addEventListener("setSession", Session.class, new DataListener<Session>() {
			@Override
			public void onData(final SocketIOClient client, final Session session, final AckRequest ackSender) {
				sessionService.joinSession(session.getKeyword(), client.getSessionId());
				/* active user count has to be sent to the client since the broadcast is
				 * not always sent as long as the polling solution is active simultaneously */
				reportActiveUserCountForSession(session.getKeyword());
				reportSessionDataToClient(session.getKeyword(), client);
			}
		});

		server.addConnectListener(new ConnectListener() {
			@Override
			public void onConnect(final SocketIOClient client) { }
		});

		server.addDisconnectListener(new DisconnectListener() {
			@Override
			public void onDisconnect(final SocketIOClient client) {
				if (
						userService == null
						|| client.getSessionId() == null
						|| userService.getUser2SocketId(client.getSessionId()) == null
						) {
					return;
				}
				final String username = userService.getUser2SocketId(client.getSessionId()).getUsername();
				final String sessionKey = userService.getSessionForUser(username);
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

	public void stopServer() {
		LOGGER.trace("In stopServer method of class: {}", getClass().getName());
		try {
			for (final SocketIOClient client : server.getAllClients()) {
				client.disconnect();
			}
		} catch (final Exception e) {
			/* If exceptions are not caught they could prevent the Socket.IO server from shutting down. */
			LOGGER.error("Exception caught on Socket.IO shutdown: {}", e.getStackTrace());
		}
		server.stop();

	}

	public int getPortNumber() {
		return portNumber;
	}

	@Required
	public void setPortNumber(final int portNumber) {
		this.portNumber = portNumber;
	}

	public String getHostIp() {
		return hostIp;
	}

	public void setHostIp(final String hostIp) {
		this.hostIp = hostIp;
	}

	public String getStorepass() {
		return storepass;
	}

	@Required
	public void setStorepass(final String storepass) {
		this.storepass = storepass;
	}

	public String getKeystore() {
		return keystore;
	}

	@Required
	public void setKeystore(final String keystore) {
		this.keystore = keystore;
	}

	public boolean isUseSSL() {
		return useSSL;
	}

	@Required
	public void setUseSSL(final boolean useSSL) {
		this.useSSL = useSSL;
	}

	public void reportDeletedFeedback(final String username, final Set<String> arsSessions) {
		final List<UUID> connectionIds = findConnectionIdForUser(username);
		if (connectionIds.isEmpty()) {
			return;
		}

		for (final SocketIOClient client : server.getAllClients()) {
			// Find the client whose feedback has been deleted and send a
			// message.
			if (connectionIds.contains(client.getSessionId())) {
				client.sendEvent("feedbackReset", arsSessions);
			}
		}
	}

	private List<UUID> findConnectionIdForUser(final String username) {
		final List<UUID> result = new ArrayList<UUID>();
		for (final Entry<UUID, User> e : userService.socketId2User()) {
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
	public void reportSessionDataToClient(final String sessionKey, final SocketIOClient client) {
		client.sendEvent("activeUserCountData", sessionService.activeUsers(sessionKey));
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getFeedback(sessionKey);
		client.sendEvent("feedbackData", fb.getValues());
	}

	public void reportUpdatedFeedbackForSession(final String sessionKey) {
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getFeedback(sessionKey);
		broadcastInSession(sessionKey, "feedbackData", fb.getValues());
		try {
			final long averageFeedback = feedbackService.getAverageFeedbackRounded(sessionKey);
			broadcastInSession(sessionKey, "feedbackDataRoundedAverage", averageFeedback);
		} catch (final NoContentException e) {
			broadcastInSession(sessionKey, "feedbackDataRoundedAverage", null);
		}
	}

	public void reportFeedbackForUserInSession(final de.thm.arsnova.entities.Session session, final User user) {
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getFeedback(session.getKeyword());
		Long averageFeedback;
		try {
			averageFeedback = feedbackService.getAverageFeedbackRounded(session.getKeyword());
		} catch (final NoContentException e) {
			averageFeedback = null;
		}
		final List<UUID> connectionIds = findConnectionIdForUser(user.getUsername());
		if (connectionIds.isEmpty()) {
			return;
		}

		for (final SocketIOClient client : server.getAllClients()) {
			if (connectionIds.contains(client.getSessionId())) {
				client.sendEvent("feedbackData", fb.getValues());
				client.sendEvent("feedbackDataRoundedAverage", averageFeedback);
			}
		}
	}

	public void reportActiveUserCountForSession(final String sessionKey) {
		/* This check is needed as long as the HTTP polling solution is active simultaneously. */
		final int count = sessionService.activeUsers(sessionKey);
		if (count == lastActiveUserCount) {
			return;
		}
		lastActiveUserCount = count;

		broadcastInSession(sessionKey, "activeUserCountData", count);
	}

	public void reportAnswersToLecturerQuestionAvailable(final String sessionKey, final String lecturerQuestionId) {
		broadcastInSession(sessionKey, "answersToLecQuestionAvail", lecturerQuestionId);
	}

	public void reportAudienceQuestionAvailable(final String sessionKey, final String audienceQuestionId) {
		/* TODO role handling implementation, send this only to users with role lecturer */
		broadcastInSession(sessionKey, "audQuestionAvail", audienceQuestionId);
	}

	public void reportLecturerQuestionAvailable(final String sessionKey, final String lecturerQuestionId) {
		/* TODO role handling implementation, send this only to users with role audience */
		broadcastInSession(sessionKey, "lecQuestionAvail", lecturerQuestionId);
	}

	public void broadcastInSession(final String sessionKey, final String eventName, final Object data) {
		/**
		 * collect a list of users which are in the current session iterate over
		 * all connected clients and if send feedback, if user is in current
		 * session
		 */
		final Set<User> users = userService.getUsersInSession(sessionKey);

		for (final SocketIOClient c : server.getAllClients()) {
			final User u = userService.getUser2SocketId(c.getSessionId());
			if (u != null && users.contains(u)) {
				c.sendEvent(eventName, data);
			}
		}
	}
}
