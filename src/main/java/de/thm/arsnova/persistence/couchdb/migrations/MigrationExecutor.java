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
package de.thm.arsnova.persistence.couchdb.migrations;

import de.thm.arsnova.model.MigrationState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Run necessary migrations based on the {@link MigrationState} to adjust data to the current entity models.
 *
 * @author Daniel Gerhardt
 */
@Service
public class MigrationExecutor {
	private static final Logger logger = LoggerFactory.getLogger(MigrationExecutor.class);

	private List<Migration> migrations = Collections.EMPTY_LIST;

	public MigrationExecutor(final Optional<List<Migration>> migrations) {
		migrations.map(m -> this.migrations = m.stream()
				.sorted(Comparator.comparing(Migration::getId)).collect(Collectors.toList()));
		logger.debug("Initialized {} migration(s).", this.migrations.size());
	}

	public boolean runMigrations(@NonNull final MigrationState migrationState) {
		List<Migration> pendingMigrations = migrations.stream()
				.filter(m -> !migrationState.getCompleted().contains(m.getId())).collect(Collectors.toList());
		boolean stateChange = false;
		if (migrationState.getActive() != null) {
			throw new IllegalStateException("An migration is already active: " + migrationState.getActive());
		}
		logger.debug("Pending migrations: " + pendingMigrations.stream()
				.map(Migration::getId).collect(Collectors.joining()));
		for (Migration migration : pendingMigrations) {
			stateChange = true;
			migrationState.setActive(migration.getId(), new Date());
			migration.migrate();
			migrationState.getCompleted().add(migration.getId());
			migrationState.setActive(null);
		}

		return stateChange;
	}
}
