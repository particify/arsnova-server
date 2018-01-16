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

import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.entities.migration.FromV2Migrator;
import de.thm.arsnova.entities.migration.v2.DbUser;
import de.thm.arsnova.entities.migration.v2.LoggedIn;
import de.thm.arsnova.entities.migration.v2.MotdList;
import de.thm.arsnova.persistance.UserRepository;
import de.thm.arsnova.persistance.couchdb.support.MangoCouchDbConnector;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performs the data migration from version 2 to version 3.
 *
 * @author Daniel Gerhardt
 */
@Service
public class V2ToV3Migration implements Migration {
	private static final String ID = "20170914131300";
	private static final int LIMIT = 200;
	private static final String FULL_INDEX_BY_TYPE = "full-index-by-type";
	private static final String USER_INDEX = "user-index";
	private static final String LOGGEDIN_INDEX = "loggedin-index";
	private static final String SESSION_INDEX = "session-index";
	private static final String MOTD_INDEX = "motd-index";
	private static final String MOTDLIST_INDEX = "motdlist-index";

	private FromV2Migrator migrator;
	private MangoCouchDbConnector toConnector;
	private MangoCouchDbConnector fromConnector;
	private UserRepository userRepository;

	public V2ToV3Migration(
			final FromV2Migrator migrator,
			final MangoCouchDbConnector toConnector,
			@Qualifier("couchDbMigrationConnector") final MangoCouchDbConnector fromConnector,
			final UserRepository userRepository) {
		this.migrator = migrator;
		this.toConnector = toConnector;
		this.fromConnector = fromConnector;
		this.userRepository = userRepository;
	}

	public String getId() {
		return ID;
	}

	public void migrate() {
		createV2Index();
		migrateUsers();
		migrateRooms();
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

	private void migrateUsers() {
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

				HashMap<String, Object> motdListQueryOptions = new HashMap<>();
				motdListQueryOptions.put("type", "motdlist");
				motdListQueryOptions.put("username", userV2.getUsername());
				MangoCouchDbConnector.MangoQuery motdlistQuery = new MangoCouchDbConnector.MangoQuery(motdListQueryOptions);
				motdlistQuery.setIndexDocument(MOTDLIST_INDEX);
				List<MotdList> motdListList = fromConnector.query(motdlistQuery, MotdList.class);
				MotdList motdList = motdListList.size() > 0 ? motdListList.get(0) : null;

				profilesV3.add(migrator.migrate(userV2, loggedIn, motdList));
			}

			toConnector.executeBulk(profilesV3);
		}
	}

	private void migrateRooms() {
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
				UserProfile profile = userRepository.findByUsername(roomV2.getCreator());
				roomsV3.add(migrator.migrate(roomV2, profile));
			}

			toConnector.executeBulk(roomsV3);
		}
	}
}
