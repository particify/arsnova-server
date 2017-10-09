package de.thm.arsnova.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.config.TestAppConfig;
import de.thm.arsnova.config.TestPersistanceConfig;
import de.thm.arsnova.config.TestSecurityConfig;
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.persistance.RoomRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.AdditionalAnswers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;


@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AppConfig.class, TestAppConfig.class, TestPersistanceConfig.class, TestSecurityConfig.class})
@ActiveProfiles("test")
public class DefaultEntityServiceImplTest {
	@Autowired
	@Qualifier("defaultJsonMessageConverter")
	private MappingJackson2HttpMessageConverter jackson2HttpMessageConverter;

	@Autowired
	private RoomRepository roomRepository;

	@Test
	@WithMockUser(username="TestUser")
	public void testPatch() throws IOException {
		final ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
		final DefaultEntityServiceImpl<Room> entityService = new DefaultEntityServiceImpl<>(Room.class, roomRepository, objectMapper);

		when(roomRepository.save(any(Room.class))).then(returnsFirstArg());

		final String originalId = "d8833f0d78964a9487ded02ba2dfbbad";
		final String originalName = "Test Room";
		final String originalOwnerId = "TestUser";
		final boolean originalActive = true;
		final Room room = new Room();
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
	@WithMockUser(username="TestUser")
	public void testPatchWithList() throws IOException {
		final ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
		final DefaultEntityServiceImpl<Room> entityService = new DefaultEntityServiceImpl<>(Room.class, roomRepository, objectMapper);

		when(roomRepository.save(any(Room.class))).then(returnsFirstArg());

		List<Room> sessions = new ArrayList<>();
		final String originalId1 = "d8833f0d78964a9487ded02ba2dfbbad";
		final String originalName1 = "Test Room 1";
		final String originalOwnerId1 = "TestUser";
		final boolean originalClosed1 = true;
		final Room room1 = new Room();
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
}
