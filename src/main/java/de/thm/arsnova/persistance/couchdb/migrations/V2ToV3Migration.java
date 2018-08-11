/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team
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
package de.thm.arsnova.persistance.couchdb.migrations;

import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.Motd;
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.entities.migration.FromV2Migrator;
import de.thm.arsnova.entities.migration.v2.DbUser;
import de.thm.arsnova.entities.migration.v2.LoggedIn;
import de.thm.arsnova.entities.migration.v2.MotdList;
import de.thm.arsnova.persistance.ContentRepository;
import de.thm.arsnova.persistance.RoomRepository;
import de.thm.arsnova.persistance.UserRepository;
import de.thm.arsnova.persistance.couchdb.support.MangoCouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Performs the data migration from version 2 to version 3.
 *
 * @author Daniel Gerhardt
 */
@Conditional(MigrateFromLegacyCondition.class)
@Service
public class V2ToV3Migration implements Migration {
	private static final String ID = "20170914131300";
	private static final int LIMIT = 200;
	private static final long OUTDATED_AFTER = 1000L * 3600 * 24 * 30 * 6;
	private static final String FULL_INDEX_BY_TYPE = "full-index-by-type";
	private static final String USER_INDEX = "user-index";
	private static final String LOGGEDIN_INDEX = "loggedin-index";
	private static final String SESSION_INDEX = "session-index";
	private static final String MOTD_INDEX = "motd-index";
	private static final String MOTDLIST_INDEX = "motdlist-index";

	private static final Logger logger = LoggerFactory.getLogger(V2ToV3Migration.class);

	private FromV2Migrator migrator;
	private MangoCouchDbConnector toConnector;
	private MangoCouchDbConnector fromConnector;
	private UserRepository userRepository;
	private RoomRepository roomRepository;
	private ContentRepository contentRepository;
	private long referenceTimestamp = System.currentTimeMillis();

	public V2ToV3Migration(
			final FromV2Migrator migrator,
			final MangoCouchDbConnector toConnector,
			@Qualifier("couchDbMigrationConnector") final MangoCouchDbConnector fromConnector,
			final UserRepository userRepository,
			final RoomRepository roomRepository,
			final ContentRepository contentRepository) {
		this.migrator = migrator;
		this.toConnector = toConnector;
		this.fromConnector = fromConnector;
		this.userRepository = userRepository;
		this.roomRepository = roomRepository;
		this.contentRepository = contentRepository;
	}

	public String getId() {
		return ID;
	}

	public void migrate() {
		createV2Index();
		migrator.setIgnoreRevision(true);
		try {
			migrateUsers();
			migrateUnregisteredUsers();
			migrateRooms();
			migrateMotds();
			migrateComments();
			migrateContents();
			migrateAnswers();
		} catch (InterruptedException e) {
			throw new DbAccessException(e);
		}
		migrator.setIgnoreRevision(false);
	}

	private void createV2Index() {
		List<MangoCouchDbConnector.MangoQuery.Sort> fields;
		Map<String, Object> filterSelector;
		Map<String, Object> subFilterSelector;

		fields = new ArrayList<>();
		fields.add(new MangoCouchDbConnector.MangoQuery.Sort("type", false));
		fromConnector.createJsonIndex(FULL_INDEX_BY_TYPE, fields);

		filterSelector = new HashMap<>();
		filterSelector.put("type", "userdetails");
		Map<String, String> lockedFilter = new HashMap<>();
		subFilterSelector = new HashMap<>();
		subFilterSelector.put("$exists", false);
		filterSelector.put("locked", subFilterSelector);
		fromConnector.createPartialJsonIndex(USER_INDEX, new ArrayList<>(), filterSelector);
		fields = new ArrayList<>();
		fields.add(new MangoCouchDbConnector.MangoQuery.Sort("username", false));
		fromConnector.createPartialJsonIndex(USER_INDEX, fields, filterSelector);

		filterSelector = new HashMap<>();
		filterSelector.put("type", "logged_in");
		fromConnector.createPartialJsonIndex(LOGGEDIN_INDEX, new ArrayList<>(), filterSelector);
		fields = new ArrayList<>();
		fields.add(new MangoCouchDbConnector.MangoQuery.Sort("user", false));
		fromConnector.createPartialJsonIndex(LOGGEDIN_INDEX, fields, filterSelector);

		filterSelector = new HashMap<>();
		filterSelector.put("type", "session");
		fromConnector.createPartialJsonIndex(SESSION_INDEX, new ArrayList<>(), filterSelector);
		fields = new ArrayList<>();
		fields.add(new MangoCouchDbConnector.MangoQuery.Sort("keyword", false));
		fromConnector.createPartialJsonIndex(SESSION_INDEX, fields, filterSelector);

		filterSelector = new HashMap<>();
		filterSelector.put("type", "motd");
		fromConnector.createPartialJsonIndex(MOTD_INDEX, new ArrayList<>(), filterSelector);
		fields = new ArrayList<>();
		fields.add(new MangoCouchDbConnector.MangoQuery.Sort("motdkey", false));
		fromConnector.createPartialJsonIndex(MOTD_INDEX, fields, filterSelector);

		filterSelector = new HashMap<>();
		filterSelector.put("type", "motdlist");
		fromConnector.createPartialJsonIndex(MOTDLIST_INDEX, new ArrayList<>(), filterSelector);
		fields = new ArrayList<>();
		fields.add(new MangoCouchDbConnector.MangoQuery.Sort("username", false));
		fromConnector.createPartialJsonIndex(MOTDLIST_INDEX, fields, filterSelector);
	}

