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
import de.thm.arsnova.event.*;
import de.thm.arsnova.model.Comment;
import de.thm.arsnova.model.ScoreOptions;
import de.thm.arsnova.model.migration.ToV2Migrator;
import de.thm.arsnova.service.AnswerService;
import de.thm.arsnova.service.CommentService;
import de.thm.arsnova.service.ContentService;
import de.thm.arsnova.service.FeedbackService;
import de.thm.arsnova.service.RoomService;
import de.thm.arsnova.service.UserService;
import de.thm.arsnova.web.exceptions.NoContentException;
import de.thm.arsnova.web.exceptions.NotFoundException;
import de.thm.arsnova.web.exceptions.UnauthorizedException;
import de.thm.arsnova.websocket.message.Content;
import de.thm.arsnova.websocket.message.Feedback;
import de.thm.arsnova.websocket.message.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.event.EventListener;
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
public class ArsnovaSocketioServerImpl implements ArsnovaSocketioServer {

	@Autowired
	private FeedbackService feedbackService;

	@Autowired
	private UserService userService;

	@Autowired
	private RoomService roomService;

	@Autowired
	private ContentService contentService;

	@Autowired
	private AnswerService answerService;

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
				final String userId = userService.getUserIdToSocketId(client.getSessionId());
				if (userId == null) {
					logger.info("Client {} tried to send feedback but is not mapped to a user", client.getSessionId());

					return;
				}
				final String roomId = userService.getRoomIdByUserId(userId);
				final de.thm.arsnova.model.Room room = roomService.getInternal(roomId, userId);

