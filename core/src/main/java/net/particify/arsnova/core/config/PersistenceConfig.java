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

package net.particify.arsnova.core.config;

import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.spring.HttpClientFactoryBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import net.particify.arsnova.core.config.properties.CouchDbMainProperties;
import net.particify.arsnova.core.config.properties.CouchDbMigrationProperties;
import net.particify.arsnova.core.model.serialization.CouchDbObjectMapperFactory;
import net.particify.arsnova.core.persistence.AccessTokenRepository;
import net.particify.arsnova.core.persistence.AnnouncementRepository;
import net.particify.arsnova.core.persistence.AnswerRepository;
import net.particify.arsnova.core.persistence.ContentGroupRepository;
import net.particify.arsnova.core.persistence.ContentRepository;
import net.particify.arsnova.core.persistence.RoomRepository;
import net.particify.arsnova.core.persistence.StatisticsRepository;
import net.particify.arsnova.core.persistence.UserRepository;
import net.particify.arsnova.core.persistence.couchdb.CouchDbAccessTokenRepository;
import net.particify.arsnova.core.persistence.couchdb.CouchDbAnnouncementRepository;
import net.particify.arsnova.core.persistence.couchdb.CouchDbAnswerRepository;
import net.particify.arsnova.core.persistence.couchdb.CouchDbContentGroupRepository;
import net.particify.arsnova.core.persistence.couchdb.CouchDbContentRepository;
import net.particify.arsnova.core.persistence.couchdb.CouchDbRoomRepository;
import net.particify.arsnova.core.persistence.couchdb.CouchDbStatisticsRepository;
import net.particify.arsnova.core.persistence.couchdb.CouchDbUserRepository;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;
import net.particify.arsnova.core.persistence.couchdb.support.http.PatchedHttpClientFactoryBean;

@ComponentScan({
    "net.particify.arsnova.core.persistence.couchdb"
})
@Configuration
@EnableConfigurationProperties({
    CouchDbMainProperties.class,
    CouchDbMigrationProperties.class
})
@Profile("!test")
public class PersistenceConfig {
  private CouchDbMainProperties properties;

  public PersistenceConfig(
      final CouchDbMainProperties couchDbProperties) {
    this.properties = couchDbProperties;
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
  public StdCouchDbInstance couchDbInstance() throws Exception {
    return new StdCouchDbInstance(couchDbHttpClientFactory().getObject());
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
  public CouchDbObjectMapperFactory couchDbObjectMapperFactory() {
    return new CouchDbObjectMapperFactory();
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
