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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.ektorp.DbAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.MigrationState;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.persistence.couchdb.support.MangoCouchDbConnector;
import de.thm.arsnova.persistence.couchdb.support.PagedMangoResponse;

/**
 * This migration fixes correctOptionIndexes of Contents which have been set
 * incorrectly by the webclient. During editing older version of the client only
 * set the points which did work correctly while the client used those points
 * to determine correct options.
 *
 * <p>
 * Furthermore, those points are removed from the
 * {@link de.thm.arsnova.model.ChoiceQuestionContent.AnswerOption}s.
 * </p>
 *
 * @author Daniel Gerhardt
 */
@Service
public class ContentCorrectOptionIndexesMigration implements Migration {
	private static final String ID = "20210325161700";
	private static final int LIMIT = 200;
	private static final String CONTENT_INDEX = "migration-20210325161700-content-index";
	private static final Map<String, Boolean> existsSelector = Map.of("$exists", true);
	private static final Logger logger = LoggerFactory.getLogger(ContentCorrectOptionIndexesMigration.class);

	private MangoCouchDbConnector connector;

	public ContentCorrectOptionIndexesMigration(
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
					migrateContentCorrectOptionIndexes(state);
					break;
				default:
					throw new IllegalStateException("Invalid migration step:" + state.getStep() + ".");
			}
		} catch (final InterruptedException e) {
			throw new DbAccessException(e);
		}
	}

	private void createContentIndex() {
		final Map<String, Object> filterSelector = new HashMap<>();
		filterSelector.put("type", "Content");
		/* Filter by options array which needs to contain at least one element with a points property */
		filterSelector.put("options", Map.of("$elemMatch", Map.of("points", existsSelector)));
		connector.createPartialJsonIndex(CONTENT_INDEX, Collections.emptyList(), filterSelector);
	}

	private void waitForIndex(final String name) throws InterruptedException {
		for (int i = 0; i < 10; i++) {
			if (connector.initializeIndex(name)) {
				return;
			}
			Thread.sleep(10000 * Math.round(1.0 + 0.5 * i));
		}
	}

	public void migrateContentCorrectOptionIndexes(final MigrationState.Migration state) throws InterruptedException {
		createContentIndex();
		waitForIndex(CONTENT_INDEX);

		final Map<String, Object> queryOptions = new HashMap<>();
		queryOptions.put("type", "Content");
		queryOptions.put("options", Map.of("$elemMatch", Map.of("points", existsSelector)));
		final MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(queryOptions);
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
				if (content.getCorrectOptionIndexes().isEmpty()) {
					content.setCorrectOptionIndexes(
							IntStream.range(0, content.getOptions().size())
									.filter(i -> content.getOptions().get(i).getPoints() > 0)
									.boxed()
									.collect(Collectors.toList()));
				}
			}

			/* Update Contents to adjust their models. */
			connector.executeBulk(contents);

			state.setState(bookmark);
		}
		state.setState(null);
	}

	/**
	 * This class is used to access legacy properties for Contents which are no
	 * longer part of the domain model.
	 */
	private static class ContentMigrationEntity extends MigrationEntity {
		private List<AnswerOptionMigrationEntity> options;
		private List<Integer> correctOptionIndexes;

		@JsonView(View.Persistence.class)
		public List<AnswerOptionMigrationEntity> getOptions() {
			return options;
		}

		@JsonView(View.Persistence.class)
		public void setOptions(final List<AnswerOptionMigrationEntity> options) {
			this.options = options;
		}

		@JsonView(View.Persistence.class)
		public List<Integer> getCorrectOptionIndexes() {
			return correctOptionIndexes;
		}

		@JsonView(View.Persistence.class)
		public void setCorrectOptionIndexes(final List<Integer> correctOptionIndexes) {
			this.correctOptionIndexes = correctOptionIndexes;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("options", options)
					.append("correctOptionIndexes", correctOptionIndexes)
					.append("[properties]", getProperties())
					.toString();
		}

		private static class AnswerOptionMigrationEntity extends InnerMigrationEntity {
			private int points;

			/* Legacy property: Do not serialize. */
			@JsonIgnore
			public int getPoints() {
				return points;
			}

			@JsonView(View.Persistence.class)
			public void setPoints(final int points) {
				this.points = points;
			}
		}
	}
}