	private void waitForV2Index(final String name) throws InterruptedException {
		for (int i = 0; i < 10; i++) {
			if (fromConnector.initializeIndex(name)) {
				return;
			}
			Thread.sleep(10000 * Math.round(1.0 + 0.5 * i));
		}
	}

	private void migrateUsers() throws InterruptedException {
		waitForV2Index(USER_INDEX);
		waitForV2Index(LOGGEDIN_INDEX);
		Map<String, Object> queryOptions = new HashMap<>();
		queryOptions.put("type", "userdetails");
		MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(queryOptions);
		query.setIndexDocument(USER_INDEX);
		query.setLimit(LIMIT);

		for (int skip = 0;; skip += LIMIT) {
			query.setSkip(skip);
			List<UserProfile> profilesV3 = new ArrayList<>();
			List<DbUser> dbUsersV2 = fromConnector.query(query, DbUser.class);
			if (dbUsersV2.size() == 0) {
				break;
			}

			for (DbUser userV2 : dbUsersV2) {
				HashMap<String, Object> loggedInQueryOptions = new HashMap<>();
				loggedInQueryOptions.put("type", "logged_in");
				loggedInQueryOptions.put("user", userV2.getUsername());
				MangoCouchDbConnector.MangoQuery loggedInQuery = new MangoCouchDbConnector.MangoQuery(loggedInQueryOptions);
				loggedInQuery.setIndexDocument(LOGGEDIN_INDEX);
				List<LoggedIn> loggedInList = fromConnector.query(loggedInQuery, LoggedIn.class);
				LoggedIn loggedIn = loggedInList.size() > 0 ? loggedInList.get(0) : null;

				UserProfile profileV3 = migrator.migrate(userV2, loggedIn, loadMotdList(userV2.getUsername()));
				profileV3.setAcknowledgedMotds(migrateMotdIds(profileV3.getAcknowledgedMotds()));
				profilesV3.add(profileV3);
			}

			toConnector.executeBulk(profilesV3);
		}
	}

	private void migrateUnregisteredUsers() throws InterruptedException {
		waitForV2Index(USER_INDEX);
		waitForV2Index(LOGGEDIN_INDEX);
		/* Load registered usernames to exclude them later */
		Map<String, Object> queryOptions = new HashMap<>();
		queryOptions.put("type", "userdetails");
		MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(queryOptions);
		query.setIndexDocument(USER_INDEX);
		query.setLimit(LIMIT);
		Set<String> usernames = new HashSet<>();
		for (int skip = 0;; skip += LIMIT) {
			query.setSkip(skip);
			List<String> result = fromConnector.query(query, "username", String.class);
			if (result.isEmpty()) {
				break;
			}
			usernames.addAll(result);
		}

		queryOptions = new HashMap<>();
		queryOptions.put("type", "logged_in");
		query = new MangoCouchDbConnector.MangoQuery(queryOptions);
		query.setIndexDocument(LOGGEDIN_INDEX);
		query.setLimit(LIMIT);
		for (int skip = 0;; skip += LIMIT) {
			query.setSkip(skip);
			List<UserProfile> profilesV3 = new ArrayList<>();
			List<LoggedIn> loggedInsV2 = fromConnector.query(query, LoggedIn.class);
			if (loggedInsV2.isEmpty()) {
				break;
			}
			for (LoggedIn loggedInV2 : loggedInsV2) {
				if (usernames.contains(loggedInV2.getUser())) {
					continue;
				}
				/* There might be rare cases of duplicate LoggedIn records for a user so add them to the filter list */
				usernames.add(loggedInV2.getUser());
				UserProfile profileV3 = migrator.migrate(null, loggedInV2, loadMotdList(loggedInV2.getUser()));
				profileV3.setAcknowledgedMotds(migrateMotdIds(profileV3.getAcknowledgedMotds()));
				profilesV3.add(profileV3);
			}
			toConnector.executeBulk(profilesV3);
		}
	}

