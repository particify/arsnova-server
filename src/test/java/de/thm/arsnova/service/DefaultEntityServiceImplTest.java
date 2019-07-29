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

package de.thm.arsnova.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.ValidationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.validation.Validator;

import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.config.TestAppConfig;
import de.thm.arsnova.config.TestPersistanceConfig;
import de.thm.arsnova.config.TestSecurityConfig;
import de.thm.arsnova.config.WebSocketConfig;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.persistence.RoomRepository;
import de.thm.arsnova.test.context.support.WithMockUser;


@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
		AppConfig.class,
		TestAppConfig.class,
		TestPersistanceConfig.class,
		TestSecurityConfig.class,
		WebSocketConfig.class})
@ActiveProfiles("test")
public class DefaultEntityServiceImplTest {
	private static final String SOME_TEXT = "SomeText";

	@Autowired
	@Qualifier("defaultJsonMessageConverter")
	private MappingJackson2HttpMessageConverter jackson2HttpMessageConverter;

	@Autowired
	private Validator validator;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private RoomRepository roomRepository;

	@Test
	@WithMockUser("TestUser")
	public void testPatch() throws IOException {
		final ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
		final DefaultEntityServiceImpl<Room> entityService =
				new DefaultEntityServiceImpl<>(Room.class, roomRepository, objectMapper, validator);
		entityService.setApplicationEventPublisher(eventPublisher);

		when(roomRepository.save(any(Room.class))).then(returnsFirstArg());

		final String originalId = "d8833f0d78964a9487ded02ba2dfbbad";
		final String originalName = "Test Room";
		final String originalOwnerId = "TestUser";
		final boolean originalActive = true;
		final Room room = new Room();
		prefillRoomFields(room);
		room.setId(originalId);
		room.setName(originalName);
		room.setClosed(originalActive);
		room.setOwnerId(originalOwnerId);

		final String patchedName = "Patched Room";
		final boolean patchedActive = false;
		final Map<String, Object> patchedValues = new HashMap<>();
		patchedValues.put("name", patchedName);
		patchedValues.put("closed", patchedActive);
		patchedValues.put("ownerId", "Should not be changeable.");

		entityService.patch(room, patchedValues);

		assertEquals(originalId, room.getId());
		assertEquals(patchedName, room.getName());
		assertEquals(patchedActive, room.isClosed());
		assertEquals(originalOwnerId, room.getOwnerId());
	}

	@Test
	@WithMockUser("TestUser")
	public void testPatchWithList() throws IOException {
		final ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
		final DefaultEntityServiceImpl<Room> entityService =
				new DefaultEntityServiceImpl<>(Room.class, roomRepository, objectMapper, validator);
		entityService.setApplicationEventPublisher(eventPublisher);

		when(roomRepository.saveAll(anyListOf(Room.class))).then(returnsFirstArg());

		final List<Room> sessions = new ArrayList<>();
		final String originalId1 = "d8833f0d78964a9487ded02ba2dfbbad";
		final String originalName1 = "Test Room 1";
		final String originalOwnerId1 = "TestUser";
		final boolean originalClosed1 = true;
		final Room room1 = new Room();
		prefillRoomFields(room1);
		room1.setId(originalId1);
		room1.setName(originalName1);
		room1.setClosed(originalClosed1);
		room1.setOwnerId(originalOwnerId1);
		sessions.add(room1);
		final String originalId2 = "3dc8cbff05da49d5980f6c001a6ea867";
		final String originalName2 = "Test Room 2";
		final String originalOwnerId2 = "TestUser";
		final boolean originalClosed2 = true;
		final Room room2 = new Room();
		prefillRoomFields(room2);
		room2.setId(originalId2);
		room2.setName(originalName2);
		room2.setClosed(originalClosed2);
		room2.setOwnerId(originalOwnerId2);
		sessions.add(room2);

		final String patchedName = "Patched Room";
		final boolean patchedClosed = false;
		final Map<String, Object> patchedValues = new HashMap<>();
		patchedValues.put("name", patchedName);
		patchedValues.put("closed", patchedClosed);
		patchedValues.put("ownerId", "Should not be changeable.");

		entityService.patch(sessions, patchedValues);

		assertEquals(originalId1, room1.getId());
		assertEquals(patchedName, room1.getName());
		assertEquals(patchedClosed, room1.isClosed());
		assertEquals(originalOwnerId1, room1.getOwnerId());
		assertEquals(originalId2, room2.getId());
		assertEquals(patchedName, room2.getName());
		assertEquals(patchedClosed, room2.isClosed());
		assertEquals(originalOwnerId2, room2.getOwnerId());
	}

	@Test
	@WithMockUser("TestUser")
	public void testCaching() {
		final ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
		final DefaultEntityServiceImpl<Room> entityService =
				new DefaultEntityServiceImpl<>(Room.class, roomRepository, objectMapper, validator);
		entityService.setApplicationEventPublisher(eventPublisher);

		final Room room1 = new Room();
		prefillRoomFields(room1);
		room1.setId("a34876427c634a9b9cb56789d73607f0");
		room1.setOwnerId("TestUser");
		final Room room2 = new Room();
		prefillRoomFields(room2);
		room2.setId("4638748d89884ff7936d7fe994a4090c");
		room2.setOwnerId("TestUser");
		final Room room3 = new Room();
		prefillRoomFields(room3);
		room3.setId("c9651db0a67b49789a354e90e0401032");
		room3.setOwnerId("TestUser");
		final Room room4 = new Room();
		prefillRoomFields(room4);
		room4.setId("66c1673056b2410b87335b9f317da5aa");
		room4.setOwnerId("TestUser");

		when(roomRepository.findById(any(String.class))).thenReturn(Optional.of(room1));
		when(roomRepository.findOne(any(String.class))).thenReturn(room1);
		assertSame(room1, entityService.get(room1.getId()));
		/* room1 should now be cached for room1.id */
		assertSame(room1, cacheManager.getCache("entity").get("room-" + room1.getId()).get());
		when(roomRepository.findById(any(String.class))).thenReturn(Optional.of(room2));
		when(roomRepository.findOne(any(String.class))).thenReturn(room2);
		assertSame(room1, entityService.get(room1.getId()));
		assertSame("Cache should not be used if internal == true.", room2, entityService.get(room1.getId(), true));

		entityService.delete(room1);
		/* room1 should no longer be cached for room1.id */
		assertSame("Entity should not be cached.", null, cacheManager.getCache("entity").get("room-" + room1.getId()));
		assertSame(room2, entityService.get(room1.getId()));
	}

	@Test(expected = ValidationException.class)
	@WithMockUser("TestUser")
	public void testValidation() {
		final ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
		final DefaultEntityServiceImpl<Room> entityService =
			new DefaultEntityServiceImpl<>(Room.class, roomRepository, objectMapper, validator);
		entityService.setApplicationEventPublisher(eventPublisher);

		when(roomRepository.save(any(Room.class))).then(returnsFirstArg());

		final Room room1 = new Room();
		room1.setOwnerId("TestUser");
		room1.setName("");

		entityService.create(room1);
	}

	private void prefillRoomFields(final Room room) {
		room.setName(SOME_TEXT);
		room.setAbbreviation(SOME_TEXT);
		room.setShortId("12345678");
	}
}
