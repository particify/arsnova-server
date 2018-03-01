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
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.ScoreOptions;
import de.thm.arsnova.entities.migration.ToV2Migrator;
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
import java.io.IOException;
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

	@Autowired
	private ToV2Migrator toV2Migrator;

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
				final ClientAuthentication u = userService.getUserToSocketId(client.getSessionId());
				if (u == null) {
					logger.info("Client {} tried to send feedback but is not mapped to a user", client.getSessionId());

					return;
				}
				final String roomId = userService.getRoomIdByUserId(u.getId());
				final de.thm.arsnova.entities.Room room = roomService.getInternal(roomId, u);

				if (room.getSettings().isFeedbackLocked()) {
					logger.debug("Feedback ignored: User: {}, Room Id: {}, Feedback: {}", u, roomId, data.getValue());
				} else {
					logger.debug("Feedback received: User: {}, Room Id: {}, Feedback: {}", u, roomId, data.getValue());
					if (null != roomId) {
						feedbackService.save(roomId, data.getValue(), u);
					}
				}
			}
		});

		server.addEventListener("setSession", Room.class, new DataListener<Room>() {
			@Override
			@Timed(name = "setSessionEvent.onData")
			public void onData(final SocketIOClient client, final Room room, final AckRequest ackSender) {
				final ClientAuthentication u = userService.getUserToSocketId(client.getSessionId());
				if (null == u) {
					logger.info("Client {} requested to join room but is not mapped to a user", client.getSessionId());

					return;
				}
				final String oldRoomId = userService.getRoomIdByUserId(u.getId());
				if (null != room.getKeyword()) {
					if (room.getKeyword().equals(oldRoomId)) {
						return;
					}
					final String roomId = roomService.getIdByShortId(room.getKeyword());

					if (null != roomId && null != roomService.join(roomId, client.getSessionId())) {
						/* active user count has to be sent to the client since the broadcast is
						 * not always sent as long as the polling solution is active simultaneously */
						reportActiveUserCountForRoom(roomId);
						reportRoomDataToClient(roomId, u, client);
					}
				}
				if (null != oldRoomId) {
					reportActiveUserCountForRoom(oldRoomId);
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
				final ClientAuthentication user = userService.getUserToSocketId(client.getSessionId());
				try {
					commentService.getAndMarkRead(comment.getId());
				} catch (IOException | NotFoundException | UnauthorizedException e) {
					logger.error("Loading of comment {} failed for user {} with exception {}", comment.getId(), user, e.getMessage());
				}
			}
		});

		server.addEventListener("readFreetextAnswer", String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String answerId, AckRequest ackRequest) {
				final ClientAuthentication user = userService.getUserToSocketId(client.getSessionId());
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
//				final ClientAuthentication user = userService.getUserToSocketId(client.getSessionId());
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
				final String userId = userService.getUserToSocketId(client.getSessionId()).getId();
				final String roomId = userService.getRoomIdByUserId(userId);
				userService.removeUserFromRoomBySocketId(client.getSessionId());
				userService.removeUserToSocketId(client.getSessionId());
				if (null != roomId) {
					/* user disconnected before joining a session */
					reportActiveUserCountForRoom(roomId);
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

	public void reportDeletedFeedback(final ClientAuthentication user, final Set<de.thm.arsnova.entities.Room> rooms) {
		final List<String> roomShortIds = new ArrayList<>();
		for (final de.thm.arsnova.entities.Room room : rooms) {
			roomShortIds.add(room.getShortId());
		}
		this.sendToUser(user, "feedbackReset", roomShortIds);
	}

	private List<UUID> findConnectionIdForUser(final ClientAuthentication user) {
		final List<UUID> result = new ArrayList<>();
		for (final Entry<UUID, ClientAuthentication> e : userService.getSocketIdToUser()) {
			final UUID someUsersConnectionId = e.getKey();
			final ClientAuthentication someUser = e.getValue();
			if (someUser.equals(user)) {
				result.add(someUsersConnectionId);
			}
		}
		return result;
	}

	private void sendToUser(final ClientAuthentication user, final String event, Object data) {
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
	public void reportRoomDataToClient(final String roomId, final ClientAuthentication user, final SocketIOClient client) {
		final de.thm.arsnova.entities.Room room = roomService.getInternal(roomId, user);
		final de.thm.arsnova.entities.Room.Settings settings = room.getSettings();

		client.sendEvent("unansweredLecturerQuestions", contentService.getUnAnsweredLectureContentIds(roomId, user));
		client.sendEvent("unansweredPreparationQuestions", contentService.getUnAnsweredPreparationContentIds(roomId, user));
		/* FIXME: Content variant is ignored for now */
		client.sendEvent("countLectureQuestionAnswers", contentService.countTotalAnswersByRoomId(roomId));
		client.sendEvent("countPreparationQuestionAnswers", contentService.countTotalAnswersByRoomId(roomId));
		client.sendEvent("activeUserCountData", roomService.activeUsers(roomId));
//		client.sendEvent("learningProgressOptions", room.getLearningProgressOptions());
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getByRoomId(roomId);
		client.sendEvent("feedbackData", fb.getValues());

		if (settings.isFlashcardsEnabled()) {
			client.sendEvent("countFlashcards", contentService.countFlashcardsForUserInternal(roomId));
//			client.sendEvent("flipFlashcards", room.getFlipFlashcards());
		}

		try {
			final long averageFeedback = feedbackService.calculateRoundedAverageFeedback(roomId);
			client.sendEvent("feedbackDataRoundedAverage", averageFeedback);
		} catch (final NoContentException e) {
			final Object object = null; // can't directly use "null".
			client.sendEvent("feedbackDataRoundedAverage", object);
		}
	}

	public void reportUpdatedFeedbackForRoom(final de.thm.arsnova.entities.Room room) {
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getByRoomId(room.getId());
		broadcastInRoom(room.getId(), "feedbackData", fb.getValues());
		try {
			final long averageFeedback = feedbackService.calculateRoundedAverageFeedback(room.getId());
			broadcastInRoom(room.getId(), "feedbackDataRoundedAverage", averageFeedback);
		} catch (final NoContentException e) {
			broadcastInRoom(room.getId(), "feedbackDataRoundedAverage", null);
		}
	}

	public void reportFeedbackForUserInRoom(final Room room, final ClientAuthentication user) {
		final de.thm.arsnova.entities.Feedback fb = feedbackService.getByRoomId(room.getKeyword());
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

	public void reportActiveUserCountForRoom(final String roomId) {
		final int count = userService.getUsersByRoomId(roomId).size();

		broadcastInRoom(roomId, "activeUserCountData", count);
	}

	public void reportAnswersToContentAvailable(final de.thm.arsnova.entities.Room room, final Content content) {
		broadcastInRoom(room.getId(), "answersToLecQuestionAvail", content.get_id());
	}

	public void reportCommentAvailable(final de.thm.arsnova.entities.Room room, final Comment comment) {
		/* TODO role handling implementation, send this only to users with role lecturer */
		broadcastInRoom(room.getId(), "audQuestionAvail", comment.getId());
	}

	public void reportContentAvailable(final de.thm.arsnova.entities.Room room, final List<de.thm.arsnova.entities.Content> qs) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.entities.Content q : qs) {
			contents.add(new Content(q));
		}

		/* TODO role handling implementation, send this only to users with role audience */
		if (!qs.isEmpty()) {
			broadcastInRoom(room.getId(), "lecQuestionAvail", contents.get(0).get_id()); // deprecated!
		}
		broadcastInRoom(room.getId(), "lecturerQuestionAvailable", contents);
	}

	public void reportContentsLocked(final de.thm.arsnova.entities.Room room, final List<de.thm.arsnova.entities.Content> qs) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.entities.Content q : qs) {
			contents.add(new Content(q));
		}
		broadcastInRoom(room.getId(), "lecturerQuestionLocked", contents);
	}

	public void reportRoomStatus(final String roomId, final boolean active) {
		broadcastInRoom(roomId, "setSessionActive", active);
	}

	public void broadcastInRoom(final String roomId, final String eventName, final Object data) {
		/* collect a list of users which are in the current room iterate over
		 * all connected clients and if send feedback, if user is in current
		 * room
		 */
		final Set<ClientAuthentication> users = userService.getUsersByRoomId(roomId);

		for (final SocketIOClient c : server.getAllClients()) {
			final ClientAuthentication u = userService.getUserToSocketId(c.getSessionId());
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
		final String roomId = event.getRoom().getId();
		this.reportAnswersToContentAvailable(event.getRoom(), new Content(event.getContent()));
		broadcastInRoom(roomId, "countQuestionAnswersByQuestionId", contentService.countAnswersAndAbstentionsInternal(event.getContent().getId()));
		/* FIXME: Content variant is ignored for now */
		broadcastInRoom(roomId, "countLectureQuestionAnswers", contentService.countTotalAnswersByRoomId(roomId));
		broadcastInRoom(roomId, "countPreparationQuestionAnswers", contentService.countTotalAnswersByRoomId(roomId));

		// Update the unanswered count for the content variant that was answered.
		final de.thm.arsnova.entities.Content content = event.getContent();
		if ("lecture".equals(content.getGroup())) {
			sendToUser(event.getUser(), "unansweredLecturerQuestions", contentService.getUnAnsweredLectureContentIds(roomId, event.getUser()));
		} else if ("preparation".equals(content.getGroup())) {
			sendToUser(event.getUser(), "unansweredPreparationQuestions", contentService.getUnAnsweredPreparationContentIds(roomId, event.getUser()));
		}
	}

	@Async
	@Override
	@Timed(name = "visit.DeleteAnswerEvent")
	public void visit(DeleteAnswerEvent event) {
		final String roomId = event.getRoom().getId();
		this.reportAnswersToContentAvailable(event.getRoom(), new Content(event.getQuestion()));
		// We do not know which user's answer was deleted, so we can't update his 'unanswered' list of questions...
		/* FIXME: Content variant is ignored for now */
		broadcastInRoom(roomId, "countLectureQuestionAnswers", contentService.countTotalAnswersByRoomId(roomId));
		broadcastInRoom(roomId, "countPreparationQuestionAnswers", contentService.countTotalAnswersByRoomId(roomId));
	}

	@Async
	@Override
	@Timed(name = "visit.PiRoundDelayedStartEvent")
	public void visit(PiRoundDelayedStartEvent event) {
		final String roomId = event.getRoom().getId();
		broadcastInRoom(roomId, "startDelayedPiRound", event.getPiRoundInformations());
	}

	@Async
	@Override
	@Timed(name = "visit.PiRoundEndEvent")
	public void visit(PiRoundEndEvent event) {
		final String roomId = event.getRoom().getId();
		broadcastInRoom(roomId, "endPiRound", event.getPiRoundEndInformations());
	}

	@Async
	@Override
	@Timed(name = "visit.PiRoundCancelEvent")
	public void visit(PiRoundCancelEvent event) {
		final String roomId = event.getRoom().getId();
		broadcastInRoom(roomId, "cancelPiRound", event.getContentId());
	}

	@Override
	public void visit(PiRoundResetEvent event) {
		final String roomId = event.getRoom().getId();
		broadcastInRoom(roomId, "resetPiRound", event.getPiRoundResetInformations());
	}

	@Override
	public void visit(LockVoteEvent event) {
		final String roomId = event.getRoom().getId();
		broadcastInRoom(roomId, "lockVote", event.getVotingAdmission());
	}

	@Override
	public void visit(UnlockVoteEvent event) {
		final String roomId = event.getRoom().getId();
		broadcastInRoom(roomId, "unlockVote", event.getVotingAdmission());
	}

	@Override
	public void visit(LockVotesEvent event) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.entities.Content q : event.getQuestions()) {
			contents.add(new Content(q));
		}
		broadcastInRoom(event.getRoom().getId(), "lockVotes", contents);
	}

	@Override
	public void visit(UnlockVotesEvent event) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.entities.Content q : event.getQuestions()) {
			contents.add(new Content(q));
		}
		broadcastInRoom(event.getRoom().getId(), "unlockVotes", contents);
	}

	@Override
	public void visit(FeatureChangeEvent event) {
		final String roomId = event.getRoom().getId();
		final de.thm.arsnova.entities.Room.Settings settings = event.getRoom().getSettings();
		broadcastInRoom(roomId, "featureChange", toV2Migrator.migrate(settings));

		if (settings.isFlashcardsEnabled()) {
			broadcastInRoom(roomId, "countFlashcards", contentService.countFlashcardsForUserInternal(roomId));
//			broadcastInRoom(roomId, "flipFlashcards", event.getRoom().getFlipFlashcards());
		}
	}

	@Override
	public void visit(LockFeedbackEvent event) {
		broadcastInRoom(event.getRoom().getId(), "lockFeedback", event.getRoom().getSettings().isFeedbackLocked());
	}

	@Override
	public void visit(FlipFlashcardsEvent event) {
//		broadcastInRoom(event.getRoom().getId(), "flipFlashcards", event.getRoom().getFlipFlashcards());
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
		this.reportRoomStatus(event.getRoom().getId(), !event.getRoom().isClosed());
	}

	@Override
	public void visit(ChangeScoreEvent event) {
		broadcastInRoom(event.getRoom().getId(), "learningProgressChange", null);
	}

	@Override
	public void visit(NewRoomEvent event) { }

	@Override
	public void visit(DeleteRoomEvent event) { }
}