				if (room.getSettings().isFeedbackLocked()) {
					logger.debug("Feedback ignored: User: {}, Room Id: {}, Feedback: {}", userId, roomId, data.getValue());
				} else {
					logger.debug("Feedback received: User: {}, Room Id: {}, Feedback: {}", userId, roomId, data.getValue());
					if (null != roomId) {
						feedbackService.save(roomId, data.getValue(), userId);
					}
				}
			}
		});

		server.addEventListener("setSession", Room.class, new DataListener<Room>() {
			@Override
			@Timed(name = "setSessionEvent.onData")
			public void onData(final SocketIOClient client, final Room room, final AckRequest ackSender) {
				final String userId = userService.getUserIdToSocketId(client.getSessionId());
				if (null == userId) {
					logger.info("Client {} requested to join room but is not mapped to a user", client.getSessionId());

					return;
				}
				final String oldRoomId = userService.getRoomIdByUserId(userId);
				if (null != room.getKeyword()) {
					if (room.getKeyword().equals(oldRoomId)) {
						return;
					}
					final String roomId = roomService.getIdByShortId(room.getKeyword());

					if (null != roomId && null != roomService.join(roomId, client.getSessionId())) {
						/* active user count has to be sent to the client since the broadcast is
						 * not always sent as long as the polling solution is active simultaneously */
						reportActiveUserCountForRoom(roomId);
						reportRoomDataToClient(roomId, userId, client);
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
				final String user = userService.getUserIdToSocketId(client.getSessionId());
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
				final String userId = userService.getUserIdToSocketId(client.getSessionId());
				try {
					answerService.getFreetextAnswerAndMarkRead(answerId, userId);
				} catch (NotFoundException | UnauthorizedException e) {
					logger.error("Marking answer {} as read failed for user {} with exception {}", answerId, userId, e.getMessage());
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
						|| userService.getUserIdToSocketId(client.getSessionId()) == null
						) {
					return;
				}
				final String userId = userService.getUserIdToSocketId(client.getSessionId());
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

	public void reportDeletedFeedback(final String userId, final Set<de.thm.arsnova.model.Room> rooms) {
		final List<String> roomShortIds = new ArrayList<>();
		for (final de.thm.arsnova.model.Room room : rooms) {
			roomShortIds.add(room.getShortId());
		}
		this.sendToUser(userId, "feedbackReset", roomShortIds);
	}

	private List<UUID> findConnectionIdForUserId(final String userId) {
		final List<UUID> result = new ArrayList<>();
		for (final Entry<UUID, String> e : userService.getSocketIdToUserId()) {
			final UUID someUsersConnectionId = e.getKey();
			final String someUser = e.getValue();
			if (someUser.equals(userId)) {
				result.add(someUsersConnectionId);
			}
		}
		return result;
	}

	private void sendToUser(final String userId, final String event, Object data) {
		final List<UUID> connectionIds = findConnectionIdForUserId(userId);
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
	public void reportRoomDataToClient(final String roomId, final String userId, final SocketIOClient client) {
		final de.thm.arsnova.model.Room room = roomService.getInternal(roomId, userId);
		final de.thm.arsnova.model.Room.Settings settings = room.getSettings();

		client.sendEvent("unansweredLecturerQuestions", contentService.getUnAnsweredLectureContentIds(roomId, userId));
		client.sendEvent("unansweredPreparationQuestions", contentService.getUnAnsweredPreparationContentIds(roomId, userId));
		/* FIXME: Content variant is ignored for now */
		client.sendEvent("countLectureQuestionAnswers", answerService.countTotalAnswersByRoomId(roomId));
		client.sendEvent("countPreparationQuestionAnswers", answerService.countTotalAnswersByRoomId(roomId));
		client.sendEvent("activeUserCountData", roomService.activeUsers(roomId));
//		client.sendEvent("learningProgressOptions", room.getLearningProgressOptions());
		final de.thm.arsnova.model.Feedback fb = feedbackService.getByRoomId(roomId);
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

	public void reportUpdatedFeedbackForRoom(final de.thm.arsnova.model.Room room) {
		final de.thm.arsnova.model.Feedback fb = feedbackService.getByRoomId(room.getId());
		broadcastInRoom(room.getId(), "feedbackData", fb.getValues());
		try {
			final long averageFeedback = feedbackService.calculateRoundedAverageFeedback(room.getId());
			broadcastInRoom(room.getId(), "feedbackDataRoundedAverage", averageFeedback);
		} catch (final NoContentException e) {
			broadcastInRoom(room.getId(), "feedbackDataRoundedAverage", null);
		}
	}

	public void reportFeedbackForUserInRoom(final String roomId, final String userId) {
		final de.thm.arsnova.model.Feedback fb = feedbackService.getByRoomId(roomId);
		Long averageFeedback;
		try {
			averageFeedback = feedbackService.calculateRoundedAverageFeedback(roomId);
		} catch (final NoContentException e) {
			averageFeedback = null;
		}
		final List<UUID> connectionIds = findConnectionIdForUserId(userId);
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

	public void reportAnswersToContentAvailable(final de.thm.arsnova.model.Room room, final Content content) {
		broadcastInRoom(room.getId(), "answersToLecQuestionAvail", content.get_id());
	}

	public void reportCommentAvailable(final de.thm.arsnova.model.Room room, final Comment comment) {
		/* TODO role handling implementation, send this only to users with role lecturer */
		broadcastInRoom(room.getId(), "audQuestionAvail", comment.getId());
	}

	public void reportContentAvailable(final de.thm.arsnova.model.Room room, final List<de.thm.arsnova.model.Content> qs) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.model.Content q : qs) {
			contents.add(new Content(q));
		}

		/* TODO role handling implementation, send this only to users with role audience */
		if (!qs.isEmpty()) {
			broadcastInRoom(room.getId(), "lecQuestionAvail", contents.get(0).get_id()); // deprecated!
		}
		broadcastInRoom(room.getId(), "lecturerQuestionAvailable", contents);
	}

	public void reportContentsLocked(final de.thm.arsnova.model.Room room, final List<de.thm.arsnova.model.Content> qs) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.model.Content q : qs) {
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
		final Set<String> userIds = userService.getUsersByRoomId(roomId);

		for (final SocketIOClient c : server.getAllClients()) {
			final String userId = userService.getUserIdToSocketId(c.getSessionId());
			if (userId != null && userIds.contains(userId)) {
				c.sendEvent(eventName, data);
			}
		}
	}

	@EventListener
	public void handleNewQuestion(NewQuestionEvent event) {
		this.reportContentAvailable(event.getRoom(), Collections.singletonList(event.getQuestion()));
	}

	@EventListener
	public void handleUnlockQuestion(UnlockQuestionEvent event) {
		this.reportContentAvailable(event.getRoom(), Collections.singletonList(event.getQuestion()));
	}

	@EventListener
	public void handleLockQuestion(LockQuestionEvent event) {
		this.reportContentsLocked(event.getRoom(), Collections.singletonList(event.getQuestion()));
	}

	@EventListener
	public void handleUnlockQuestions(UnlockQuestionsEvent event) {
		this.reportContentAvailable(event.getRoom(), event.getQuestions());
	}

	@EventListener
	public void handleLockQuestions(LockQuestionsEvent event) {
		this.reportContentsLocked(event.getRoom(), event.getQuestions());
	}

	@EventListener
	public void handleNewComment(NewCommentEvent event) {
		this.reportCommentAvailable(event.getRoom(), event.getQuestion());
	}

	@Async
	@EventListener
	@Timed
	public void handleNewAnswer(NewAnswerEvent event) {
		final String roomId = event.getRoom().getId();
		this.reportAnswersToContentAvailable(event.getRoom(), new Content(event.getContent()));
		broadcastInRoom(roomId, "countQuestionAnswersByQuestionId", answerService.countAnswersAndAbstentionsInternal(event.getContent().getId()));
		/* FIXME: Content variant is ignored for now */
		broadcastInRoom(roomId, "countLectureQuestionAnswers", answerService.countTotalAnswersByRoomId(roomId));
		broadcastInRoom(roomId, "countPreparationQuestionAnswers", answerService.countTotalAnswersByRoomId(roomId));

		// Update the unanswered count for the content variant that was answered.
		final de.thm.arsnova.model.Content content = event.getContent();
		if (content.getGroups().contains("lecture")) {
			sendToUser(event.getUserId(), "unansweredLecturerQuestions", contentService.getUnAnsweredLectureContentIds(roomId, event.getUserId()));
		} else if (content.getGroups().contains("preparation")) {
			sendToUser(event.getUserId(), "unansweredPreparationQuestions", contentService.getUnAnsweredPreparationContentIds(roomId, event.getUserId()));
		}
	}

	@Async
	@EventListener
	@Timed
	public void handleDeleteAnswer(DeleteAnswerEvent event) {
		final String roomId = event.getRoom().getId();
		this.reportAnswersToContentAvailable(event.getRoom(), new Content(event.getQuestion()));
		// We do not know which user's answer was deleted, so we can't update his 'unanswered' list of questions...
		/* FIXME: Content variant is ignored for now */
		broadcastInRoom(roomId, "countLectureQuestionAnswers", answerService.countTotalAnswersByRoomId(roomId));
		broadcastInRoom(roomId, "countPreparationQuestionAnswers", answerService.countTotalAnswersByRoomId(roomId));
	}

	@Async
	@EventListener
	@Timed
	public void handlePiRoundDelayedStart(PiRoundDelayedStartEvent event) {
		final String roomId = event.getRoom().getId();
		broadcastInRoom(roomId, "startDelayedPiRound", event.getPiRoundInformations());
	}

	@Async
	@EventListener
	@Timed
	public void handlePiRoundEnd(PiRoundEndEvent event) {
		final String roomId = event.getRoom().getId();
		broadcastInRoom(roomId, "endPiRound", event.getPiRoundEndInformations());
	}

	@Async
	@EventListener
	@Timed
	public void handlePiRoundCancel(PiRoundCancelEvent event) {
		final String roomId = event.getRoom().getId();
		broadcastInRoom(roomId, "cancelPiRound", event.getContentId());
	}

	@EventListener
	public void handlePiRoundReset(PiRoundResetEvent event) {
		final String roomId = event.getRoom().getId();
		broadcastInRoom(roomId, "resetPiRound", event.getPiRoundResetInformations());
	}

	@EventListener
	public void handleLockVote(LockVoteEvent event) {
		final String roomId = event.getRoom().getId();
		broadcastInRoom(roomId, "lockVote", event.getVotingAdmission());
	}

	@EventListener
	public void handleUnlockVote(UnlockVoteEvent event) {
		final String roomId = event.getRoom().getId();
		broadcastInRoom(roomId, "unlockVote", event.getVotingAdmission());
	}

	@EventListener
	public void handleLockVotes(LockVotesEvent event) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.model.Content q : event.getQuestions()) {
			contents.add(new Content(q));
		}
		broadcastInRoom(event.getRoom().getId(), "lockVotes", contents);
	}

	@EventListener
	public void handleUnlockVotes(UnlockVotesEvent event) {
		List<Content> contents = new ArrayList<>();
		for (de.thm.arsnova.model.Content q : event.getQuestions()) {
			contents.add(new Content(q));
		}
		broadcastInRoom(event.getRoom().getId(), "unlockVotes", contents);
	}

	@EventListener
	public void handleFeatureChange(FeatureChangeEvent event) {
		final String roomId = event.getRoom().getId();
		final de.thm.arsnova.model.Room.Settings settings = event.getRoom().getSettings();
		broadcastInRoom(roomId, "featureChange", toV2Migrator.migrate(settings));

		if (settings.isFlashcardsEnabled()) {
			broadcastInRoom(roomId, "countFlashcards", contentService.countFlashcardsForUserInternal(roomId));
//			broadcastInRoom(roomId, "flipFlashcards", event.getRoom().getFlipFlashcards());
		}
	}

	@EventListener
	public void handleLockFeedback(LockFeedbackEvent event) {
		broadcastInRoom(event.getRoom().getId(), "lockFeedback", event.getRoom().getSettings().isFeedbackLocked());
	}

	@EventListener
	public void handleFlipFlashcards(FlipFlashcardsEvent event) {
//		broadcastInRoom(event.getRoom().getId(), "flipFlashcards", event.getRoom().getFlipFlashcards());
	}

	@EventListener
	public void handleNewFeedback(NewFeedbackEvent event) {
		this.reportUpdatedFeedbackForRoom(event.getRoom());
	}

	@EventListener
	public void handleDeleteFeedbackForRooms(DeleteFeedbackForRoomsEvent event) {
		this.reportDeletedFeedback(event.getUserId(), event.getSessions());

	}

	@EventListener
	public void handleStatusRoom(StatusRoomEvent event) {
		this.reportRoomStatus(event.getRoom().getId(), !event.getRoom().isClosed());
	}

	@EventListener
	public void handleChangeScore(ChangeScoreEvent event) {
		broadcastInRoom(event.getRoom().getId(), "learningProgressChange", null);
	}
}