	private void migrateRooms() throws InterruptedException {
		waitForV2Index(SESSION_INDEX);
		Map<String, Object> queryOptions = new HashMap<>();
		queryOptions.put("type", "session");
		MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(queryOptions);
		query.setIndexDocument(SESSION_INDEX);
		query.setLimit(LIMIT);

		for (int skip = 0;; skip += LIMIT) {
			query.setSkip(skip);
			List<Room> roomsV3 = new ArrayList<>();
			List<de.thm.arsnova.entities.migration.v2.Room> roomsV2 = fromConnector.query(query,
					de.thm.arsnova.entities.migration.v2.Room.class);
			if (roomsV2.size() == 0) {
				break;
			}

			for (de.thm.arsnova.entities.migration.v2.Room roomV2 : roomsV2) {
				List<UserProfile> profiles = userRepository.findByLoginId(roomV2.getCreator());
				if (profiles.size() == 0) {
					logger.warn("Skipping migration of Room {}. Creator {} does not exist.",
							roomV2.getId(), roomV2.getCreator());
					continue;
				}
				roomsV3.add(migrator.migrate(roomV2, Optional.of(profiles.get(0))));
			}

			toConnector.executeBulk(roomsV3);
		}
	}

	private void migrateMotds() throws InterruptedException {
		waitForV2Index(MOTD_INDEX);
		Map<String, Object> queryOptions = new HashMap<>();
		queryOptions.put("type", "motd");
		/* Exclude outdated MotDs */
		HashMap<String, String> subQuery = new HashMap<>();
		subQuery.put("$gt", String.valueOf(referenceTimestamp - OUTDATED_AFTER));
		queryOptions.put("enddate", subQuery);
		MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(queryOptions);
		query.setIndexDocument(MOTD_INDEX);
		query.setLimit(LIMIT);

		for (int skip = 0;; skip += LIMIT) {
			query.setSkip(skip);
			List<Motd> motdsV3 = new ArrayList<>();
			List<de.thm.arsnova.entities.migration.v2.Motd> motdsV2 = fromConnector.query(query,
					de.thm.arsnova.entities.migration.v2.Motd.class);
			if (motdsV2.size() == 0) {
				break;
			}

			for (de.thm.arsnova.entities.migration.v2.Motd motdV2 : motdsV2) {
				if (motdV2.getAudience().equals("session")) {
					Room room = roomRepository.findByShortId(motdV2.getSessionkey());
					/* sessionId has not been set for some old MotDs */
					if (room == null) {
						logger.warn("Skipping migration of Motd {}. Room {} does not exist.",
								motdV2.getId(), motdV2.getSessionId());
						continue;
					}
					motdV2.setSessionId(room.getId());
				}
				motdsV3.add(migrator.migrate(motdV2));
			}

			toConnector.executeBulk(motdsV3);
		}
	}

	private void migrateComments() throws InterruptedException {
		waitForV2Index(FULL_INDEX_BY_TYPE);
		Map<String, Object> queryOptions = new HashMap<>();
		queryOptions.put("type", "interposed_question");
		MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(queryOptions);
		query.setIndexDocument(FULL_INDEX_BY_TYPE);
		query.setLimit(LIMIT);

		for (int skip = 0;; skip += LIMIT) {
			query.setSkip(skip);
			List<Comment> commentsV3 = new ArrayList<>();
			List<de.thm.arsnova.entities.migration.v2.Comment> commentsV2 = fromConnector.query(query,
					de.thm.arsnova.entities.migration.v2.Comment.class);
			if (commentsV2.size() == 0) {
				break;
			}

			for (de.thm.arsnova.entities.migration.v2.Comment commentV2 : commentsV2) {
				try {
					Room roomV3 = roomRepository.findOne(commentV2.getSessionId());
					List<UserProfile> profiles = Collections.EMPTY_LIST;
					if (commentV2.getCreator() != null && !commentV2.getCreator().equals("")) {
						profiles = userRepository.findByLoginId(commentV2.getCreator());
					}
					if (profiles.size() == 0) {
						/* No creator is set or creator does not exist -> fallback: creator = Room owner */
						commentV2.setCreator(null);
						Comment commentV3 = migrator.migrate(commentV2);
						commentV3.setCreatorId(roomV3.getOwnerId());
						commentsV3.add(commentV3);
					} else {
						commentsV3.add(migrator.migrate(commentV2, profiles.get(0)));
					}
				} catch (DocumentNotFoundException e) {
					logger.warn("Skipping migration of Comment {}. Room {} does not exist.",
							commentV2.getId(), commentV2.getSessionId());
					continue;
				}
			}

			toConnector.executeBulk(commentsV3);
		}
	}

