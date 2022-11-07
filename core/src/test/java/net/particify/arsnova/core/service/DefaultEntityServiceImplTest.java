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

package net.particify.arsnova.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ValidationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.config.TestAppConfig;
import net.particify.arsnova.core.config.TestPersistanceConfig;
import net.particify.arsnova.core.config.TestSecurityConfig;
import net.particify.arsnova.core.event.AfterUpdateEvent;
import net.particify.arsnova.core.event.BeforeUpdateEvent;
import net.particify.arsnova.core.model.ContentGroup;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.persistence.ContentGroupRepository;
import net.particify.arsnova.core.persistence.RoomRepository;
import net.particify.arsnova.core.test.context.support.WithMockUser;

@SpringBootTest
@Import({
    TestAppConfig.class,
    TestPersistanceConfig.class,
    TestSecurityConfig.class})
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

  @Autowired
  private ContentGroupRepository contentGroupRepository;

  @Autowired
  private EventListenerConfig eventListenerConfig;

  @BeforeEach
  public void prepare() {
    eventListenerConfig.resetEvents();
  }

  @Test
  @WithMockUser("TestUser")
  public void testUpdateRetainsInternalProperties() {
    final ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
    final DefaultEntityServiceImpl<Room> entityService =
        new DefaultEntityServiceImpl<>(Room.class, roomRepository, objectMapper, validator);
    entityService.setApplicationEventPublisher(eventPublisher);

    when(roomRepository.save(any(Room.class))).then(returnsFirstArg());

    final String originalName = "Test Room 1";
    final String originalOwnerId = "TestUser";
    final String newOwnerId = "ShouldNotReplaceOwnerId";
    final Room room = new Room();
    prefillRoomFields(room);
    room.setName(originalName);
    room.setOwnerId(originalOwnerId);
    final Room persistedRoom = entityService.create(room);
    assertNotNull(persistedRoom.getCreationTimestamp());
    final Room roomUpdate = new Room();
    roomUpdate.setOwnerId(newOwnerId);
    prefillRoomFields(roomUpdate);
    final Room updatedRoom = entityService.update(persistedRoom, roomUpdate, View.Public.class);
    assertNotNull(updatedRoom.getCreationTimestamp());
    assertEquals(originalOwnerId, updatedRoom.getOwnerId());
  }

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

    entityService.patch(room, patchedValues, View.Public.class);

    assertEquals(originalId, room.getId());
    assertEquals(patchedName, room.getName());
    assertEquals(patchedActive, room.isClosed());
    assertEquals(originalOwnerId, room.getOwnerId());

    assertEquals(originalName, eventListenerConfig.getRoomBeforeUpdateEvents().get(0).getOldEntity().getName());
    assertEquals(originalName, eventListenerConfig.getRoomAfterUpdateEvents().get(0).getOldEntity().getName());
    assertEquals(room.getName(), eventListenerConfig.getRoomBeforeUpdateEvents().get(0).getEntity().getName());
    assertEquals(room.getName(), eventListenerConfig.getRoomAfterUpdateEvents().get(0).getEntity().getName());
  }

  @Test
  @WithMockUser("TestUser")
  public void testPatchWithList() throws IOException {
    final ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
    final DefaultEntityServiceImpl<Room> entityService =
        new DefaultEntityServiceImpl<>(Room.class, roomRepository, objectMapper, validator);
    entityService.setApplicationEventPublisher(eventPublisher);

    when(roomRepository.saveAll(anyList())).then(returnsFirstArg());

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

    entityService.patch(sessions, patchedValues, View.Public.class);

    assertEquals(originalId1, room1.getId());
    assertEquals(patchedName, room1.getName());
    assertEquals(patchedClosed, room1.isClosed());
    assertEquals(originalOwnerId1, room1.getOwnerId());
    assertEquals(originalId2, room2.getId());
    assertEquals(patchedName, room2.getName());
    assertEquals(patchedClosed, room2.isClosed());
    assertEquals(originalOwnerId2, room2.getOwnerId());

    assertEquals(originalName1, eventListenerConfig.getRoomBeforeUpdateEvents().get(0).getOldEntity().getName());
    assertEquals(originalName1, eventListenerConfig.getRoomAfterUpdateEvents().get(0).getOldEntity().getName());
    assertEquals(room1.getName(), eventListenerConfig.getRoomBeforeUpdateEvents().get(0).getEntity().getName());
    assertEquals(room1.getName(), eventListenerConfig.getRoomAfterUpdateEvents().get(0).getEntity().getName());
    assertEquals(originalName2, eventListenerConfig.getRoomBeforeUpdateEvents().get(1).getOldEntity().getName());
    assertEquals(originalName2, eventListenerConfig.getRoomAfterUpdateEvents().get(1).getOldEntity().getName());
    assertEquals(room2.getName(), eventListenerConfig.getRoomBeforeUpdateEvents().get(1).getEntity().getName());
    assertEquals(room2.getName(), eventListenerConfig.getRoomAfterUpdateEvents().get(1).getEntity().getName());
  }

  @Test
  @WithMockUser("TestUser")
  public void testPatchReplacesLists() throws IOException {
    final ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
    final DefaultEntityServiceImpl<ContentGroup> entityService =
        new DefaultEntityServiceImpl<>(ContentGroup.class, contentGroupRepository, objectMapper, validator);
    entityService.setApplicationEventPublisher(eventPublisher);

    when(contentGroupRepository.save(any(ContentGroup.class))).then(returnsFirstArg());

    final ContentGroup contentGroup = new ContentGroup();
    contentGroup.setId("ContentGroup1");
    contentGroup.setRoomId("RoomId1");
    contentGroup.setName("ContentGroupName");
    contentGroup.setContentIds(List.of("X", "Y", "A", "B"));
    final Map<String, Object> mapForPatching = Map.of(
        "name", "UpdatedName",
        "contentIds", List.of("A", "B", "C")
    );
    entityService.patch(contentGroup, mapForPatching);

    assertEquals(List.of("A", "B", "C"), contentGroup.getContentIds());
  }

  @Test
  @WithMockUser("TestUser")
  public void testPatchUpdatesListElementOrder() throws IOException {
    final ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
    final DefaultEntityServiceImpl<ContentGroup> entityService =
        new DefaultEntityServiceImpl<>(ContentGroup.class, contentGroupRepository, objectMapper, validator);
    entityService.setApplicationEventPublisher(eventPublisher);

    when(contentGroupRepository.save(any(ContentGroup.class))).then(returnsFirstArg());

    final ContentGroup contentGroup = new ContentGroup();
    contentGroup.setId("ContentGroup1");
    contentGroup.setRoomId("RoomId1");
    contentGroup.setName("ContentGroupName");
    contentGroup.setContentIds(List.of("C", "A", "B"));
    final Map<String, Object> mapForPatching = Map.of(
        "name", "UpdatedName",
        "contentIds", List.of("B", "C", "A")
    );
    entityService.patch(contentGroup, mapForPatching, View.Public.class);

    assertEquals(List.of("B", "C", "A"), contentGroup.getContentIds());
  }

  @Test
  @Disabled("Test breaks because of side effects from JsonViewControllerAdviceTest")
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
    final Cache cache = cacheManager.getCache("entity");
    assertNotNull(cache, "'entity' cache should not be null.");
    assertSame(room1, cache.get("room-" + room1.getId(), Room.class));
    when(roomRepository.findById(any(String.class))).thenReturn(Optional.of(room2));
    when(roomRepository.findOne(any(String.class))).thenReturn(room2);
    assertSame(room1, entityService.get(room1.getId()));
    assertSame(room2, entityService.get(room1.getId(), true), "Cache should not be used if internal == true.");

    entityService.delete(room1);
    /* room1 should no longer be cached for room1.id */
    assertSame(null, cacheManager.getCache("entity").get("room-" + room1.getId()), "Entity should not be cached.");
    assertSame(room2, entityService.get(room1.getId()));
  }

  @Test
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

    assertThrows(ValidationException.class, () -> entityService.create(room1));
  }

  @Test
  public void testChangeDetection() throws JsonProcessingException {
    final ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
    final DefaultEntityServiceImpl<Room> entityService =
        new DefaultEntityServiceImpl<>(Room.class, roomRepository, objectMapper, validator);
    entityService.setApplicationEventPublisher(eventPublisher);

    when(roomRepository.save(any(Room.class))).then(returnsFirstArg());

    final Room room1 = new Room();
    prefillRoomFields(room1);
    room1.setId("ID");
    room1.setOwnerId("TestUser");
    room1.setName("TestName");
    room1.getSettings().setFeedbackLocked(true);
    final Room room2 = new Room();
    prefillRoomFields(room2);
    room2.setId("ID");
    room2.setOwnerId("PrivatePropertyChange");
    room2.setName("ChangedTestName");
    room2.getSettings().setFeedbackLocked(false);
    final Map<String, Object> expectedChanges = Map.of(
        "name", room2.getName(),
        "settings", Map.of("feedbackLocked", room2.getSettings().isFeedbackLocked())
    );

    entityService.update(room1, room2, View.Public.class);
    final Map<String, Object> changes = eventListenerConfig.getRoomAfterUpdateEvents().get(0).getChanges();
    assertEquals(expectedChanges, changes);
  }

  private void prefillRoomFields(final Room room) {
    room.setName(SOME_TEXT);
    room.setShortId("12345678");
  }

  @Configuration
  public static class EventListenerConfig {
    private final List<BeforeUpdateEvent<Room>> roomBeforeUpdateEvents = new ArrayList<>();
    private final List<AfterUpdateEvent<Room>> roomAfterUpdateEvents = new ArrayList<>();

    @EventListener
    public void handleContentStateChangeEvent(final BeforeUpdateEvent<Room> event) {
      roomBeforeUpdateEvents.add(event);
    }

    @EventListener
    public void handleContentStateChangeEvent(final AfterUpdateEvent<Room> event) {
      roomAfterUpdateEvents.add(event);
    }

    public List<BeforeUpdateEvent<Room>> getRoomBeforeUpdateEvents() {
      return roomBeforeUpdateEvents;
    }

    public List<AfterUpdateEvent<Room>> getRoomAfterUpdateEvents() {
      return roomAfterUpdateEvents;
    }

    public void resetEvents() {
      roomBeforeUpdateEvents.clear();
      roomAfterUpdateEvents.clear();
    }
  }
}
