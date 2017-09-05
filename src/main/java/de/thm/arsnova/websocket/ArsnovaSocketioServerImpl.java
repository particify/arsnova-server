/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.websocket;

import com.codahale.metrics.annotation.Timed;
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
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.migration.v2.Comment;
import de.thm.arsnova.entities.ScoreOptions;
import de.thm.arsnova.entities.migration.v2.SessionFeature;
import de.thm.arsnova.events.*;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.CommentService;
import de.thm.arsnova.services.FeedbackService;
import de.thm.arsnova.services.ContentService;
import de.thm.arsnova.services.SessionService;
import de.thm.arsnova.services.UserService;
import de.thm.arsnova.websocket.message.Feedback;
import de.thm.arsnova.websocket.message.Content;
import de.thm.arsnova.websocket.message.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

/**
 * Web socket implementation based on Socket.io.
 */
@Component
public class ArsnovaSocketioServerImpl implements ArsnovaSocketioServer, ArsnovaEventVisitor {

	@Autowired
	private FeedbackService feedbackService;

	@Autowired
	private UserService userService;

	@Autowired
	private SessionService sessionService;

	@Autowired
	private ContentService contentService;

	@Autowired
	private CommentService commentService;

	private static final Logger logger = LoggerFactory.getLogger(ArsnovaSocketioServerImpl.class);

	private int portNumber;
	private String hostIp;
	private boolean useSSL = false;
	private String keystore;
	private String storepass;
	private final Configuration config;
	private SocketIOServer server;
	private boolean securityInitialized;

	public ArsnovaSocketioServerImpl() {
		config = new Configuration();
	}

	@PreDestroy
	public void closeAllSessions() {
		logger.info("Close all websockets due to @PreDestroy");
		for (final SocketIOClient c : server.getAllClients()) {
			c.disconnect();
		}

		int clientCount = 0;
		for (final SocketIOClient c : server.getAllClients()) {
			c.send(new Packet(PacketType.DISCONNECT));
			clientCount++;
		}
		logger.info("Pending websockets at @PreDestroy: {}", clientCount);
		server.stop();
	}