	private void migrateContents() throws InterruptedException {
		waitForV2Index(FULL_INDEX_BY_TYPE);
		Map<String, Object> queryOptions = new HashMap<>();
		queryOptions.put("type", "skill_question");
		MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(queryOptions);
		query.setIndexDocument(FULL_INDEX_BY_TYPE);
		query.setLimit(LIMIT);

		for (int skip = 0;; skip += LIMIT) {
			query.setSkip(skip);
			List<Content> contentsV3 = new ArrayList<>();
			List<de.thm.arsnova.entities.migration.v2.Content> contentsV2 = fromConnector.query(query,
					de.thm.arsnova.entities.migration.v2.Content.class);
			if (contentsV2.size() == 0) {
				break;
			}

			for (de.thm.arsnova.entities.migration.v2.Content contentV2 : contentsV2) {
				if (roomRepository.existsById(contentV2.getSessionId())) {
					try {
						contentsV3.add(migrator.migrate(contentV2));
					} catch (IllegalArgumentException e) {
						logger.warn("Skipping migration of Content {}.", contentV2.getId(), e);
					}
				} else {
					logger.warn("Skipping migration of Content {}. Room {} does not exist.",
							contentV2.getId(), contentV2.getSessionId());
				}
			}

			toConnector.executeBulk(contentsV3);
		}
	}

	private void migrateAnswers() throws InterruptedException {
		waitForV2Index(FULL_INDEX_BY_TYPE);
		Map<String, Object> queryOptions = new HashMap<>();
		queryOptions.put("type", "skill_question_answer");
		MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(queryOptions);
		query.setIndexDocument(FULL_INDEX_BY_TYPE);
		query.setLimit(LIMIT);

		for (int skip = 0;; skip += LIMIT) {
			query.setSkip(skip);
			List<Answer> answersV3 = new ArrayList<>();
			List<de.thm.arsnova.entities.migration.v2.Answer> answersV2 = fromConnector.query(query,
					de.thm.arsnova.entities.migration.v2.Answer.class);
			if (answersV2.size() == 0) {
				break;
			}

			for (de.thm.arsnova.entities.migration.v2.Answer answerV2 : answersV2) {
				if (!roomRepository.existsById(answerV2.getSessionId())) {
					logger.warn("Skipping migration of Answer {}. Room {} does not exist.",
							answerV2.getId(), answerV2.getQuestionId());
					continue;
				}
				try {
					Content contentV3 = contentRepository.findOne(answerV2.getQuestionId());
					answersV3.add(migrator.migrate(answerV2, contentV3));
				} catch (DocumentNotFoundException e) {
					logger.warn("Skipping migration of Answer {}. Content {} does not exist.",
							answerV2.getId(), answerV2.getQuestionId());
					continue;
				} catch (IndexOutOfBoundsException e) {
					logger.warn("Skipping migration of Answer {}. Data inconsistency detected.", answerV2.getId());
				}
			}

			toConnector.executeBulk(answersV3);
		}
	}

	private HashSet<String> migrateMotdIds(final Set<String> oldIds) throws InterruptedException {
		if (oldIds.isEmpty()) {
			return new HashSet<>();
		}
		waitForV2Index(MOTD_INDEX);
		Map<String, Object> queryOptions = new HashMap<>();
		Map<String, Set<String>> subQuery1 = new HashMap<>();
		subQuery1.put("$in", oldIds);
		queryOptions.put("type", "motd");
		queryOptions.put("motdkey", subQuery1);
		/* Exclude outdated MotDs */
		HashMap<String, String> subQuery2 = new HashMap<>();
		subQuery2.put("$gt", String.valueOf(referenceTimestamp - OUTDATED_AFTER));
		queryOptions.put("enddate", subQuery2);
		MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(queryOptions);
		query.setIndexDocument(MOTD_INDEX);
		query.setLimit(LIMIT);

		return new HashSet<>(fromConnector.query(query, "_id", String.class));
	}

	private MotdList loadMotdList(final String username) throws InterruptedException {
		waitForV2Index(MOTDLIST_INDEX);
		HashMap<String, Object> motdListQueryOptions = new HashMap<>();
		motdListQueryOptions.put("type", "motdlist");
		motdListQueryOptions.put("username", username);
		MangoCouchDbConnector.MangoQuery motdListQuery = new MangoCouchDbConnector.MangoQuery(motdListQueryOptions);
		motdListQuery.setIndexDocument(MOTDLIST_INDEX);
		List<MotdList> motdListList = fromConnector.query(motdListQuery, MotdList.class);

		return motdListList.size() > 0 ? motdListList.get(0) : null;
	}
}
