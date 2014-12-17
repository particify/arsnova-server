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
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;

import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.events.DeleteAnswerEvent;
import de.thm.arsnova.events.NewAnswerEvent;
import de.thm.arsnova.events.NewInterposedQuestionEvent;
import de.thm.arsnova.events.NewQuestionEvent;
import de.thm.arsnova.events.NovaEvent;
import de.thm.arsnova.events.NovaEventVisitor;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.services.IFeedbackService;
import de.thm.arsnova.services.IQuestionService;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.socket.message.Feedback;
import de.thm.arsnova.socket.message.Question;
import de.thm.arsnova.socket.message.Session;

@Component
public class ARSnovaSocketIOServer implements ApplicationListener<NovaEvent>, NovaEventVisitor {

	@Autowired
	private IFeedbackService feedbackService;

	@Autowired
	private IUserService userService;

	@Autowired
	private ISessionService sessionService;

	@Autowired
	private IQuestionService questionService;

	private static final Logger LOGGER = LoggerFactory.getLogger(ARSnovaSocketIOServer.class);

	private int portNumber;
	private String hostIp;
	private boolean useSSL = false;
	private String keystore;
	private String storepass;
	private final Configuration config;
	private SocketIOServer server;

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

		SocketConfig soConfig = new SocketConfig();
		soConfig.setReuseAddress(true);
		config.setSocketConfig(soConfig);
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
				final User u = userService.getUser2SocketId(client.getSessionId());
				final String sessionKey = userService.getSessionForUser(u.getUsername());
				LOGGER.debug("Feedback recieved: {}", new Object[] {u, sessionKey, data.getValue()});
				if (null != sessionKey) {
					feedbackService.saveFeedback(sessionKey, data.getValue(), u);
				}
			}
		});

		server.addEventListener("setSession", Session.class, new DataListener<Session>() {
			@Override
			public void onData(final SocketIOClient client, final Session session, final AckRequest ackSender) {
				final User u = userService.getUser2SocketId(client.getSessionId());
				if (null == u) {
					LOGGER.info("Client {} requested to join session but is not mapped to a user", client.getSessionId());

					return;
				}
				final String oldSessionKey = userService.getSessionForUser(u.getUsername());
				if (session.getKeyword() == oldSessionKey) {
					return;
				}
				if (null != oldSessionKey) {
					reportActiveUserCountForSession(oldSessionKey);
				}

				if (null != sessionService.joinSession(session.getKeyword(), client.getSessionId())) {
					/* active user count has to be sent to the client since the broadcast is
					 * not always sent as long as the polling solution is active simultaneously */
					reportActiveUserCountForSession(session.getKeyword());
					reportSessionDataToClient(session.getKeyword(), u, client);
				}
			}
		});

		server.addEventListener(
				"readInterposedQuestion",
				de.thm.arsnova.entities.transport.InterposedQuestion.class,
				new DataListener<de.thm.arsnova.entities.transport.InterposedQuestion>() {
			@Override
			public void onData(
					SocketIOClient client,
					de.thm.arsnova.entities.transport.InterposedQuestion question,
					AckRequest ackRequest) {
				final User user = userService.getUser2SocketId(client.getSessionId());
				try {
					questionService.readInterposedQuestionInternal(question.getId(), user);
				} catch (NotFoundException | UnauthorizedException e) {
					LOGGER.error("Loading of question {} failed for user {} with exception {}", question.getId(), user, e.getMessage());
				}
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

	public void reportDeletedFeedback(final User user, final Set<de.thm.arsnova.entities.Session> arsSessions) {
		final List<String> keywords = new ArrayList<String>();
		for (final de.thm.arsnova.entities.Session session : arsSessions) {
			keywords.add(session.getKeyword());
		}
		this.sendToUser(user, "feedbackReset", keywords);
	}

	private List<UUID> findConnectionIdForUser(final User user) {
		final List<UUID> result = new ArrayList<UUID>();
		for (final Entry<UUID, User> e : userService.socketId2User()) {
			final UUID someUsersConnectionId = e.getKey();
			final User someUser = e.getValue();
			if (someUser.equals(user)) {
				result.add(someUsersConnectionId);
			}
		}
		return result;
	}

	private void sendToUser(final User user, final String event, Object data) {
		final List<UUID> connectionIds = findConnectionIdForUser(user);
		if (connectionIds.isEmpty()) {
			return;
		}
		for (final SocketIOClient client : server.getAllClients()) {
			if (connectionIds.contains(client.getSessionId())) {
				client.sendEvent(event, data);
			}
		}
	}

	/**
	 * Currently only sends the feedback data to the client. Should be used for all
	 * relevant Socket.IO data, the client needs to know after joining a session.
	 *
	 * @param sessionKey
	 * @param user
	 * @param client
	 */
	public void reportSessionDataToClient(final String sessionKey, final User user, final SocketIOClient client) {
		client.sendEvent("unansweredLecturerQuestions", questionService.getUnAnsweredLectureQuestionIds(sessionKey, user));
		client.sendEvent("unansweredPreparationQuestions", questionService.getUnAnsweredPreparationQuestionIds(sessionKey, user));
		client.sendEvent("countLectureQuestionAnswers", questionService.countLectureQuestionAnswersInternal(sessionKey));
		client.sendEvent("countPreparationQuestionAnswers", questionService.countPreparationQuestionAnswersInternal(sessionKey));
		client.sendEvent("activeUserCountData", sessionService.activeUsers(sessionKey));
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getFeedback(sessionKey);
		client.sendEvent("feedbackData", fb.getValues());
		try {
			final long averageFeedback = feedbackService.getAverageFeedbackRounded(sessionKey);
			client.sendEvent("feedbackDataRoundedAverage", averageFeedback);
		} catch (final NoContentException e) {
			final Object object = null; // can't directly use "null".
			client.sendEvent("feedbackDataRoundedAverage", object);
		}
	}

	public void reportUpdatedFeedbackForSession(final de.thm.arsnova.entities.Session session) {
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getFeedback(session.getKeyword());
		broadcastInSession(session.getKeyword(), "feedbackData", fb.getValues());
		try {
			final long averageFeedback = feedbackService.getAverageFeedbackRounded(session.getKeyword());
			broadcastInSession(session.getKeyword(), "feedbackDataRoundedAverage", averageFeedback);
		} catch (final NoContentException e) {
			broadcastInSession(session.getKeyword(), "feedbackDataRoundedAverage", null);
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
		final List<UUID> connectionIds = findConnectionIdForUser(user);
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
		final int count = userService.getUsersInSession(sessionKey).size();

		broadcastInSession(sessionKey, "activeUserCountData", count);
	}

	public void reportAnswersToLecturerQuestionAvailable(final de.thm.arsnova.entities.Session session, final Question lecturerQuestion) {
		broadcastInSession(session.getKeyword(), "answersToLecQuestionAvail", lecturerQuestion.get_id());
	}

	public void reportAudienceQuestionAvailable(final de.thm.arsnova.entities.Session session, final InterposedQuestion audienceQuestion) {
		/* TODO role handling implementation, send this only to users with role lecturer */
		broadcastInSession(session.getKeyword(), "audQuestionAvail", audienceQuestion.get_id());
	}

	public void reportLecturerQuestionAvailable(final de.thm.arsnova.entities.Session session, final Question lecturerQuestion) {
		/* TODO role handling implementation, send this only to users with role audience */
		broadcastInSession(session.getKeyword(), "lecQuestionAvail", lecturerQuestion.get_id()); // deprecated!
		broadcastInSession(session.getKeyword(), "lecturerQuestionAvailable", lecturerQuestion);
	}

	public void reportSessionStatus(final String sessionKey, final boolean active) {
		broadcastInSession(sessionKey, "setSessionActive", active);
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

	@Override
	public void visit(NewQuestionEvent event) {
		this.reportLecturerQuestionAvailable(event.getSession(), new Question(event.getQuestion()));
	}

	@Override
	public void visit(NewInterposedQuestionEvent event) {
		this.reportAudienceQuestionAvailable(event.getSession(), event.getQuestion());
	}

	@Override
	public void visit(NewAnswerEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		this.reportAnswersToLecturerQuestionAvailable(event.getSession(), new Question(event.getQuestion()));
		broadcastInSession(sessionKey, "countLectureQuestionAnswers", questionService.countLectureQuestionAnswersInternal(sessionKey));
		broadcastInSession(sessionKey, "countPreparationQuestionAnswers", questionService.countPreparationQuestionAnswersInternal(sessionKey));
		sendToUser(event.getUser(), "unansweredLecturerQuestions", questionService.getUnAnsweredLectureQuestionIds(sessionKey, event.getUser()));
		sendToUser(event.getUser(), "unansweredPreparationQuestions", questionService.getUnAnsweredPreparationQuestionIds(sessionKey, event.getUser()));
	}

	@Override
	public void visit(DeleteAnswerEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		this.reportAnswersToLecturerQuestionAvailable(event.getSession(), new Question(event.getQuestion()));
		// We do not know which user's answer was deleted, so we can't update his 'unanswered' list of questions...
		broadcastInSession(sessionKey, "countLectureQuestionAnswers", questionService.countLectureQuestionAnswersInternal(sessionKey));
		broadcastInSession(sessionKey, "countPreparationQuestionAnswers", questionService.countPreparationQuestionAnswersInternal(sessionKey));
	}

	@Override
	public void onApplicationEvent(NovaEvent event) {
		event.accept(this);
	}
}
