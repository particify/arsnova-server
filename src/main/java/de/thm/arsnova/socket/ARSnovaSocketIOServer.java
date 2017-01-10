/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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
package de.thm.arsnova.socket;

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
import de.thm.arsnova.entities.transport.LearningProgressOptions;
import de.thm.arsnova.events.*;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.IFeedbackService;
import de.thm.arsnova.services.IQuestionService;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.socket.message.Feedback;
import de.thm.arsnova.socket.message.Question;
import de.thm.arsnova.socket.message.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

/**
 * Web socket implementation based on Socket.io.
 */
@Component
public class ARSnovaSocketIOServer implements ARSnovaSocket, NovaEventVisitor {

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
				if (u == null) {
					LOGGER.info("Client {} tried to send feedback but is not mapped to a user", client.getSessionId());

					return;
				}
				final String sessionKey = userService.getSessionForUser(u.getUsername());
				final de.thm.arsnova.entities.Session session = sessionService.getSessionInternal(sessionKey, u);

				if (session.getFeedbackLock()) {
					LOGGER.debug("Feedback save blocked: {}", new Object[] {u, sessionKey, data.getValue()});
				} else {
					LOGGER.debug("Feedback recieved: {}", new Object[] {u, sessionKey, data.getValue()});
					if (null != sessionKey) {
						feedbackService.saveFeedback(sessionKey, data.getValue(), u);
					}
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
				if (null != session.getKeyword() && session.getKeyword().equals(oldSessionKey)) {
					return;
				}

				if (null != sessionService.joinSession(session.getKeyword(), client.getSessionId())) {
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

		server.addEventListener("readFreetextAnswer", String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String answerId, AckRequest ackRequest) {
				final User user = userService.getUser2SocketId(client.getSessionId());
				try {
					questionService.readFreetextAnswer(answerId, user);
				} catch (NotFoundException | UnauthorizedException e) {
					LOGGER.error("Marking answer {} as read failed for user {} with exception {}", answerId, user, e.getMessage());
				}
			}
		});

