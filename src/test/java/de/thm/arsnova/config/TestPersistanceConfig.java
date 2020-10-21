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

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import de.thm.arsnova.persistence.AnswerRepository;
import de.thm.arsnova.persistence.AttachmentRepository;
import de.thm.arsnova.persistence.CommentRepository;
import de.thm.arsnova.persistence.ContentGroupRepository;
import de.thm.arsnova.persistence.ContentRepository;
import de.thm.arsnova.persistence.LogEntryRepository;
import de.thm.arsnova.persistence.MotdRepository;
import de.thm.arsnova.persistence.RoomRepository;
import de.thm.arsnova.persistence.StatisticsRepository;
import de.thm.arsnova.persistence.UserRepository;

@Profile("test")
@Configuration
public class TestPersistanceConfig {
	@Bean
	public LogEntryRepository logEntryRepository() {
		return Mockito.mock(LogEntryRepository.class);
	}

	@Bean
	public UserRepository userRepository() {
		return Mockito.mock(UserRepository.class);
	}

	@Bean
	public RoomRepository sessionRepository() {
		return Mockito.mock(RoomRepository.class);
	}

	@Bean
	public CommentRepository commentRepository() {
		return Mockito.mock(CommentRepository.class);
	}

	@Bean
	public ContentRepository contentRepository() {
		return Mockito.mock(ContentRepository.class);
	}

	@Bean
	public ContentGroupRepository contentGroupRepository() {
		return Mockito.mock(ContentGroupRepository.class);
	}

	@Bean
	public AnswerRepository answerRepository() {
		return Mockito.mock(AnswerRepository.class);
	}

	@Bean
	public AttachmentRepository attachmentRepository() {
		return Mockito.mock(AttachmentRepository.class);
	}

	@Bean
	public MotdRepository motdRepository() {
		return Mockito.mock(MotdRepository.class);
	}

	@Bean
	public StatisticsRepository statisticsRepository() {
		return Mockito.mock(StatisticsRepository.class);
	}
}
