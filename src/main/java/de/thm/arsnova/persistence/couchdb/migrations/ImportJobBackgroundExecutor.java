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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import de.thm.arsnova.config.properties.CouchDbMigrationProperties;
import de.thm.arsnova.model.MigrationState;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.persistence.couchdb.support.MangoCouchDbConnector;
import de.thm.arsnova.persistence.couchdb.support.PagedMangoResponse;

@Service
@ConditionalOnProperty(
		name = "enabled",
		prefix = CouchDbMigrationProperties.PREFIX)
public class ImportJobBackgroundExecutor {
	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonView(View.Persistence.class)
	static class ImportJob {
		private static final String TYPE = "ImportJob";

		private String id;
		private String rev;
		private String userId;
		private String token;
		private String sessionId;
		private ImportMigrationState state;
		private ImportMigrationState migrationState;
		private MigrationState.Migration migration;
		private Date creationTimestamp;

		@JsonProperty("_id")
		public String getId() {
			return id;
		}

		@JsonProperty("_id")
		public void setId(final String id) {
			this.id = id;
		}

		@JsonProperty("_rev")
		public String getRevision() {
			return rev;
		}

		@JsonProperty("_rev")
		public void setRevision(final String rev) {
			this.rev = rev;
		}

		public String getUserId() {
			return userId;
		}

		public void setUserId(final String userId) {
			this.userId = userId;
		}

		public String getToken() {
			return token;
		}

		public void setToken(final String token) {
			this.token = token;
		}

		public String getSessionId() {
			return sessionId;
		}

		public void setSessionId(final String sessionId) {
			this.sessionId = sessionId;
		}

		public ImportMigrationState getState() {
			return state;
		}

		public void setState(final ImportMigrationState state) {
			this.state = state;
		}

		public ImportMigrationState getMigrationState() {
			return migrationState;
		}

		public void setMigrationState(final ImportMigrationState migrationState) {
			this.migrationState = migrationState;
		}

		public MigrationState.Migration getMigration() {
			return migration;
		}

		public void setMigration(final MigrationState.Migration migration) {
			this.migration = migration;
		}

		public Date getCreationTimestamp() {
			return creationTimestamp;
		}

		public void setCreationTimestamp(final Date creationTimestamp) {
			this.creationTimestamp = creationTimestamp;
		}

		public String getType() {
			return TYPE;
		}
	}

	enum ImportMigrationState {
		PENDING,
		IN_PROGRESS,
		FINISHED,
		FAILED
	}

	private static final Logger logger = LoggerFactory.getLogger(ImportJobBackgroundExecutor.class);
	private static final int LIMIT = 10;
	private static final int STEP_COUNT = 6;
	private static final String IMPORTJOB_INDEX = "importjob-index";
	private V2ToV3Migration v2ToV3Migration;
	private MangoCouchDbConnector connector;

	public ImportJobBackgroundExecutor(
			final V2ToV3Migration v2ToV3Migration,
			@Qualifier("couchDbMigrationConnector") final MangoCouchDbConnector connector) {
		this.v2ToV3Migration = v2ToV3Migration;
		this.connector = connector;
	}

	@PostConstruct
	public void init() {
		createImportJobIndex();
		new Thread(() -> runPendingImportJobs()).start();
		logger.info("Started background migration loop to handle import jobs.");
	}

	public void runPendingImportJobs() {
		try {
			Thread.sleep(1000 * 15);
			while (true) {
				final Map<String, Object> queryOptions = new HashMap<>();
				queryOptions.put("type", "ImportJob");
				queryOptions.put("migrationState", "PENDING");
				final MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(queryOptions);
				query.setIndexDocument(IMPORTJOB_INDEX);
				query.setLimit(LIMIT);
				String bookmark = null;

				for (int skip = 0;; skip += LIMIT) {
					logger.debug("Import migration progress: {}, bookmark: {}", skip, bookmark);
					query.setBookmark(bookmark);
					final PagedMangoResponse<ImportJob> importJobResponse =
							connector.queryForPage(query, ImportJob.class);
					bookmark = importJobResponse.getBookmark();
					if (importJobResponse.getEntities().size() == 0) {
						break;
					}

					for (final ImportJob importJob : importJobResponse.getEntities()) {
						logger.info("Running import job ID {} for user ID {}.", importJob.id, importJob.userId);
						final MigrationState.Migration state = importJob.getMigration() != null
								? importJob.getMigration()
								: new MigrationState.Migration(v2ToV3Migration.getId(), new Date());
						importJob.setMigrationState(ImportMigrationState.IN_PROGRESS);
						importJob.setMigration(state);
						connector.update(importJob);
						for (int step = state.getStep(); step < STEP_COUNT; step++) {
							try {
								v2ToV3Migration.migrateForImport(state, importJob);
							} catch (final Exception e) {
								logger.error("Data migration for job ID {} failed.", importJob.id, e);
								importJob.setMigrationState(ImportMigrationState.FAILED);
							}
							state.setStep(step + 1);
							importJob.setMigration(state);
							connector.update(importJob);
						}
						importJob.setMigrationState(ImportMigrationState.FINISHED);
						connector.update(importJob);
						logger.info("Finished import job ID {}.", importJob.id, importJob.userId);
					}
				}
				Thread.sleep(1000 * 10);
			}
		} catch (final InterruptedException e) {
			logger.error("Background execution loop interrupted.", e);
		}
	}

	private void createImportJobIndex() {
		final Map<String, Object> filterSelector = new HashMap<>();
		filterSelector.put("type", "ImportJob");
		final List<MangoCouchDbConnector.MangoQuery.Sort> fields = new ArrayList<>();
		fields.add(new MangoCouchDbConnector.MangoQuery.Sort("migrationState", false));

		connector.createPartialJsonIndex(IMPORTJOB_INDEX, fields, filterSelector);
	}
}
