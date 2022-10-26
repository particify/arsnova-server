package de.thm.arsnova.persistence.couchdb.migrations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ektorp.DbAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import de.thm.arsnova.config.properties.CouchDbMigrationProperties;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.MigrationState;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.persistence.couchdb.support.MangoCouchDbConnector;
import de.thm.arsnova.persistence.couchdb.support.PagedMangoResponse;

/**
 * This migration adjusts Contents which have been created through migration of
 * a legacy flashcard before. These flashcards have been previously been stored
 * as slide or text content.
 *
 * @author Daniel Gerhardt
 */
@Service
@ConditionalOnProperty(
		name = "enabled",
		prefix = CouchDbMigrationProperties.PREFIX)
public class MigratedFlashcardMigration implements Migration {
	private static final String ID = "20210223155000";
	private static final int LIMIT = 200;
	private static final String CONTENT_FLASHCARD_INDEX = "migration-20210223155000-content-flashcard-index";
	private static final Logger logger = LoggerFactory.getLogger(MigratedFlashcardMigration.class);

	private final MangoCouchDbConnector connector;

	public MigratedFlashcardMigration(
			final MangoCouchDbConnector connector) {
		this.connector = connector;
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
		filterSelector.put("type", "Content");
		filterSelector.put("extensions.v2.format", "flashcard");
		connector.createPartialJsonIndex(CONTENT_FLASHCARD_INDEX, Collections.emptyList(), filterSelector);
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
		waitForIndex(CONTENT_FLASHCARD_INDEX);

		final Map<String, Object> queryOptions = new HashMap<>();
		queryOptions.put("type", "Content");
		queryOptions.put("extensions.v2.format", "flashcard");
		final MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(queryOptions);
		query.setIndexDocument(CONTENT_FLASHCARD_INDEX);
		query.setLimit(LIMIT);
		String bookmark = (String) state.getState();

		for (int skip = 0;; skip += LIMIT) {
			logger.debug("Migration progress: {}, bookmark: {}", skip, bookmark);
			query.setBookmark(bookmark);
			final PagedMangoResponse<ContentMigrationEntity> response =
					connector.queryForPage(query, ContentMigrationEntity.class);
			final List<ContentMigrationEntity> contents = response.getEntities();
			bookmark = response.getBookmark();
			if (contents.size() == 0) {
				break;
			}

			for (final ContentMigrationEntity content : contents) {
				content.setFormat(Content.Format.FLASHCARD);
				content.getExtensions().remove("v2");
			}

			connector.executeBulk(contents);
			state.setState(bookmark);
		}
		state.setState(null);
	}

	private static class ContentMigrationEntity extends MigrationEntity {
		private Content.Format format;
		private String additionalTextTitle;
		private Map<String, Object> extensions;

		@JsonView(View.Persistence.class)
		public Content.Format getFormat() {
			return format;
		}

		@JsonView(View.Persistence.class)
		public void setFormat(final Content.Format format) {
			this.format = format;
		}

		/* Value obsolete for flashcards. Do not serialize. */
		@JsonIgnore
		public String getAdditionalTextTitle() {
			return additionalTextTitle;
		}

		@JsonView(View.Persistence.class)
		public void setAdditionalTextTitle(final String additionalTextTitle) {
			this.additionalTextTitle = additionalTextTitle;
		}

		@JsonView(View.Persistence.class)
		public Map<String, Object> getExtensions() {
			return extensions;
		}

		@JsonView(View.Persistence.class)
		public void setExtensions(final Map<String, Object> extensions) {
			this.extensions = extensions;
		}
	}
}