	public void startServer() {
		/* hack: listen to ipv4 adresses */
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
				logger.error("Keystore {} not found on filesystem", keystore);
			}
		}
		server = new SocketIOServer(config);

		server.addEventListener("setFeedback", Feedback.class, new DataListener<Feedback>() {
			@Override
			@Timed(name = "setFeedbackEvent.onData")
			public void onData(final SocketIOClient client, final Feedback data, final AckRequest ackSender) {
				final UserAuthentication u = userService.getUser2SocketId(client.getSessionId());
				if (u == null) {
					logger.info("Client {} tried to send feedback but is not mapped to a user", client.getSessionId());

					return;
				}
				final String sessionKey = userService.getSessionByUsername(u.getUsername());
				final de.thm.arsnova.entities.migration.v2.Session session = sessionService.getInternal(sessionKey, u);

				if (session.getFeedbackLock()) {
					logger.debug("Feedback save blocked: {}", u, sessionKey, data.getValue());
				} else {
					logger.debug("Feedback recieved: {}", u, sessionKey, data.getValue());
					if (null != sessionKey) {
						feedbackService.save(sessionKey, data.getValue(), u);
					}
				}
			}
		});

		server.addEventListener("setSession", Session.class, new DataListener<Session>() {
			@Override
			@Timed(name = "setSessionEvent.onData")
			public void onData(final SocketIOClient client, final Session session, final AckRequest ackSender) {
				final UserAuthentication u = userService.getUser2SocketId(client.getSessionId());
				if (null == u) {
					logger.info("Client {} requested to join session but is not mapped to a user", client.getSessionId());

					return;
				}
				final String oldSessionKey = userService.getSessionByUsername(u.getUsername());
				if (null != session.getKeyword() && session.getKeyword().equals(oldSessionKey)) {
					return;
				}

				if (null != sessionService.join(session.getKeyword(), client.getSessionId())) {
					/* active user count has to be sent to the client since the broadcast is
					 * not always sent as long as the polling solution is active simultaneously */
					reportActiveUserCountForSession(session.getKeyword());
					reportSessionDataToClient(session.getKeyword(), u, client);
				}
				if (null != oldSessionKey) {
					reportActiveUserCountForSession(oldSessionKey);
				}
			}
		});

		/* TODO: This listener expects a Comment entity but only uses the ID. Reduce transmitted data. */
		server.addEventListener(
				"readInterposedQuestion",
				Comment.class,
				new DataListener<Comment>() {
			@Override
			@Timed(name = "readInterposedQuestionEvent.onData")
			public void onData(
					SocketIOClient client,
					Comment comment,
					AckRequest ackRequest) {
				final UserAuthentication user = userService.getUser2SocketId(client.getSessionId());
				try {
					commentService.getAndMarkReadInternal(comment.getId(), user);
				} catch (NotFoundException | UnauthorizedException e) {
					logger.error("Loading of comment {} failed for user {} with exception {}", comment.getId(), user, e.getMessage());
				}
			}
		});

		server.addEventListener("readFreetextAnswer", String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String answerId, AckRequest ackRequest) {
				final UserAuthentication user = userService.getUser2SocketId(client.getSessionId());
				try {
					contentService.getFreetextAnswerAndMarkRead(answerId, user);
				} catch (NotFoundException | UnauthorizedException e) {
					logger.error("Marking answer {} as read failed for user {} with exception {}", answerId, user, e.getMessage());
				}
			}
		});

		server.addEventListener(
				"setLearningProgressOptions",
				ScoreOptions.class,
				new DataListener<ScoreOptions>() {
			@Override
			@Timed(name = "setLearningProgressOptionsEvent.onData")
			public void onData(SocketIOClient client, ScoreOptions scoreOptions, AckRequest ack) {
				final UserAuthentication user = userService.getUser2SocketId(client.getSessionId());
				final String sessionKey = userService.getSessionByUsername(user.getUsername());
				final de.thm.arsnova.entities.migration.v2.Session session = sessionService.getInternal(sessionKey, user);
				if (session.isCreator(user)) {
					session.setLearningProgressOptions(scoreOptions);
					sessionService.updateInternal(session, user);
					broadcastInSession(session.getKeyword(), "learningProgressOptions", scoreOptions);
				}
			}
		});

		server.addConnectListener(new ConnectListener() {
			@Override
			@Timed
			public void onConnect(final SocketIOClient client) {
				if (!securityInitialized) {
					initializeSecurity();
				}
			}
		});

		server.addDisconnectListener(new DisconnectListener() {
			@Override
			@Timed
			public void onDisconnect(final SocketIOClient client) {
				if (
						userService == null
						|| client.getSessionId() == null
						|| userService.getUser2SocketId(client.getSessionId()) == null
						) {
					return;
				}
				final String username = userService.getUser2SocketId(client.getSessionId()).getUsername();
				final String sessionKey = userService.getSessionByUsername(username);
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
		logger.trace("In stopServer method of class: {}", getClass().getName());
		try {
			for (final SocketIOClient client : server.getAllClients()) {
				client.disconnect();
			}
		} catch (final Exception e) {
			/* If exceptions are not caught they could prevent the Socket.IO server from shutting down. */
			logger.error("Exception caught on Socket.IO shutdown: {}", e.getMessage());
		}
		server.stop();

	}

	@Override
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

	@Override
	public boolean isUseSSL() {
		return useSSL;
	}

	@Required
	public void setUseSSL(final boolean useSSL) {
		this.useSSL = useSSL;
	}

	public void reportDeletedFeedback(final UserAuthentication user, final Set<de.thm.arsnova.entities.migration.v2.Session> arsSessions) {
		final List<String> keywords = new ArrayList<>();
		for (final de.thm.arsnova.entities.migration.v2.Session session : arsSessions) {
			keywords.add(session.getKeyword());
		}
		this.sendToUser(user, "feedbackReset", keywords);
	}

	private List<UUID> findConnectionIdForUser(final UserAuthentication user) {
		final List<UUID> result = new ArrayList<>();
		for (final Entry<UUID, UserAuthentication> e : userService.socketId2User()) {
			final UUID someUsersConnectionId = e.getKey();
			final UserAuthentication someUser = e.getValue();
			if (someUser.equals(user)) {
				result.add(someUsersConnectionId);
			}
		}
		return result;
	}

	private void sendToUser(final UserAuthentication user, final String event, Object data) {
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
	 */
	public void reportSessionDataToClient(final String sessionKey, final UserAuthentication user, final SocketIOClient client) {
		final de.thm.arsnova.entities.migration.v2.Session session = sessionService.getInternal(sessionKey, user);
		final SessionFeature features = sessionService.getFeatures(sessionKey);

		client.sendEvent("unansweredLecturerQuestions", contentService.getUnAnsweredLectureQuestionIds(sessionKey, user));
		client.sendEvent("unansweredPreparationQuestions", contentService.getUnAnsweredPreparationQuestionIds(sessionKey, user));
		client.sendEvent("countLectureQuestionAnswers", contentService.countLectureQuestionAnswersInternal(sessionKey));
		client.sendEvent("countPreparationQuestionAnswers", contentService.countPreparationQuestionAnswersInternal(sessionKey));
		client.sendEvent("activeUserCountData", sessionService.activeUsers(sessionKey));
		client.sendEvent("learningProgressOptions", session.getLearningProgressOptions());
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getBySessionKey(sessionKey);
		client.sendEvent("feedbackData", fb.getValues());

		if (features.isFlashcard() || features.isFlashcardFeature()) {
			client.sendEvent("countFlashcards", contentService.countFlashcardsForUserInternal(sessionKey));
			client.sendEvent("flipFlashcards", session.getFlipFlashcards());
		}

		try {
			final long averageFeedback = feedbackService.calculateRoundedAverageFeedback(sessionKey);
			client.sendEvent("feedbackDataRoundedAverage", averageFeedback);
		} catch (final NoContentException e) {
			final Object object = null; // can't directly use "null".
			client.sendEvent("feedbackDataRoundedAverage", object);
		}
	}

	public void reportUpdatedFeedbackForSession(final de.thm.arsnova.entities.migration.v2.Session session) {
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getBySessionKey(session.getKeyword());
		broadcastInSession(session.getKeyword(), "feedbackData", fb.getValues());
		try {
			final long averageFeedback = feedbackService.calculateRoundedAverageFeedback(session.getKeyword());
			broadcastInSession(session.getKeyword(), "feedbackDataRoundedAverage", averageFeedback);
		} catch (final NoContentException e) {
			broadcastInSession(session.getKeyword(), "feedbackDataRoundedAverage", null);
		}
	}

	public void reportFeedbackForUserInSession(final de.thm.arsnova.entities.migration.v2.Session session, final UserAuthentication user) {
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getBySessionKey(session.getKeyword());
		Long averageFeedback;
		try {
			averageFeedback = feedbackService.calculateRoundedAverageFeedback(session.getKeyword());
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
		final int count = userService.getUsersBySessionKey(sessionKey).size();

		broadcastInSession(sessionKey, "activeUserCountData", count);
	}

	public void reportAnswersToLecturerQuestionAvailable(final de.thm.arsnova.entities.migration.v2.Session session, final Content content) {
		broadcastInSession(session.getKeyword(), "answersToLecQuestionAvail", content.get_id());
	}

	public void reportAudienceQuestionAvailable(final de.thm.arsnova.entities.migration.v2.Session session, final Comment audienceQuestion) {
		/* TODO role handling implementation, send this only to users with role lecturer */
		broadcastInSession(session.getKeyword(), "audQuestionAvail", audienceQuestion.getId());
	}

	public void reportLecturerQuestionAvailable(final de.thm.arsnova.entities.migration.v2.Session session, final List<de.thm.arsnova.entities.migration.v2.Content> qs) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.entities.migration.v2.Content q : qs) {
			contents.add(new Content(q));
		}

		/* TODO role handling implementation, send this only to users with role audience */
		if (!qs.isEmpty()) {
			broadcastInSession(session.getKeyword(), "lecQuestionAvail", contents.get(0).get_id()); // deprecated!
		}
		broadcastInSession(session.getKeyword(), "lecturerQuestionAvailable", contents);
	}

	public void reportLecturerQuestionsLocked(final de.thm.arsnova.entities.migration.v2.Session session, final List<de.thm.arsnova.entities.migration.v2.Content> qs) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.entities.migration.v2.Content q : qs) {
			contents.add(new Content(q));
		}
		broadcastInSession(session.getKeyword(), "lecturerQuestionLocked", contents);
	}

	public void reportSessionStatus(final String sessionKey, final boolean active) {
		broadcastInSession(sessionKey, "setSessionActive", active);
	}

	public void broadcastInSession(final String sessionKey, final String eventName, final Object data) {
		/* collect a list of users which are in the current session iterate over
		 * all connected clients and if send feedback, if user is in current
		 * session
		 */
		final Set<UserAuthentication> users = userService.getUsersBySessionKey(sessionKey);

		for (final SocketIOClient c : server.getAllClients()) {
			final UserAuthentication u = userService.getUser2SocketId(c.getSessionId());
			if (u != null && users.contains(u)) {
				c.sendEvent(eventName, data);
			}
		}
	}

	@Override
	public void visit(NewQuestionEvent event) {
		this.reportLecturerQuestionAvailable(event.getSession(), Collections.singletonList(event.getQuestion()));
	}

	@Override
	public void visit(UnlockQuestionEvent event) {
		this.reportLecturerQuestionAvailable(event.getSession(), Collections.singletonList(event.getQuestion()));
	}

	@Override
	public void visit(LockQuestionEvent event) {
		this.reportLecturerQuestionsLocked(event.getSession(), Collections.singletonList(event.getQuestion()));
	}

	@Override
	public void visit(UnlockQuestionsEvent event) {
		this.reportLecturerQuestionAvailable(event.getSession(), event.getQuestions());
	}

	@Override
	public void visit(LockQuestionsEvent event) {
		this.reportLecturerQuestionsLocked(event.getSession(), event.getQuestions());
	}

	@Override
	public void visit(NewCommentEvent event) {
		this.reportAudienceQuestionAvailable(event.getSession(), event.getQuestion());
	}

	@Async
	@Override
	@Timed(name = "visit.NewAnswerEvent")
	public void visit(NewAnswerEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		this.reportAnswersToLecturerQuestionAvailable(event.getSession(), new Content(event.getContent()));
		broadcastInSession(sessionKey, "countQuestionAnswersByQuestionId", contentService.countAnswersAndAbstentionsInternal(event.getContent().getId()));
		broadcastInSession(sessionKey, "countLectureQuestionAnswers", contentService.countLectureQuestionAnswersInternal(sessionKey));
		broadcastInSession(sessionKey, "countPreparationQuestionAnswers", contentService.countPreparationQuestionAnswersInternal(sessionKey));

		// Update the unanswered count for the content variant that was answered.
		final de.thm.arsnova.entities.migration.v2.Content content = event.getContent();
		if ("lecture".equals(content.getQuestionVariant())) {
			sendToUser(event.getUser(), "unansweredLecturerQuestions", contentService.getUnAnsweredLectureQuestionIds(sessionKey, event.getUser()));
		} else if ("preparation".equals(content.getQuestionVariant())) {
			sendToUser(event.getUser(), "unansweredPreparationQuestions", contentService.getUnAnsweredPreparationQuestionIds(sessionKey, event.getUser()));
		}
	}

	@Async
	@Override
	@Timed(name = "visit.DeleteAnswerEvent")
	public void visit(DeleteAnswerEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		this.reportAnswersToLecturerQuestionAvailable(event.getSession(), new Content(event.getQuestion()));
		// We do not know which user's answer was deleted, so we can't update his 'unanswered' list of questions...
		broadcastInSession(sessionKey, "countLectureQuestionAnswers", contentService.countLectureQuestionAnswersInternal(sessionKey));
		broadcastInSession(sessionKey, "countPreparationQuestionAnswers", contentService.countPreparationQuestionAnswersInternal(sessionKey));
	}

	@Async
	@Override
	@Timed(name = "visit.PiRoundDelayedStartEvent")
	public void visit(PiRoundDelayedStartEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		broadcastInSession(sessionKey, "startDelayedPiRound", event.getPiRoundInformations());
	}

	@Async
	@Override
	@Timed(name = "visit.PiRoundEndEvent")
	public void visit(PiRoundEndEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		broadcastInSession(sessionKey, "endPiRound", event.getPiRoundEndInformations());
	}

	@Async
	@Override
	@Timed(name = "visit.PiRoundCancelEvent")
	public void visit(PiRoundCancelEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		broadcastInSession(sessionKey, "cancelPiRound", event.getQuestionId());
	}

	@Override
	public void visit(PiRoundResetEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		broadcastInSession(sessionKey, "resetPiRound", event.getPiRoundResetInformations());
	}

	@Override
	public void visit(LockVoteEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		broadcastInSession(sessionKey, "lockVote", event.getVotingAdmission());
	}

	@Override
	public void visit(UnlockVoteEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		broadcastInSession(sessionKey, "unlockVote", event.getVotingAdmission());
	}

	@Override
	public void visit(LockVotesEvent event) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.entities.migration.v2.Content q : event.getQuestions()) {
			contents.add(new Content(q));
		}
		broadcastInSession(event.getSession().getKeyword(), "lockVotes", contents);
	}

	@Override
	public void visit(UnlockVotesEvent event) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.entities.migration.v2.Content q : event.getQuestions()) {
			contents.add(new Content(q));
		}
		broadcastInSession(event.getSession().getKeyword(), "unlockVotes", contents);
	}

	@Override
	public void visit(FeatureChangeEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		final SessionFeature features = event.getSession().getFeatures();
		broadcastInSession(sessionKey, "featureChange", features);

		if (features.isFlashcard() || features.isFlashcardFeature()) {
			broadcastInSession(sessionKey, "countFlashcards", contentService.countFlashcardsForUserInternal(sessionKey));
			broadcastInSession(sessionKey, "flipFlashcards", event.getSession().getFlipFlashcards());
		}
	}

	@Override
	public void visit(LockFeedbackEvent event) {
		broadcastInSession(event.getSession().getKeyword(), "lockFeedback", event.getSession().getFeedbackLock());
	}

	@Override
	public void visit(FlipFlashcardsEvent event) {
		broadcastInSession(event.getSession().getKeyword(), "flipFlashcards", event.getSession().getFlipFlashcards());
	}

	@Override
	public void visit(DeleteQuestionEvent deleteQuestionEvent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(DeleteAllQuestionsEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(DeleteAllQuestionsAnswersEvent deleteAllAnswersEvent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(DeleteAllPreparationAnswersEvent deleteAllPreparationAnswersEvent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(DeleteAllLectureAnswersEvent deleteAllLectureAnswersEvent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(DeleteCommentEvent deleteCommentEvent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(NewFeedbackEvent event) {
		this.reportUpdatedFeedbackForSession(event.getSession());
	}

	@Override
	public void visit(DeleteFeedbackForSessionsEvent event) {
		this.reportDeletedFeedback(event.getUser(), event.getSessions());

	}

	@Override
	public void visit(StatusSessionEvent event) {
		this.reportSessionStatus(event.getSession().getKeyword(), event.getSession().isActive());
	}

	@Override
	public void visit(ChangeScoreEvent event) {
		broadcastInSession(event.getSession().getKeyword(), "learningProgressChange", null);
	}

	@Override
	public void visit(NewSessionEvent event) { }

	@Override
	public void visit(DeleteSessionEvent event) { }

	private void initializeSecurity() {
		Authentication auth = new AnonymousAuthenticationToken("websocket", "websocket",
				AuthorityUtils.createAuthorityList("ROLE_WEBSOCKET_ACCESS"));
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);
		securityInitialized = true;
	}
}
