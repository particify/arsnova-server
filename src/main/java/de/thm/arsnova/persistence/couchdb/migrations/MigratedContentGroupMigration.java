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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.ektorp.DbAccessException;
import org.ektorp.ViewQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import de.thm.arsnova.config.properties.CouchDbMigrationProperties;
import de.thm.arsnova.model.MigrationState;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.persistence.couchdb.support.MangoCouchDbConnector;
import de.thm.arsnova.persistence.couchdb.support.PagedMangoResponse;

/**
 * This migration adjusts ContentGroups which have been created through
 * migration before. It restores the order of contents and replaces the group's
 * name with a mapped group name. The order is only updated for ContentGroups
 * which have not yet been modified manually by the user.
 *
 * @author Daniel Gerhardt
 */
@Service
@ConditionalOnProperty(
		name = "enabled",
		prefix = CouchDbMigrationProperties.PREFIX)
public class MigratedContentGroupMigration implements Migration {
	private static final String ID = "20201208172400";
	private static final int LIMIT = 200;
	private static final String CONTENT_GROUP_INDEX = "migration-20201208172400-contentgroup-index";
	private static final String CONTENT_DESIGN_DOC = "_design/Content";
	private static final String CONTENT_BY_ID_VIEW = "by_id";
	private static final Map<String, Boolean> notExistsSelector = Map.of("$exists", false);
	private static final Logger logger = LoggerFactory.getLogger(MigratedContentGroupMigration.class);

	private final MangoCouchDbConnector connector;
	private Map<String, String> contentGroupNames;

	public MigratedContentGroupMigration(
			final MangoCouchDbConnector connector,
			final CouchDbMigrationProperties couchDbMigrationProperties) {
		this.connector = connector;
		this.contentGroupNames = couchDbMigrationProperties.getContentGroupNames();
	}

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
					migrateContentGroups(state);
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
		filterSelector.put("type", "ContentGroup");
		/* The creationTimestamp was not set for migrated ContentGroups in the
		 * past, so its non-existence is a feature of migrated ContentGroups. */
		filterSelector.put("creationTimestamp", notExistsSelector);
		connector.createPartialJsonIndex(CONTENT_GROUP_INDEX, Collections.emptyList(), filterSelector);
	}

	private void waitForIndex(final String name) throws InterruptedException {
		for (int i = 0; i < 10; i++) {
			if (connector.initializeIndex(name)) {
				return;
			}
			Thread.sleep(10000 * Math.round(1.0 + 0.5 * i));
		}
	}

	private void migrateContentGroups(final MigrationState.Migration state) throws InterruptedException {
		createIndex();
		waitForIndex(CONTENT_GROUP_INDEX);

		final Map<String, Object> queryOptions = new HashMap<>();
		queryOptions.put("type", "ContentGroup");
		queryOptions.put("creationTimestamp", notExistsSelector);
		final MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(queryOptions);
		query.setIndexDocument(CONTENT_GROUP_INDEX);
		query.setLimit(LIMIT);
		String bookmark = (String) state.getState();

		for (int skip = 0;; skip += LIMIT) {
			logger.debug("Migration progress: {}, bookmark: {}", skip, bookmark);
			query.setBookmark(bookmark);
			final PagedMangoResponse<ContentGroupMigrationEntity> response =
					connector.queryForPage(query, ContentGroupMigrationEntity.class);
			final List<ContentGroupMigrationEntity> contentGroups = response.getEntities();
			bookmark = response.getBookmark();
			if (contentGroups.size() == 0) {
				break;
			}

			for (final ContentGroupMigrationEntity contentGroup : contentGroups) {
				contentGroup.setName(contentGroupNames.getOrDefault(contentGroup.getName(), contentGroup.getName()));
				contentGroup.setAutoSort(false);
				/* Apply alphanumerical sorting if the ContentGroup has not yet been manually modified. */
				if (contentGroup.getUpdateTimestamp() == null) {
					final List<ContentMigrationEntity> contents = connector.queryView(new ViewQuery()
							.designDocId(CONTENT_DESIGN_DOC)
							.viewName(CONTENT_BY_ID_VIEW)
							.keys(contentGroup.getContentIds())
							.reduce(false)
							.includeDocs(true),
							ContentMigrationEntity.class);
					contentGroup.setContentIds(
							contents.stream()
									.sorted(Comparator.comparing(ContentMigrationEntity::getBody))
									.map(ContentMigrationEntity::getId)
									.collect(Collectors.toList()));
				}
				final RoomMigrationEntity room = connector.get(RoomMigrationEntity.class, contentGroup.getRoomId());
				contentGroup.setCreationTimestamp(room.getCreationTimestamp());
			}

			connector.executeBulk(contentGroups);
			state.setState(bookmark);
		}
		state.setState(null);
	}

	private static class ContentGroupMigrationEntity extends MigrationEntity {
		private String roomId;
		private String name;
		private List<String> contentIds;
		private boolean autoSort;

		@JsonView(View.Persistence.class)
		public String getRoomId() {
			return roomId;
		}

		@JsonView(View.Persistence.class)
		public void setRoomId(final String roomId) {
			this.roomId = roomId;
		}

		@JsonView(View.Persistence.class)
		public String getName() {
			return name;
		}

		@JsonView(View.Persistence.class)
		public void setName(final String name) {
			this.name = name;
		}

		@JsonView(View.Persistence.class)
		public List<String> getContentIds() {
			return contentIds;
		}

		@JsonView(View.Persistence.class)
		public void setContentIds(final List<String> contentIds) {
			this.contentIds = contentIds;
		}

		@JsonView(View.Persistence.class)
		public boolean isAutoSort() {
			return autoSort;
		}

		@JsonView(View.Persistence.class)
		public void setAutoSort(final boolean autoSort) {
			this.autoSort = autoSort;
		}
	}

	private static class ContentMigrationEntity extends MigrationEntity {
		private String body;

		@JsonView(View.Persistence.class)
		public String getBody() {
			return body;
		}

		@JsonView(View.Persistence.class)
		public void setBody(final String body) {
			this.body = body;
		}
	}

	private static class RoomMigrationEntity extends MigrationEntity {
	}
}
