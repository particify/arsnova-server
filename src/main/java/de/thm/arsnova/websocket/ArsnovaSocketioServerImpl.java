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
import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.ScoreOptions;
import de.thm.arsnova.events.*;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.CommentService;
import de.thm.arsnova.services.FeedbackService;
import de.thm.arsnova.services.ContentService;
import de.thm.arsnova.services.RoomService;
import de.thm.arsnova.services.UserService;
import de.thm.arsnova.websocket.message.Feedback;
import de.thm.arsnova.websocket.message.Content;
import de.thm.arsnova.websocket.message.Room;
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
	private RoomService roomService;

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
				final UserAuthentication u = userService.getUserToSocketId(client.getSessionId());
				if (u == null) {
					logger.info("Client {} tried to send feedback but is not mapped to a user", client.getSessionId());

					return;
				}
				final String roomShortId = userService.getRoomByUsername(u.getUsername());
				final de.thm.arsnova.entities.Room room = roomService.getInternal(roomShortId, u);

				if (room.getSettings().isFeedbackLocked()) {
					logger.debug("Feedback save blocked: {}", u, roomShortId, data.getValue());
				} else {
					logger.debug("Feedback recieved: {}", u, roomShortId, data.getValue());
					if (null != roomShortId) {
						feedbackService.save(roomShortId, data.getValue(), u);
					}
				}
			}
		});

		server.addEventListener("setSession", Room.class, new DataListener<Room>() {
			@Override
			@Timed(name = "setSessionEvent.onData")
			public void onData(final SocketIOClient client, final Room room, final AckRequest ackSender) {
				final UserAuthentication u = userService.getUserToSocketId(client.getSessionId());
				if (null == u) {
					logger.info("Client {} requested to join room but is not mapped to a user", client.getSessionId());

					return;
				}
				final String oldShortRoomId = userService.getRoomByUsername(u.getUsername());
				if (null != room.getKeyword() && room.getKeyword().equals(oldShortRoomId)) {
					return;
				}

				if (null != roomService.join(room.getKeyword(), client.getSessionId())) {
					/* active user count has to be sent to the client since the broadcast is
					 * not always sent as long as the polling solution is active simultaneously */
					reportActiveUserCountForRoom(room.getKeyword());
					reportRoomDataToClient(room.getKeyword(), u, client);
				}
				if (null != oldShortRoomId) {
					reportActiveUserCountForRoom(oldShortRoomId);
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
				final UserAuthentication user = userService.getUserToSocketId(client.getSessionId());
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
				final UserAuthentication user = userService.getUserToSocketId(client.getSessionId());
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
				throw new UnsupportedOperationException("Not implemented.");
//				final UserAuthentication user = userService.getUserToSocketId(client.getSessionId());
//				final String shortRoomId = userService.getSessionByUsername(user.getUsername());
//				final de.thm.arsnova.entities.Room room = roomService.getInternal(shortRoomId, user);
//				if (room.getOwnerId().equals(user.getId())) {
//					room.setLearningProgressOptions(scoreOptions);
//					roomService.updateInternal(room, user);
//					broadcastInRoom(room.getShortId(), "learningProgressOptions", scoreOptions);
//				}
			}
		});

		server.addConnectListener(new ConnectListener() {
			@Override
			@Timed
			public void onConnect(final SocketIOClient client) {

			}
		});

		server.addDisconnectListener(new DisconnectListener() {
			@Override
			@Timed
			public void onDisconnect(final SocketIOClient client) {
				if (
						userService == null
						|| client.getSessionId() == null
						|| userService.getUserToSocketId(client.getSessionId()) == null
						) {
					return;
				}
				final String username = userService.getUserToSocketId(client.getSessionId()).getUsername();
				final String shortRoomId = userService.getRoomByUsername(username);
				userService.removeUserFromRoomBySocketId(client.getSessionId());
				userService.removeUserToSocketId(client.getSessionId());
				if (null != shortRoomId) {
					/* user disconnected before joining a session */
					reportActiveUserCountForRoom(shortRoomId);
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

	public void reportDeletedFeedback(final UserAuthentication user, final Set<de.thm.arsnova.entities.Room> arsRooms) {
		final List<String> keywords = new ArrayList<>();
		for (final de.thm.arsnova.entities.Room room : arsRooms) {
			keywords.add(room.getShortId());
		}
		this.sendToUser(user, "feedbackReset", keywords);
	}

	private List<UUID> findConnectionIdForUser(final UserAuthentication user) {
		final List<UUID> result = new ArrayList<>();
		for (final Entry<UUID, UserAuthentication> e : userService.getSocketIdToUser()) {
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
	public void reportRoomDataToClient(final String shortRoomId, final UserAuthentication user, final SocketIOClient client) {
		final de.thm.arsnova.entities.Room room = roomService.getInternal(shortRoomId, user);
		final de.thm.arsnova.entities.Room.Settings settings = roomService.getFeatures(shortRoomId);

		client.sendEvent("unansweredLecturerQuestions", contentService.getUnAnsweredLectureQuestionIds(shortRoomId, user));
		client.sendEvent("unansweredPreparationQuestions", contentService.getUnAnsweredPreparationQuestionIds(shortRoomId, user));
		client.sendEvent("countLectureQuestionAnswers", contentService.countLectureQuestionAnswersInternal(shortRoomId));
		client.sendEvent("countPreparationQuestionAnswers", contentService.countPreparationQuestionAnswersInternal(shortRoomId));
		client.sendEvent("activeUserCountData", roomService.activeUsers(shortRoomId));
//		client.sendEvent("learningProgressOptions", room.getLearningProgressOptions());
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getByRoomShortId(shortRoomId);
		client.sendEvent("feedbackData", fb.getValues());

		if (settings.isFlashcardsEnabled()) {
			client.sendEvent("countFlashcards", contentService.countFlashcardsForUserInternal(shortRoomId));
//			client.sendEvent("flipFlashcards", room.getFlipFlashcards());
		}

		try {
			final long averageFeedback = feedbackService.calculateRoundedAverageFeedback(shortRoomId);
			client.sendEvent("feedbackDataRoundedAverage", averageFeedback);
		} catch (final NoContentException e) {
			final Object object = null; // can't directly use "null".
			client.sendEvent("feedbackDataRoundedAverage", object);
		}
	}

	public void reportUpdatedFeedbackForRoom(final de.thm.arsnova.entities.Room room) {
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getByRoomShortId(room.getShortId());
		broadcastInRoom(room.getShortId(), "feedbackData", fb.getValues());
		try {
			final long averageFeedback = feedbackService.calculateRoundedAverageFeedback(room.getShortId());
			broadcastInRoom(room.getShortId(), "feedbackDataRoundedAverage", averageFeedback);
		} catch (final NoContentException e) {
			broadcastInRoom(room.getShortId(), "feedbackDataRoundedAverage", null);
		}
	}

	public void reportFeedbackForUserInRoom(final Room room, final UserAuthentication user) {
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getByRoomShortId(room.getKeyword());
		Long averageFeedback;
		try {
			averageFeedback = feedbackService.calculateRoundedAverageFeedback(room.getKeyword());
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

	public void reportActiveUserCountForRoom(final String shortRoomId) {
		final int count = userService.getUsersByRoomShortId(shortRoomId).size();

		broadcastInRoom(shortRoomId, "activeUserCountData", count);
	}

	public void reportAnswersToContentAvailable(final de.thm.arsnova.entities.Room room, final Content content) {
		broadcastInRoom(room.getShortId(), "answersToLecQuestionAvail", content.get_id());
	}

	public void reportCommentAvailable(final de.thm.arsnova.entities.Room room, final Comment comment) {
		/* TODO role handling implementation, send this only to users with role lecturer */
		broadcastInRoom(room.getShortId(), "audQuestionAvail", comment.getId());
	}

	public void reportContentAvailable(final de.thm.arsnova.entities.Room room, final List<de.thm.arsnova.entities.Content> qs) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.entities.Content q : qs) {
			contents.add(new Content(q));
		}

		/* TODO role handling implementation, send this only to users with role audience */
		if (!qs.isEmpty()) {
			broadcastInRoom(room.getShortId(), "lecQuestionAvail", contents.get(0).get_id()); // deprecated!
		}
		broadcastInRoom(room.getShortId(), "lecturerQuestionAvailable", contents);
	}

	public void reportContentsLocked(final de.thm.arsnova.entities.Room room, final List<de.thm.arsnova.entities.Content> qs) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.entities.Content q : qs) {
			contents.add(new Content(q));
		}
		broadcastInRoom(room.getShortId(), "lecturerQuestionLocked", contents);
	}

	public void reportRoomStatus(final String shortRoomId, final boolean active) {
		broadcastInRoom(shortRoomId, "setSessionActive", active);
	}

	public void broadcastInRoom(final String shortRoomId, final String eventName, final Object data) {
		/* collect a list of users which are in the current room iterate over
		 * all connected clients and if send feedback, if user is in current
		 * room
		 */
		final Set<UserAuthentication> users = userService.getUsersByRoomShortId(shortRoomId);

		for (final SocketIOClient c : server.getAllClients()) {
			final UserAuthentication u = userService.getUserToSocketId(c.getSessionId());
			if (u != null && users.contains(u)) {
				c.sendEvent(eventName, data);
			}
		}
	}

	@Override
	public void visit(NewQuestionEvent event) {
		this.reportContentAvailable(event.getRoom(), Collections.singletonList(event.getQuestion()));
	}

	@Override
	public void visit(UnlockQuestionEvent event) {
		this.reportContentAvailable(event.getRoom(), Collections.singletonList(event.getQuestion()));
	}

	@Override
	public void visit(LockQuestionEvent event) {
		this.reportContentsLocked(event.getRoom(), Collections.singletonList(event.getQuestion()));
	}

	@Override
	public void visit(UnlockQuestionsEvent event) {
		this.reportContentAvailable(event.getRoom(), event.getQuestions());
	}

	@Override
	public void visit(LockQuestionsEvent event) {
		this.reportContentsLocked(event.getRoom(), event.getQuestions());
	}

	@Override
	public void visit(NewCommentEvent event) {
		this.reportCommentAvailable(event.getRoom(), event.getQuestion());
	}

	@Async
	@Override
	@Timed(name = "visit.NewAnswerEvent")
	public void visit(NewAnswerEvent event) {
		final String shortRoomId = event.getRoom().getShortId();
		this.reportAnswersToContentAvailable(event.getRoom(), new Content(event.getContent()));
		broadcastInRoom(shortRoomId, "countQuestionAnswersByQuestionId", contentService.countAnswersAndAbstentionsInternal(event.getContent().getId()));
		broadcastInRoom(shortRoomId, "countLectureQuestionAnswers", contentService.countLectureQuestionAnswersInternal(shortRoomId));
		broadcastInRoom(shortRoomId, "countPreparationQuestionAnswers", contentService.countPreparationQuestionAnswersInternal(shortRoomId));

		// Update the unanswered count for the content variant that was answered.
		final de.thm.arsnova.entities.Content content = event.getContent();
		if ("lecture".equals(content.getGroup())) {
			sendToUser(event.getUser(), "unansweredLecturerQuestions", contentService.getUnAnsweredLectureQuestionIds(shortRoomId, event.getUser()));
		} else if ("preparation".equals(content.getGroup())) {
			sendToUser(event.getUser(), "unansweredPreparationQuestions", contentService.getUnAnsweredPreparationQuestionIds(shortRoomId, event.getUser()));
		}
	}

	@Async
	@Override
	@Timed(name = "visit.DeleteAnswerEvent")
	public void visit(DeleteAnswerEvent event) {
		final String shortRoomId = event.getRoom().getShortId();
		this.reportAnswersToContentAvailable(event.getRoom(), new Content(event.getQuestion()));
		// We do not know which user's answer was deleted, so we can't update his 'unanswered' list of questions...
		broadcastInRoom(shortRoomId, "countLectureQuestionAnswers", contentService.countLectureQuestionAnswersInternal(shortRoomId));
		broadcastInRoom(shortRoomId, "countPreparationQuestionAnswers", contentService.countPreparationQuestionAnswersInternal(shortRoomId));
	}

	@Async
	@Override
	@Timed(name = "visit.PiRoundDelayedStartEvent")
	public void visit(PiRoundDelayedStartEvent event) {
		final String shortRoomId = event.getRoom().getShortId();
		broadcastInRoom(shortRoomId, "startDelayedPiRound", event.getPiRoundInformations());
	}

	@Async
	@Override
	@Timed(name = "visit.PiRoundEndEvent")
	public void visit(PiRoundEndEvent event) {
		final String shortRoomId = event.getRoom().getShortId();
		broadcastInRoom(shortRoomId, "endPiRound", event.getPiRoundEndInformations());
	}

	@Async
	@Override
	@Timed(name = "visit.PiRoundCancelEvent")
	public void visit(PiRoundCancelEvent event) {
		final String shortRoomId = event.getRoom().getShortId();
		broadcastInRoom(shortRoomId, "cancelPiRound", event.getContentId());
	}

	@Override
	public void visit(PiRoundResetEvent event) {
		final String shortRoomId = event.getRoom().getShortId();
		broadcastInRoom(shortRoomId, "resetPiRound", event.getPiRoundResetInformations());
	}

	@Override
	public void visit(LockVoteEvent event) {
		final String shortRoomId = event.getRoom().getShortId();
		broadcastInRoom(shortRoomId, "lockVote", event.getVotingAdmission());
	}

	@Override
	public void visit(UnlockVoteEvent event) {
		final String shortRoomId = event.getRoom().getShortId();
		broadcastInRoom(shortRoomId, "unlockVote", event.getVotingAdmission());
	}

	@Override
	public void visit(LockVotesEvent event) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.entities.Content q : event.getQuestions()) {
			contents.add(new Content(q));
		}
		broadcastInRoom(event.getRoom().getShortId(), "lockVotes", contents);
	}

	@Override
	public void visit(UnlockVotesEvent event) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.entities.Content q : event.getQuestions()) {
			contents.add(new Content(q));
		}
		broadcastInRoom(event.getRoom().getShortId(), "unlockVotes", contents);
	}

	@Override
	public void visit(FeatureChangeEvent event) {
		final String shortRoomId = event.getRoom().getShortId();
		final de.thm.arsnova.entities.Room.Settings settings = event.getRoom().getSettings();
		broadcastInRoom(shortRoomId, "featureChange", settings);

		if (settings.isFlashcardsEnabled()) {
			broadcastInRoom(shortRoomId, "countFlashcards", contentService.countFlashcardsForUserInternal(shortRoomId));
//			broadcastInRoom(shortRoomId, "flipFlashcards", event.getRoom().getFlipFlashcards());
		}
	}

	@Override
	public void visit(LockFeedbackEvent event) {
		broadcastInRoom(event.getRoom().getShortId(), "lockFeedback", event.getRoom().getSettings().isFeedbackLocked());
	}

	@Override
	public void visit(FlipFlashcardsEvent event) {
//		broadcastInRoom(event.getRoom().getShortId(), "flipFlashcards", event.getRoom().getFlipFlashcards());
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
		this.reportUpdatedFeedbackForRoom(event.getRoom());
	}

	@Override
	public void visit(DeleteFeedbackForRoomsEvent event) {
		this.reportDeletedFeedback(event.getUser(), event.getSessions());

	}

	@Override
	public void visit(StatusRoomEvent event) {
		this.reportRoomStatus(event.getRoom().getShortId(), !event.getRoom().isClosed());
	}

	@Override
	public void visit(ChangeScoreEvent event) {
		broadcastInRoom(event.getRoom().getShortId(), "learningProgressChange", null);
	}

	@Override
	public void visit(NewRoomEvent event) { }

	@Override
	public void visit(DeleteRoomEvent event) { }
}
