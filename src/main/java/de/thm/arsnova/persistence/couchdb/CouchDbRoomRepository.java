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

package de.thm.arsnova.persistence.couchdb;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.RoomStatistics;
import de.thm.arsnova.model.migration.v2.ClientAuthentication;
import de.thm.arsnova.model.transport.ImportExportContainer;
import de.thm.arsnova.persistence.LogEntryRepository;
import de.thm.arsnova.persistence.MotdRepository;
import de.thm.arsnova.persistence.RoomRepository;

public class CouchDbRoomRepository extends CouchDbCrudRepository<Room> implements RoomRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbRoomRepository.class);

	@Autowired
	private LogEntryRepository dbLogger;

	@Autowired
	private MotdRepository motdRepository;

	public CouchDbRoomRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Room.class, db, "by_id", createIfNotExists);
	}

	@Override
	public Room findByShortId(final String shortId) {
		if (shortId == null) {
			return null;
		}
		final List<Room> roomList = queryView("by_shortid", shortId);

		return !roomList.isEmpty() ? roomList.get(0) : null;
	}

	/* TODO: Move to service layer. */
	private String getShortId(final String id) throws IOException {
		final Room room = get(id);
		if (room == null) {
			logger.error("No room found for id {}.", id);

			return null;
		}

		return room.getShortId();
	}

	@Override
	public List<Room> findRoomsByCourses(final List<Course> courses) {
		return queryView("by_courseid",
				ComplexKey.of(courses.stream().map(Course::getId).collect(Collectors.toList())));
	}

	@Override
	public List<Room> findInactiveGuestRoomsMetadata(final long lastActivityBefore) {
		final ViewResult result = db.queryView(
				createQuery("by_lastactivity_for_guests").endKey(lastActivityBefore));
		final int[] count = new int[3];

		List<Room> rooms = new ArrayList<>();
		for (final ViewResult.Row row : result.getRows()) {
			final Room s = new Room();
			s.setId(row.getId());
			s.setRevision(row.getValueAsNode().get("_rev").asText());
			rooms.add(s);
		}

		return rooms;
	}

	/* TODO: Move to service layer. */
	@Override
	public Room importRoom(final String userId, final ImportExportContainer importRoom) {
		/* FIXME: not yet migrated - move to service layer */
		throw new UnsupportedOperationException();
	}

	/* TODO: Move to service layer. */
	@Override
	public ImportExportContainer exportRoom(
			final String id,
			final Boolean withAnswers,
			final Boolean withFeedbackQuestions) {
		/* FIXME: not yet migrated - move to service layer */
		throw new UnsupportedOperationException();
	}

	/* TODO: Move to service layer. */
	private Room calculateSessionInfo(final ImportExportContainer importExportSession, final Room room) {
		/* FIXME: not yet migrated - move to service layer */
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Room> findByOwner(final ClientAuthentication owner, final int start, final int limit) {
		return findByOwnerId(owner.getId(), start, limit);
	}

	@Override
	public List<Room> findByOwnerId(final String ownerId, final int start, final int limit) {
		final int qSkip = start > 0 ? start : -1;
		final int qLimit = limit > 0 ? limit : -1;

		/* TODO: Only load IDs and check against cache for data. */
		return db.queryView(
				createQuery("partial_by_pool_ownerid_name")
						.skip(qSkip)
						.limit(qLimit)
						.startKey(ComplexKey.of(false, ownerId))
						.endKey(ComplexKey.of(false, ownerId, ComplexKey.emptyObject()))
						.includeDocs(true),
				Room.class);
	}

	@Override
	public List<String> findIdsByOwnerId(final String ownerId) {
		ViewResult result = db.queryView(createQuery("by_ownerid")
				.key(ownerId)
				.includeDocs(false));

		return result.getRows().stream().map(ViewResult.Row::getId).collect(Collectors.toList());
	}

	@Override
	public List<String> findIdsByModeratorId(final String moderatorId) {
		ViewResult result = db.queryView(createQuery("by_moderators_containing_userid")
				.key(moderatorId)
				.includeDocs(false));

		return result.getRows().stream().map(ViewResult.Row::getId).collect(Collectors.toList());
	}

	@Override
	public List<Room> findAllForPublicPool() {
		// TODO replace with new view
		return queryView("partial_by_category_name_for_pool");
	}

	@Override
	public List<Room> findInfosForPublicPool() {
		final List<Room> rooms = this.findAllForPublicPool();
		return attachStatsForRooms(rooms);
	}

	@Override
	public List<Room> findForPublicPoolByOwnerId(final String ownerId) {
		/* TODO: Only load IDs and check against cache for data. */
		return db.queryView(
				createQuery("partial_by_pool_ownerid_name")
						.startKey(ComplexKey.of(true, ownerId))
						.endKey(ComplexKey.of(true, ownerId, ComplexKey.emptyObject()))
						.includeDocs(true),
				Room.class);
	}

	/* TODO: Move to service layer. */
	@Override
	public List<Room> findInfosForPublicPoolByOwnerId(final String ownerId) {
		final List<Room> rooms = this.findForPublicPoolByOwnerId(ownerId);
		if (rooms.isEmpty()) {
			return new ArrayList<>();
		}
		return attachStatsForRooms(rooms);
	}

	/* TODO: Move to service layer. */
	@Override
	public List<Room> getRoomsWithStatsForOwnerId(final String ownerId, final int start, final int limit) {
		final List<Room> rooms = this.findByOwnerId(ownerId, start, limit);
		if (rooms.isEmpty()) {
			return new ArrayList<>();
		}
		return attachStatsForRooms(rooms);
	}

	/* TODO: Move to service layer. */
	private List<Room> attachStatsForRooms(final List<Room> rooms) {
		final List<String> roomIds = rooms.stream().map(Room::getId).collect(Collectors.toList());
		final ViewQuery questionCountView = createQuery("by_roomid").designDocId("_design/Content")
				.group(true).keys(roomIds);
		final ViewQuery answerCountView = createQuery("by_roomid").designDocId("_design/Answer")
				.group(true).keys(roomIds);
		final ViewQuery commentCountView = createQuery("by_roomid").designDocId("_design/Comment")
				.group(true).keys(roomIds);
		final ViewQuery unreadCommentCountView = createQuery("by_roomid_read").designDocId("_design/Comment")
				.group(true).keys(rooms.stream().map(session -> ComplexKey.of(session.getId(), false))
				.collect(Collectors.toList()));

		return attachStats(rooms, questionCountView, answerCountView, commentCountView, unreadCommentCountView);
	}

	/* TODO: Move to service layer. */
	public List<Room> getRoomHistoryWithStatsForUser(final List<Room> rooms, final String ownerId) {
		final ViewQuery answeredQuestionsView = createQuery("by_creatorid_roomid")
				.designDocId("_design/Answer")
				.reduce(false).keys(rooms.stream().map(room -> ComplexKey.of(ownerId, room.getId()))
				.collect(Collectors.toList()));
		final ViewQuery contentIdsView = createQuery("by_roomid").designDocId("_design/Content")
				.reduce(false).keys(rooms.stream().map(Room::getId).collect(Collectors.toList()));

		return attachRoomHistoryStats(rooms, answeredQuestionsView, contentIdsView);
	}

	/* TODO: Move to service layer. */
	private List<Room> attachRoomHistoryStats(
			final List<Room> rooms,
			final ViewQuery answeredQuestionsView,
			final ViewQuery contentIdsView) {
		final Map<String, Set<String>> answeredQuestionsMap = new HashMap<>();
		final Map<String, Set<String>> contentIdMap = new HashMap<>();

		// Maps a room ID to a set of question IDs of answered questions of that room
		for (final ViewResult.Row row : db.queryView(answeredQuestionsView).getRows()) {
			final String roomId = row.getKey();
			final String contentId = row.getValue();
			Set<String> contentIdsInRoom = answeredQuestionsMap.get(roomId);
			if (contentIdsInRoom == null) {
				contentIdsInRoom = new HashSet<>();
			}
			contentIdsInRoom.add(contentId);
			answeredQuestionsMap.put(roomId, contentIdsInRoom);
		}

		// Maps a room ID to a set of question IDs of that room
		for (final ViewResult.Row row : db.queryView(contentIdsView).getRows()) {
			final String roomId = row.getKey();
			final String contentId = row.getId();
			Set<String> contentIdsInRoom = contentIdMap.get(roomId);
			if (contentIdsInRoom == null) {
				contentIdsInRoom = new HashSet<>();
			}
			contentIdsInRoom.add(contentId);
			contentIdMap.put(roomId, contentIdsInRoom);
		}

		// For each room, count the question IDs that are not yet answered
		final Map<String, Integer> unansweredQuestionsCountMap = new HashMap<>();
		for (final Room s : rooms) {
			if (!contentIdMap.containsKey(s.getId())) {
				continue;
			}
			// Note: create a copy of the first set so that we don't modify the contents in the original set
			final Set<String> contentIdsInRoom = new HashSet<>(contentIdMap.get(s.getId()));
			Set<String> answeredContentIdsInRoom = answeredQuestionsMap.get(s.getId());
			if (answeredContentIdsInRoom == null) {
				answeredContentIdsInRoom = new HashSet<>();
			}
			contentIdsInRoom.removeAll(answeredContentIdsInRoom);
			unansweredQuestionsCountMap.put(s.getId(), contentIdsInRoom.size());
		}

		for (final Room room : rooms) {
			int numUnanswered = 0;

			if (unansweredQuestionsCountMap.containsKey(room.getId())) {
				numUnanswered = unansweredQuestionsCountMap.get(room.getId());
			}
			RoomStatistics stats = new RoomStatistics();
			room.setStatistics(stats);
			stats.setUnansweredContentCount(numUnanswered);
		}
		return rooms;
	}

	/* TODO: Move to service layer. */
	private List<Room> attachStats(
			final List<Room> rooms,
			final ViewQuery questionCountView,
			final ViewQuery answerCountView,
			final ViewQuery commentCountView,
			final ViewQuery unreadCommentCountView) {
		final Map<String, Integer> questionCountMap = db.queryView(questionCountView).getRows()
				.stream().map(row -> new AbstractMap.SimpleImmutableEntry<>(row.getKey(), row.getValueAsInt()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		final Map<String, Integer> answerCountMap = db.queryView(answerCountView).getRows()
				.stream().map(row -> new AbstractMap.SimpleImmutableEntry<>(row.getKey(), row.getValueAsInt()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		final Map<String, Integer> commentCountMap = db.queryView(commentCountView).getRows()
				.stream().map(row -> new AbstractMap.SimpleImmutableEntry<>(row.getKey(), row.getValueAsInt()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		final Map<String, Integer> unreadCommentCountMap = db.queryView(unreadCommentCountView).getRows()
				.stream().map(row -> new AbstractMap.SimpleImmutableEntry<>(row.getKey(), row.getValueAsInt()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		for (final Room room : rooms) {
			int numQuestions = 0;
			int numAnswers = 0;
			int numComments = 0;
			int numUnreadComments = 0;
			if (questionCountMap.containsKey(room.getId())) {
				numQuestions = questionCountMap.get(room.getId());
			}
			if (answerCountMap.containsKey(room.getId())) {
				numAnswers = answerCountMap.get(room.getId());
			}
			if (commentCountMap.containsKey(room.getId())) {
				numComments = commentCountMap.get(room.getId());
			}
			if (unreadCommentCountMap.containsKey(room.getId())) {
				numUnreadComments = unreadCommentCountMap.get(room.getId());
			}

			final RoomStatistics stats = new RoomStatistics();
			room.setStatistics(stats);
			stats.setContentCount(numQuestions);
			stats.setAnswerCount(numAnswers);
			stats.setCommentCount(numComments);
			stats.setUnreadCommentCount(numUnreadComments);
		}
		return rooms;
	}
}
