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

import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.spring.HttpClientFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import de.thm.arsnova.config.properties.CouchDbMainProperties;
import de.thm.arsnova.config.properties.CouchDbMigrationProperties;
import de.thm.arsnova.model.serialization.CouchDbObjectMapperFactory;
import de.thm.arsnova.persistence.AccessTokenRepository;
import de.thm.arsnova.persistence.AnnouncementRepository;
import de.thm.arsnova.persistence.AnswerRepository;
import de.thm.arsnova.persistence.ContentGroupRepository;
import de.thm.arsnova.persistence.ContentRepository;
import de.thm.arsnova.persistence.LogEntryRepository;
import de.thm.arsnova.persistence.RoomRepository;
import de.thm.arsnova.persistence.StatisticsRepository;
import de.thm.arsnova.persistence.UserRepository;
import de.thm.arsnova.persistence.couchdb.CouchDbAccessTokenRepository;
import de.thm.arsnova.persistence.couchdb.CouchDbAnnouncementRepository;
import de.thm.arsnova.persistence.couchdb.CouchDbAnswerRepository;
import de.thm.arsnova.persistence.couchdb.CouchDbContentGroupRepository;
import de.thm.arsnova.persistence.couchdb.CouchDbContentRepository;
import de.thm.arsnova.persistence.couchdb.CouchDbLogEntryRepository;
import de.thm.arsnova.persistence.couchdb.CouchDbRoomRepository;
import de.thm.arsnova.persistence.couchdb.CouchDbStatisticsRepository;
import de.thm.arsnova.persistence.couchdb.CouchDbUserRepository;
import de.thm.arsnova.persistence.couchdb.support.MangoCouchDbConnector;
import de.thm.arsnova.persistence.couchdb.support.http.PatchedHttpClientFactoryBean;

@ComponentScan({
		"de.thm.arsnova.persistence.couchdb"
})
@Configuration
@EnableConfigurationProperties({
		CouchDbMainProperties.class,
		CouchDbMigrationProperties.class
})
@Profile("!test")
public class PersistenceConfig {
	private static final int MIGRATION_SOCKET_TIMEOUT = 30000;

	private CouchDbMainProperties properties;
	private CouchDbMigrationProperties migrationProperties;

	public PersistenceConfig(
			final CouchDbMainProperties couchDbProperties,
			final CouchDbMigrationProperties migrationProperties) {
		this.properties = couchDbProperties;
		this.migrationProperties = migrationProperties;
	}

	@Bean
	@Primary
	public MangoCouchDbConnector couchDbConnector() throws Exception {
		final MangoCouchDbConnector connector = new MangoCouchDbConnector(
				properties.getDbName(), couchDbInstance(), couchDbObjectMapperFactory());
		if (properties.isCreateDb()) {
			connector.createDatabaseIfNotExists();
		}

		return connector;
	}

	@Bean
	@ConditionalOnProperty(
			name = "enabled",
			prefix = CouchDbMigrationProperties.PREFIX)
	public MangoCouchDbConnector couchDbMigrationConnector() throws Exception {
		return new MangoCouchDbConnector(
				migrationProperties.getDbName(),
				couchDbMigrationInstance(),
				couchDbObjectMapperFactory());
	}

	@Bean
	public StdCouchDbInstance couchDbInstance() throws Exception {
		return new StdCouchDbInstance(couchDbHttpClientFactory().getObject());
	}

	@Bean
	@ConditionalOnProperty(
			name = "enabled",
			prefix = CouchDbMigrationProperties.PREFIX)
	public StdCouchDbInstance couchDbMigrationInstance() throws Exception {
		return new StdCouchDbInstance(couchDbMigrationHttpClientFactory().getObject());
	}

	@Bean
	public HttpClientFactoryBean couchDbHttpClientFactory() throws Exception {
		final HttpClientFactoryBean factory = new PatchedHttpClientFactoryBean();
		factory.setHost(properties.getHost());
		factory.setPort(properties.getPort());
		if (!properties.getUsername().isEmpty()) {
			factory.setUsername(properties.getUsername());
			factory.setPassword(properties.getPassword());
		}

		return factory;
	}

	@Bean
	@ConditionalOnProperty(
			name = "enabled",
			prefix = CouchDbMigrationProperties.PREFIX)
	public HttpClientFactoryBean couchDbMigrationHttpClientFactory() throws Exception {
		final HttpClientFactoryBean factory = new PatchedHttpClientFactoryBean();
		factory.setHost(migrationProperties.getHost());
		factory.setPort(migrationProperties.getPort());
		if (!migrationProperties.getUsername().isEmpty()) {
			factory.setUsername(migrationProperties.getUsername());
			factory.setPassword(migrationProperties.getPassword());
		}
		factory.setSocketTimeout(MIGRATION_SOCKET_TIMEOUT);

		return factory;
	}

	@Bean
	public CouchDbObjectMapperFactory couchDbObjectMapperFactory() {
		return new CouchDbObjectMapperFactory();
	}

	@Bean
	public LogEntryRepository logEntryRepository() throws Exception {
		return new CouchDbLogEntryRepository(couchDbConnector(), false);
	}

	@Bean
	public UserRepository userRepository() throws Exception {
		return new CouchDbUserRepository(couchDbConnector(), false);
	}

	@Bean
	public RoomRepository sessionRepository() throws Exception {
		return new CouchDbRoomRepository(couchDbConnector(), false);
	}

	@Bean
	public ContentRepository contentRepository() throws Exception {
		return new CouchDbContentRepository(couchDbConnector(), false);
	}

	@Bean
	public ContentGroupRepository contentGroupRepository() throws Exception {
		return new CouchDbContentGroupRepository(couchDbConnector(), false);
	}

	@Bean
	public AnswerRepository answerRepository() throws Exception {
		return new CouchDbAnswerRepository(couchDbConnector(), false);
	}

	@Bean
	public AnnouncementRepository announcementRepository() throws Exception {
		return new CouchDbAnnouncementRepository(couchDbConnector(), false);
	}

	@Bean
	public AccessTokenRepository accessTokenRepository() throws Exception {
		return new CouchDbAccessTokenRepository(couchDbConnector(), false);
	}

	@Bean
	public StatisticsRepository statisticsRepository() throws Exception {
		return new CouchDbStatisticsRepository(couchDbConnector(), false);
	}
}
