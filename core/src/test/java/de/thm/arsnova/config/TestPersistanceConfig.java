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

package de.thm.arsnova.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;

import de.thm.arsnova.config.properties.CouchDbMigrationProperties;
import de.thm.arsnova.persistence.AnswerRepository;
import de.thm.arsnova.persistence.ContentGroupRepository;
import de.thm.arsnova.persistence.ContentRepository;
import de.thm.arsnova.persistence.LogEntryRepository;
import de.thm.arsnova.persistence.RoomRepository;
import de.thm.arsnova.persistence.StatisticsRepository;
import de.thm.arsnova.persistence.UserRepository;
import de.thm.arsnova.persistence.couchdb.support.MangoCouchDbConnector;

@TestConfiguration
@EnableConfigurationProperties(CouchDbMigrationProperties.class)
@Profile("test")
public class TestPersistanceConfig {
	@MockBean
	private MangoCouchDbConnector mangoCouchDbConnector;

	@MockBean
	private LogEntryRepository logEntryRepository;

	@MockBean
	private UserRepository userRepository;

	@MockBean
	private RoomRepository sessionRepository;

	@MockBean
	private ContentRepository contentRepository;

	@MockBean
	private ContentGroupRepository contentGroupRepository;

	@MockBean
	private AnswerRepository answerRepository;

	@MockBean
	private StatisticsRepository statisticsRepository;
}
