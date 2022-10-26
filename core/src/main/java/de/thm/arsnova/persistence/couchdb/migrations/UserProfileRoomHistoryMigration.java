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
import java.util.stream.Collectors;
import org.ektorp.DbAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import de.thm.arsnova.event.RoomHistoryMigrationEvent;
import de.thm.arsnova.model.MigrationState;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.persistence.couchdb.support.MangoCouchDbConnector;
import de.thm.arsnova.persistence.couchdb.support.PagedMangoResponse;

/**
 * This migration only creates events for {@link de.thm.arsnova.model.UserProfile.RoomHistoryEntry}s
 * of {@link de.thm.arsnova.model.UserProfile}s which are obsolete and will be
 * removed by a later migration. It does not perform any database changes.
 *
 * @author Daniel Gerhardt
 */
@Service
public class UserProfileRoomHistoryMigration implements Migration {
	private static final String ID = "20210511192000";
	private static final int LIMIT = 200;
	private static final String USER_PROFILE_INDEX = "migration-20210511192000-userprofile-index";
	private static final Map<String, Map<String, Integer>> arrayNotEmptySelector =
			Map.of("$not", Map.of("$size", 0));
	private static final Logger logger = LoggerFactory.getLogger(UserProfileRoomHistoryMigration.class);

	private final MangoCouchDbConnector connector;
	private final ApplicationEventPublisher applicationEventPublisher;

	public UserProfileRoomHistoryMigration(
			final MangoCouchDbConnector connector, final ApplicationEventPublisher applicationEventPublisher) {
		this.connector = connector;
		this.applicationEventPublisher = applicationEventPublisher;
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
					migrateRoomHistories(state);
					break;
				default:
					throw new IllegalStateException("Invalid migration step:" + state.getStep() + ".");
			}
		} catch (final InterruptedException e) {
			throw new DbAccessException(e);
		}
	}

	private void createIndex() {
		final Map<String, Object> filterSelector = new HashMap<>();
		filterSelector.put("type", "UserProfile");
		filterSelector.put("roomHistory", arrayNotEmptySelector);
		connector.createPartialJsonIndex(USER_PROFILE_INDEX, Collections.emptyList(), filterSelector);
	}

	private void waitForIndex(final String name) throws InterruptedException {
		for (int i = 0; i < 10; i++) {
			if (connector.initializeIndex(name)) {
				return;
			}
			Thread.sleep(10000 * Math.round(1.0 + 0.5 * i));
		}
	}

	private void migrateRoomHistories(final MigrationState.Migration state) throws InterruptedException {
		createIndex();
		waitForIndex(USER_PROFILE_INDEX);

		final Map<String, Object> queryOptions = new HashMap<>();
		queryOptions.put("type", "UserProfile");
		queryOptions.put("roomHistory", arrayNotEmptySelector);
		final MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(queryOptions);
		query.setIndexDocument(USER_PROFILE_INDEX);
		query.setLimit(LIMIT);
		String bookmark = (String) state.getState();

		for (int skip = 0;; skip += LIMIT) {
			logger.debug("Migration progress: {}, bookmark: {}", skip, bookmark);
			query.setBookmark(bookmark);
			final PagedMangoResponse<UserProfileMigrationEntity> response =
					connector.queryForPage(query, UserProfileMigrationEntity.class);
			final List<UserProfileMigrationEntity> userProfiles = response.getEntities();
			bookmark = response.getBookmark();
			if (userProfiles.size() == 0) {
				break;
			}

			for (final UserProfileMigrationEntity userProfile : userProfiles) {
				this.applicationEventPublisher.publishEvent(new RoomHistoryMigrationEvent(
						this,
						userProfile.getId(),
						userProfile.getRoomHistory().stream().map(rh -> rh.roomId).collect(Collectors.toList())));
			}
		}
	}

	@JsonView(View.Persistence.class)
	private static class UserProfileMigrationEntity extends MigrationEntity {
		private List<RoomHistoryEntryMigrationEntity> roomHistory;

		public List<RoomHistoryEntryMigrationEntity> getRoomHistory() {
			return roomHistory;
		}

		public void setRoomHistory(final List<RoomHistoryEntryMigrationEntity> roomHistory) {
			this.roomHistory = roomHistory;
		}

		@JsonView(View.Persistence.class)
		public static class RoomHistoryEntryMigrationEntity extends InnerMigrationEntity {
			private String roomId;

			public String getRoomId() {
				return roomId;
			}

			public void setRoomId(final String roomId) {
				this.roomId = roomId;
			}
		}
	}
}
