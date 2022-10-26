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

package de.thm.arsnova.persistence.couchdb.migrations;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ektorp.DbAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.MigrationState;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.persistence.couchdb.support.MangoCouchDbConnector;
import de.thm.arsnova.persistence.couchdb.support.PagedMangoResponse;

/**
 * This migration sets the closed flag to false for Rooms that have been
 * imported.
 *
 * @author Daniel Gerhardt
 */
@Service
public class ImportedRoomUnsetClosedFlagMigration implements Migration {
	private static final String ID = "20210319160400";
	private static final int LIMIT = 200;
	private static final String ROOM_INDEX = "migration-20210319160400-room-imported-closed-index";
	private static final Logger logger = LoggerFactory.getLogger(ImportedRoomUnsetClosedFlagMigration.class);

	private MangoCouchDbConnector connector;

	public ImportedRoomUnsetClosedFlagMigration(
			final MangoCouchDbConnector connector) {
		this.connector = connector;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public int getStepCount() {
		return 1;
	}

	@Override
	public void migrate(final MigrationState.Migration state) {
		try {
			switch (state.getStep()) {
				case 0:
					migrateImportedRoom(state);
					break;
				default:
					throw new IllegalStateException("Invalid migration step:" + state.getStep() + ".");
			}
		} catch (final InterruptedException e) {
			throw new DbAccessException(e);
		}
	}

	private void createRoomIndex() {
		final Map<String, Object> filterSelector = new HashMap<>();
		filterSelector.put("type", "Room");
		filterSelector.put("closed", true);
		filterSelector.put("importMetadata", Map.of("source", "V2_IMPORT"));
		connector.createPartialJsonIndex(ROOM_INDEX, Collections.emptyList(), filterSelector);
	}

	private void waitForIndex(final String name) throws InterruptedException {
		for (int i = 0; i < 10; i++) {
			if (connector.initializeIndex(name)) {
				return;
			}
			Thread.sleep(10000 * Math.round(1.0 + 0.5 * i));
		}
	}

	public void migrateImportedRoom(final MigrationState.Migration state) throws InterruptedException {
		createRoomIndex();
		waitForIndex(ROOM_INDEX);

		final Map<String, Object> queryOptions = new HashMap<>();
		queryOptions.put("type", "Room");
		queryOptions.put("closed", true);
		queryOptions.put("importMetadata", Map.of("source", "V2_IMPORT"));
		final MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(queryOptions);
		query.setLimit(LIMIT);
		String bookmark = (String) state.getState();

		for (int skip = 0; ; skip += LIMIT) {
			logger.debug("Migration progress: {}, bookmark: {}", skip, bookmark);
			query.setBookmark(bookmark);
			final PagedMangoResponse<RoomMigrationEntity> response =
					connector.queryForPage(
							query,
							RoomMigrationEntity.class);
			final List<RoomMigrationEntity> rooms = response.getEntities();
			bookmark = response.getBookmark();
			if (rooms.size() == 0) {
				break;
			}

			for (final RoomMigrationEntity room : rooms) {
				room.setClosed(false);
			}

			connector.executeBulk(rooms);
			state.setState(bookmark);
		}
		state.setState(null);
	}

	private static class RoomMigrationEntity extends MigrationEntity {
		private boolean closed;

		@JsonView(View.Persistence.class)
		public boolean isClosed() {
			return closed;
		}

		@JsonView(View.Persistence.class)
		public void setClosed(final boolean closed) {
			this.closed = closed;
		}
	}
}
