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

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;

import net.particify.arsnova.core.config.properties.CouchDbMigrationProperties;
import net.particify.arsnova.core.persistence.AccessTokenRepository;
import net.particify.arsnova.core.persistence.AnnouncementRepository;
import net.particify.arsnova.core.persistence.AnswerRepository;
import net.particify.arsnova.core.persistence.ContentGroupRepository;
import net.particify.arsnova.core.persistence.ContentRepository;
import net.particify.arsnova.core.persistence.RoomRepository;
import net.particify.arsnova.core.persistence.StatisticsRepository;
import net.particify.arsnova.core.persistence.UserRepository;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

@TestConfiguration
@EnableConfigurationProperties(CouchDbMigrationProperties.class)
@Profile("test")
public class TestPersistanceConfig {
  @MockBean
  private MangoCouchDbConnector mangoCouchDbConnector;

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
  private AnnouncementRepository announcementRepository;

  @MockBean
  private StatisticsRepository statisticsRepository;

  @MockBean
  public AccessTokenRepository accessTokenRepository;
}
