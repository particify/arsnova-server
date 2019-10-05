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

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.MigrationState;

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

	public boolean runMigrations(@NonNull final MigrationState migrationState, final Runnable stateUpdateHandler) {
		final Thread shutdownHook = new Thread(stateUpdateHandler);
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		final List<Migration> pendingMigrations = migrations.stream()
				.filter(m -> !migrationState.getCompleted().contains(m.getId())).collect(Collectors.toList());
		boolean stateChange = false;
		logger.info("Pending migrations: " + pendingMigrations.stream()
				.map(Migration::getId).collect(Collectors.joining()));
		for (final Migration migration : pendingMigrations) {
			if (migrationState.getActive() != null) {
				logger.info("Trying to continue from aborted migration: " + migrationState.getActive());
				if (pendingMigrations.isEmpty()
						|| migrationState.getActive().getId().equals(pendingMigrations.get(0))) {
					throw new IllegalStateException("Migration state does not match next pending migration.");
				}
			} else {
				migrationState.setActive(migration.getId(), new Date());
			}
			stateChange = true;
			final int initialStep = migrationState.getActive() != null ? migrationState.getActive().getStep() : 0;
			for (int i = initialStep; i < migration.getStepCount(); i++) {
				logger.info("Performing migration {} step {}...", migration.getId(), i);
				try {
					migration.migrate(migrationState.getActive());
				} catch (final Exception e) {
					logger.info("Current migration state: {}", migrationState);
					stateUpdateHandler.run();
					throw e;
				}
				migrationState.getActive().setStep(i + 1);
				logger.info("Completed migration {} step {}.", migration.getId(), i);
				stateUpdateHandler.run();
			}
			migrationState.getCompleted().add(migration.getId());
			migrationState.setActive(null);
		}
		Runtime.getRuntime().removeShutdownHook(shutdownHook);

		return stateChange;
	}
}
