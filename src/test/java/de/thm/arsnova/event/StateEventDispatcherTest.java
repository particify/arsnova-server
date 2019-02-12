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
package de.thm.arsnova.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.config.TestAppConfig;
import de.thm.arsnova.config.TestPersistanceConfig;
import de.thm.arsnova.config.TestSecurityConfig;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.persistence.ContentRepository;
import de.thm.arsnova.persistence.RoomRepository;
import de.thm.arsnova.service.DefaultEntityServiceImpl;
import de.thm.arsnova.test.context.support.WithMockUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
		AppConfig.class,
		TestAppConfig.class,
		TestPersistanceConfig.class,
		TestSecurityConfig.class})
@ActiveProfiles("test")
public class StateEventDispatcherTest {
	public static final String SETTINGS_PROPERTY_NAME = "settings";
	public static final String STATE_PROPERTY_NAME = "state";
	private static final String QUESTIONS_ENABLED_PROPERTY_NAME = "questionsEnabled";
	private static final String VISIBLE_PROPERTY_NAME = "visible";
	private static final String TEST_USER_ID = "TestUser";
	private static final String TEST_ROOM_ID = "TestRoom";

	@Autowired
	private EventListenerConfig eventListenerConfig;

	@Autowired
	@Qualifier("defaultJsonMessageConverter")
	private MappingJackson2HttpMessageConverter jackson2HttpMessageConverter;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private ContentRepository contentRepository;

	@Before
	public void prepare() {
		eventListenerConfig.resetEvents();
	}

	@Test
	@WithMockUser(TEST_USER_ID)
	public void testDispatchRoomSettingsStateEvent() throws IOException {
		final ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
		final DefaultEntityServiceImpl<Room> entityService = new DefaultEntityServiceImpl<>(
				Room.class, roomRepository, objectMapper);
		entityService.setApplicationEventPublisher(eventPublisher);

		when(roomRepository.save(any(Room.class))).then(returnsFirstArg());

		Room room = new Room();
		room.setOwnerId(TEST_USER_ID);
		entityService.patch(room, Collections.singletonMap(QUESTIONS_ENABLED_PROPERTY_NAME, false), Room::getSettings);
		assertEquals(1, eventListenerConfig.getRoomSettingsStateChangeEvents().size());
		assertEquals(SETTINGS_PROPERTY_NAME, eventListenerConfig.getRoomSettingsStateChangeEvents().get(0).getStateName());
	}

	@Test
	@WithMockUser(TEST_USER_ID)
	public void testDispatchContentStateEvent() throws IOException {
		final ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
		final DefaultEntityServiceImpl<Content> entityService = new DefaultEntityServiceImpl<>(
				Content.class, contentRepository, objectMapper);
		entityService.setApplicationEventPublisher(eventPublisher);

		Room room = new Room();
		room.setId(TEST_ROOM_ID);
		room.setOwnerId(TEST_USER_ID);
		when(contentRepository.save(any(Content.class))).then(returnsFirstArg());
		when(roomRepository.findOne(eq(room.getId()))).thenReturn(room);

		Content content = new Content();
		content.setRoomId(room.getId());
		entityService.patch(content, Collections.singletonMap(VISIBLE_PROPERTY_NAME, false), Content::getState);
		assertEquals(1, eventListenerConfig.getContentStateChangeEvents().size());
		assertEquals(STATE_PROPERTY_NAME, eventListenerConfig.getContentStateChangeEvents().get(0).getStateName());
	}

	@Configuration
	public static class EventListenerConfig {
		private List<StateChangeEvent<Room, Room.Settings>> roomSettingsStateChangeEvents = new ArrayList<>();
		private List<StateChangeEvent<Content, Content.State>> contentStateChangeEvents = new ArrayList<>();

		@EventListener(condition = "#event.stateName == '" + SETTINGS_PROPERTY_NAME + "'")
		public void handleRoomSettingsStateChangeEvent(StateChangeEvent<Room, Room.Settings> event) {
			roomSettingsStateChangeEvents.add(event);
		}

		@EventListener(condition = "#event.stateName == '" + STATE_PROPERTY_NAME + "'")
		public void handleContentStateChangeEvent(StateChangeEvent<Content, Content.State> event) {
			contentStateChangeEvents.add(event);
		}

		public List<StateChangeEvent<Room, Room.Settings>> getRoomSettingsStateChangeEvents() {
			return roomSettingsStateChangeEvents;
		}

		public List<StateChangeEvent<Content, Content.State>> getContentStateChangeEvents() {
			return contentStateChangeEvents;
		}

		public void resetEvents() {
			roomSettingsStateChangeEvents.clear();
			contentStateChangeEvents.clear();
		}
	}
}
