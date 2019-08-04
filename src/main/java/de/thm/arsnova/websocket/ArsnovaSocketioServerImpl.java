/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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
import io.micrometer.core.annotation.Timed;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import de.thm.arsnova.event.AfterCreationEvent;
import de.thm.arsnova.event.AfterDeletionEvent;
import de.thm.arsnova.event.ChangeScoreEvent;
import de.thm.arsnova.event.DeleteFeedbackForRoomsEvent;
import de.thm.arsnova.event.FlipFlashcardsEvent;
import de.thm.arsnova.event.NewFeedbackEvent;
import de.thm.arsnova.event.StateChangeEvent;
import de.thm.arsnova.model.Answer;
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
	private boolean useSsl = false;
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

		final SocketConfig soConfig = new SocketConfig();
		soConfig.setReuseAddress(true);
		config.setSocketConfig(soConfig);
		config.setPort(portNumber);
		config.setHostname(hostIp);
		if (useSsl) {
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
			@Timed("setFeedbackEvent.onData")
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
			@Timed("setSessionEvent.onData")
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
					@Timed("readInterposedQuestionEvent.onData")
					public void onData(
							final SocketIOClient client,
							final Comment comment,
							final AckRequest ackRequest) {
						final String user = userService.getUserIdToSocketId(client.getSessionId());
						try {
							commentService.getAndMarkRead(comment.getId());
						} catch (final IOException | NotFoundException | UnauthorizedException e) {
							logger.error("Loading of comment {} failed for user {} with exception {}",
									comment.getId(), user, e.getMessage());
						}
					}
				});

		server.addEventListener("readFreetextAnswer", String.class, new DataListener<String>() {
			@Override
			public void onData(final SocketIOClient client, final String answerId, final AckRequest ackRequest) {
				final String userId = userService.getUserIdToSocketId(client.getSessionId());
				try {
					answerService.getFreetextAnswerAndMarkRead(answerId, userId);
				} catch (final NotFoundException | UnauthorizedException e) {
					logger.error("Marking answer {} as read failed for user {} with exception {}",
							answerId, userId, e.getMessage());
				}
			}
		});

		server.addEventListener(
				"setLearningProgressOptions",
				ScoreOptions.class,
				new DataListener<ScoreOptions>() {
					@Override
					@Timed("setLearningProgressOptionsEvent.onData")
					public void onData(
							final SocketIOClient client, final ScoreOptions scoreOptions, final AckRequest ack) {
						throw new UnsupportedOperationException("Not implemented.");
						/* FIXME: missing implementation */
					}
				});

		server.addConnectListener(new ConnectListener() {
			@Override
			@Timed("onConnect")
			public void onConnect(final SocketIOClient client) {

			}
		});

		server.addDisconnectListener(new DisconnectListener() {
			@Override
			@Timed("onDisconnect")
			public void onDisconnect(final SocketIOClient client) {
				if (
						userService == null
						|| client.getSessionId() == null
						|| userService.getUserIdToSocketId(client.getSessionId()) == null) {
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
	public boolean isUseSsl() {
		return useSsl;
	}

	@Required
	public void setUseSsl(final boolean useSsl) {
		this.useSsl = useSsl;
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

	private void sendToUser(final String userId, final String event, final Object data) {
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

		client.sendEvent("unansweredLecturerQuestions",
				contentService.getUnAnsweredLectureContentIds(roomId, userId));
		client.sendEvent("unansweredPreparationQuestions",
				contentService.getUnAnsweredPreparationContentIds(roomId, userId));
		/* FIXME: Content variant is ignored for now */
		client.sendEvent("countLectureQuestionAnswers", answerService.countTotalAnswersByRoomId(roomId));
		client.sendEvent("countPreparationQuestionAnswers", answerService.countTotalAnswersByRoomId(roomId));
		client.sendEvent("activeUserCountData", roomService.activeUsers(roomId));
		/* FIXME: missing implementation */
		//client.sendEvent("learningProgressOptions", room.getLearningProgressOptions());
		final de.thm.arsnova.model.Feedback fb = feedbackService.getByRoomId(roomId);
		client.sendEvent("feedbackData", fb.getValues());

		if (settings.isFlashcardsEnabled()) {
			client.sendEvent("countFlashcards", contentService.countFlashcardsForUserInternal(roomId));
			/* FIXME: missing implementation */
			//client.sendEvent("flipFlashcards", room.getFlipFlashcards());
		}

		try {
			final long averageFeedback = feedbackService.calculateRoundedAverageFeedback(roomId);
			client.sendEvent("feedbackDataRoundedAverage", averageFeedback);
		} catch (final NoContentException e) {
			final Object object = null; // can't directly use "null".
			client.sendEvent("feedbackDataRoundedAverage", object);
		}
	}

	public void reportUpdatedFeedbackForRoom(final String roomId) {
		final de.thm.arsnova.model.Feedback fb = feedbackService.getByRoomId(roomId);
		broadcastInRoom(roomId, "feedbackData", fb.getValues());
		try {
			final long averageFeedback = feedbackService.calculateRoundedAverageFeedback(roomId);
			broadcastInRoom(roomId, "feedbackDataRoundedAverage", averageFeedback);
		} catch (final NoContentException e) {
			broadcastInRoom(roomId, "feedbackDataRoundedAverage", null);
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

	public void reportAnswersToContentAvailable(final String roomId, final String contentId) {
		broadcastInRoom(roomId, "answersToLecQuestionAvail", contentId);
	}

	public void reportCommentAvailable(final String roomId, final String commentId) {
		/* TODO role handling implementation, send this only to users with role lecturer */
		broadcastInRoom(roomId, "audQuestionAvail", commentId);
	}

	public void reportContentAvailable(final String roomId, final List<de.thm.arsnova.model.Content> qs) {
		final List<Content> contents = new ArrayList<>();
		for (final de.thm.arsnova.model.Content q : qs) {
			contents.add(new Content(q));
		}

		/* TODO role handling implementation, send this only to users with role audience */
		if (!qs.isEmpty()) {
			broadcastInRoom(roomId, "lecQuestionAvail", contents.get(0).getId()); // deprecated!
		}
		broadcastInRoom(roomId, "lecturerQuestionAvailable", contents);
	}

	public void reportContentsLocked(final String roomId, final List<de.thm.arsnova.model.Content> qs) {
		final List<Content> contents = new ArrayList<>();
		for (final de.thm.arsnova.model.Content q : qs) {
			contents.add(new Content(q));
		}
		broadcastInRoom(roomId, "lecturerQuestionLocked", contents);
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
	public void handleAfterContentCreation(final AfterCreationEvent<de.thm.arsnova.model.Content> event) {
		this.reportContentAvailable(event.getEntity().getId(), Collections.singletonList(event.getEntity()));
	}

	@EventListener(condition = "#event.stateName == 'state'")
	public void handleContentIsVisibleStateChange(
			final StateChangeEvent<de.thm.arsnova.model.Content, de.thm.arsnova.model.Content.State> event) {
		if (event.getEntity().getState().isVisible()) {
			this.reportContentAvailable(event.getEntity().getRoomId(), Collections.singletonList(event.getEntity()));
		} else {
			this.reportContentsLocked(event.getEntity().getRoomId(), Collections.singletonList(event.getEntity()));
		}
	}

	@EventListener
	public void handleAfterCommentCreation(final AfterCreationEvent<Comment> event) {
		this.reportCommentAvailable(event.getEntity().getId(), event.getEntity().getId());
	}

	@Async
	@EventListener
	@Timed
	public void handleNewAnswer(final AfterCreationEvent<Answer> event) {
		final String roomId = event.getEntity().getRoomId();
		this.reportAnswersToContentAvailable(event.getEntity().getRoomId(), event.getEntity().getContentId());
		broadcastInRoom(roomId, "countQuestionAnswersByQuestionId",
				answerService.countAnswersAndAbstentionsInternal(event.getEntity().getContentId()));
		/* FIXME: Content variant is ignored for now */
		broadcastInRoom(roomId, "countLectureQuestionAnswers",
				answerService.countTotalAnswersByRoomId(roomId));
		broadcastInRoom(roomId, "countPreparationQuestionAnswers",
				answerService.countTotalAnswersByRoomId(roomId));

		// Update the unanswered count for the content variant that was answered.
		/* Is this still relevant?
		 * FIXME: Send unansweredLecturerQuestions and unansweredPreparationQuestions event messages.
		 **/
	}

	@Async
	@EventListener
	@Timed
	public void handleAfterAnswerDeletion(final AfterDeletionEvent<Answer> event) {
		final String roomId = event.getEntity().getRoomId();
		this.reportAnswersToContentAvailable(event.getEntity().getRoomId(), event.getEntity().getContentId());
		// We do not know which user's answer was deleted, so we can't update his 'unanswered' list of questions...
		/* FIXME: Content variant is ignored for now */
		broadcastInRoom(roomId, "countLectureQuestionAnswers", answerService.countTotalAnswersByRoomId(roomId));
		broadcastInRoom(roomId, "countPreparationQuestionAnswers", answerService.countTotalAnswersByRoomId(roomId));
	}

	@Async
	@EventListener(condition = "#event.stateName == 'state'")
	@Timed
	public void handlePiRoundDelayedStart(
			final StateChangeEvent<de.thm.arsnova.model.Content, de.thm.arsnova.model.Content.State> event) {
		broadcastInRoom(event.getEntity().getRoomId(), "startDelayedPiRound",
				generateRoundInfo(event.getEntity()));
	}

	@Async
	@EventListener(condition = "#event.stateName == 'state'")
	@Timed
	public void handlePiRoundEnd(
			final StateChangeEvent<de.thm.arsnova.model.Content, de.thm.arsnova.model.Content.State> event) {
		broadcastInRoom(event.getEntity().getRoomId(), "endPiRound", generateRoundInfo(event.getEntity()));
	}

	@Async
	@EventListener(condition = "#event.stateName == 'state'")
	@Timed
	public void handlePiRoundCancel(
			final StateChangeEvent<de.thm.arsnova.model.Content, de.thm.arsnova.model.Content.State> event) {
		broadcastInRoom(event.getEntity().getRoomId(), "cancelPiRound", event.getEntity().getId());
	}

	@EventListener(condition = "#event.stateName == 'state'")
	public void handlePiRoundReset(
			final StateChangeEvent<de.thm.arsnova.model.Content, de.thm.arsnova.model.Content.State> event) {
		broadcastInRoom(event.getEntity().getRoomId(), "resetPiRound", generateRoundInfo(event.getEntity()));
	}

	private Map<String, Object> generateRoundInfo(final de.thm.arsnova.model.Content content) {
		final Map<String, Object> map = new HashMap<>();
		map.put("_id", content.getId());
		if (content.getState().getRoundEndTimestamp() != null) {
			map.put("endTime", content.getState().getRoundEndTimestamp().getTime());
		}
		/* FIXME: getRoundStartTimestamp is not implemented for Content.State. Is a delayed start still useful? */
		/*
		if (content.getState().getRoundStartTimestamp() != null) {
			map.put("startTime", content.getState().getRoundStartTimestamp().getTime());
		}
		*/
		map.put("variant", content.getGroups());
		map.put("round", content.getState().getRound());

		return map;
	}

	@EventListener(condition = "#event.stateName == 'state'")
	public void handleContentResponsesEnabledStateChange(
			final StateChangeEvent<de.thm.arsnova.model.Content, de.thm.arsnova.model.Content.State> event) {
		/* Multiple groups for a single Content are not handled. */
		final String groupName = event.getEntity().getGroups().iterator().hasNext()
				? event.getEntity().getGroups().iterator().next() : "";
		final Map<String, Object> map = new HashMap<>();
		map.put("_id", event.getEntity().getId());
		map.put("variant", groupName);
		if (event.getEntity().getState().isResponsesEnabled()) {
			this.reportContentAvailable(event.getEntity().getRoomId(), Collections.singletonList(event.getEntity()));
			broadcastInRoom(event.getEntity().getRoomId(), "unlockVote", map);
		} else {
			broadcastInRoom(event.getEntity().getRoomId(), "lockVote", map);
		}
	}

	@EventListener(condition = "#event.stateName == 'settings'")
	public void handleFeatureChange(
			final StateChangeEvent<de.thm.arsnova.model.Room, de.thm.arsnova.model.Room.Settings> event) {
		final String roomId = event.getEntity().getId();
		final de.thm.arsnova.model.Room.Settings settings = event.getEntity().getSettings();
		broadcastInRoom(roomId, "featureChange", toV2Migrator.migrate(settings));

		if (settings.isFlashcardsEnabled()) {
			broadcastInRoom(roomId, "countFlashcards",
					contentService.countFlashcardsForUserInternal(roomId));
			/* FIXME: missing implementation */
			//broadcastInRoom(roomId, "flipFlashcards", event.getEntity().getSettings().isFlipFlashcards());
		}
	}

	@EventListener(condition = "#event.stateName == 'settings'")
	public void handleLockFeedback(
			final StateChangeEvent<de.thm.arsnova.model.Room, de.thm.arsnova.model.Room.Settings> event) {
		broadcastInRoom(event.getEntity().getId(), "lockFeedback",
				event.getEntity().getSettings().isFeedbackLocked());
	}

	@EventListener
	public void handleFlipFlashcards(final FlipFlashcardsEvent event) {
		/* FIXME: missing implementation */
		//broadcastInRoom(event.getRoom().getId(), "flipFlashcards", event.getRoom().getFlipFlashcards());
	}

	@EventListener
	public void handleNewFeedback(final NewFeedbackEvent event) {
		this.reportUpdatedFeedbackForRoom(event.getRoomId());
	}

	@EventListener
	public void handleDeleteFeedbackForRooms(final DeleteFeedbackForRoomsEvent event) {
		this.reportDeletedFeedback(event.getUserId(), event.getSessions());

	}

	@EventListener(condition = "#event.stateName == 'closed'")
	public void handleRoomClosedStateChange(final StateChangeEvent<de.thm.arsnova.model.Room, Boolean> event) {
		this.reportRoomStatus(event.getEntity().getId(), !event.getNewValue());
	}

	@EventListener
	public void handleChangeScore(final ChangeScoreEvent event) {
		broadcastInRoom(event.getRoomId(), "learningProgressChange", null);
	}
}