		server.addEventListener(
				"setLearningProgressOptions",
				LearningProgressOptions.class,
				new DataListener<LearningProgressOptions>() {
			@Override
			public void onData(SocketIOClient client, LearningProgressOptions progressOptions, AckRequest ack) {
				final User user = userService.getUser2SocketId(client.getSessionId());
				final de.thm.arsnova.entities.Session session = sessionService.getSessionInternal(progressOptions.getSessionKeyword(), user);
				if (session.isCreator(user)) {
					session.setLearningProgressOptions(progressOptions.toEntity());
					sessionService.updateSessionInternal(session, user);
					broadcastInSession(session.getKeyword(), "learningProgressOptions", progressOptions.toEntity());
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
			LOGGER.error("Exception caught on Socket.IO shutdown: {}", e.getMessage());
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
		final de.thm.arsnova.entities.Session session = sessionService.getSessionInternal(sessionKey, user);
		final de.thm.arsnova.entities.SessionFeature features = sessionService.getSessionFeatures(sessionKey);
		
		client.sendEvent("unansweredLecturerQuestions", questionService.getUnAnsweredLectureQuestionIds(sessionKey, user));
		client.sendEvent("unansweredPreparationQuestions", questionService.getUnAnsweredPreparationQuestionIds(sessionKey, user));
		client.sendEvent("countLectureQuestionAnswers", questionService.countLectureQuestionAnswersInternal(sessionKey));
		client.sendEvent("countPreparationQuestionAnswers", questionService.countPreparationQuestionAnswersInternal(sessionKey));
		client.sendEvent("activeUserCountData", sessionService.activeUsers(sessionKey));
		client.sendEvent("learningProgressOptions", session.getLearningProgressOptions());
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getFeedback(sessionKey);
		client.sendEvent("feedbackData", fb.getValues());

		if (features.isFlashcard() || features.isFlashcardFeature()) {
			client.sendEvent("countFlashcards", questionService.countFlashcardsForUserInternal(sessionKey));
			client.sendEvent("flipFlashcards", session.getFlipFlashcards());
		}

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

	public void reportLecturerQuestionAvailable(final de.thm.arsnova.entities.Session session, final List<de.thm.arsnova.entities.Question> qs) {
		List<Question> questions = new ArrayList<Question>();
		for (de.thm.arsnova.entities.Question q : qs) {
			questions.add(new Question(q));
		}

		/* TODO role handling implementation, send this only to users with role audience */
		if (qs.size() > 0) {
			broadcastInSession(session.getKeyword(), "lecQuestionAvail", questions.get(0).get_id()); // deprecated!
		}
		broadcastInSession(session.getKeyword(), "lecturerQuestionAvailable", questions);
	}

	public void reportLecturerQuestionsLocked(final de.thm.arsnova.entities.Session session, final List<de.thm.arsnova.entities.Question> qs) {
		List<Question> questions = new ArrayList<Question>();
		for (de.thm.arsnova.entities.Question q : qs) {
			questions.add(new Question(q));
		}
		broadcastInSession(session.getKeyword(), "lecturerQuestionLocked", questions);
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
		this.reportLecturerQuestionAvailable(event.getSession(), Arrays.asList(event.getQuestion()));
	}

	@Override
	public void visit(UnlockQuestionEvent event) {
		this.reportLecturerQuestionAvailable(event.getSession(), Arrays.asList(event.getQuestion()));
	}

	@Override
	public void visit(LockQuestionEvent event) {
		this.reportLecturerQuestionsLocked(event.getSession(), Arrays.asList(event.getQuestion()));
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
	public void visit(NewInterposedQuestionEvent event) {
		this.reportAudienceQuestionAvailable(event.getSession(), event.getQuestion());
	}

	@Async
	@Override
	public void visit(NewAnswerEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		this.reportAnswersToLecturerQuestionAvailable(event.getSession(), new Question(event.getQuestion()));
		broadcastInSession(sessionKey, "countQuestionAnswersByQuestionId", questionService.getAnswerAndAbstentionCountInternal(event.getQuestion().get_id()));
		broadcastInSession(sessionKey, "countLectureQuestionAnswers", questionService.countLectureQuestionAnswersInternal(sessionKey));
		broadcastInSession(sessionKey, "countPreparationQuestionAnswers", questionService.countPreparationQuestionAnswersInternal(sessionKey));

		// Update the unanswered count for the question variant that was answered.
		final de.thm.arsnova.entities.Question question = event.getQuestion();
		if (question.getQuestionVariant().equals("lecture")) {
			sendToUser(event.getUser(), "unansweredLecturerQuestions", questionService.getUnAnsweredLectureQuestionIds(sessionKey, event.getUser()));
		} else if (question.getQuestionVariant().equals("preparation")) {
			sendToUser(event.getUser(), "unansweredPreparationQuestions", questionService.getUnAnsweredPreparationQuestionIds(sessionKey, event.getUser()));
		}
	}

	@Async
	@Override
	public void visit(DeleteAnswerEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		this.reportAnswersToLecturerQuestionAvailable(event.getSession(), new Question(event.getQuestion()));
		// We do not know which user's answer was deleted, so we can't update his 'unanswered' list of questions...
		broadcastInSession(sessionKey, "countLectureQuestionAnswers", questionService.countLectureQuestionAnswersInternal(sessionKey));
		broadcastInSession(sessionKey, "countPreparationQuestionAnswers", questionService.countPreparationQuestionAnswersInternal(sessionKey));
	}

	@Async
	@Override
	public void visit(PiRoundDelayedStartEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		broadcastInSession(sessionKey, "startDelayedPiRound", event.getPiRoundInformations());
	}

	@Async
	@Override
	public void visit(PiRoundEndEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		broadcastInSession(sessionKey, "endPiRound", event.getPiRoundEndInformations());
	}

	@Async
	@Override
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
		List<Question> questions = new ArrayList<Question>();
		for (de.thm.arsnova.entities.Question q : event.getQuestions()) {
			questions.add(new Question(q));
		}
		broadcastInSession(event.getSession().getKeyword(), "lockVotes", questions);
	}

	@Override
	public void visit(UnlockVotesEvent event) {
		List<Question> questions = new ArrayList<Question>();
		for (de.thm.arsnova.entities.Question q : event.getQuestions()) {
			questions.add(new Question(q));
		}
		broadcastInSession(event.getSession().getKeyword(), "unlockVotes", questions);
	}

	@Override
	public void visit(FeatureChangeEvent event) {
		final String sessionKey = event.getSession().getKeyword();
		final de.thm.arsnova.entities.SessionFeature features = event.getSession().getFeatures();
		broadcastInSession(sessionKey, "featureChange", features);

		if (features.isFlashcard() || features.isFlashcardFeature()) {
			broadcastInSession(sessionKey, "countFlashcards", questionService.countFlashcardsForUserInternal(sessionKey));
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
	public void visit(DeleteInterposedQuestionEvent deleteInterposedQuestionEvent) {
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
	public void visit(ChangeLearningProgressEvent event) {
		broadcastInSession(event.getSession().getKeyword(), "learningProgressChange", null);
	}

	@Override
	public void visit(NewSessionEvent event) { }

	@Override
	public void visit(DeleteSessionEvent event) { }
}
