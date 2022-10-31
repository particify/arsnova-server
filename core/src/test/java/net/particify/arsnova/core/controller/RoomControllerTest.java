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

package net.particify.arsnova.core.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import net.particify.arsnova.core.config.TestAppConfig;
import net.particify.arsnova.core.config.TestPersistanceConfig;
import net.particify.arsnova.core.config.TestSecurityConfig;
import net.particify.arsnova.core.model.ContentGroup;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.RoomStatistics;
import net.particify.arsnova.core.persistence.ContentRepository;
import net.particify.arsnova.core.persistence.RoomRepository;
import net.particify.arsnova.core.security.User;
import net.particify.arsnova.core.service.ContentGroupService;
import net.particify.arsnova.core.service.StubUserService;
import net.particify.arsnova.core.test.context.support.WithMockUser;

@SpringBootTest
@Import({
    TestAppConfig.class,
    TestPersistanceConfig.class,
    TestSecurityConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ActiveProfiles("test")
public class RoomControllerTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private StubUserService stubUserService;

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private ContentGroupService contentGroupService;

  @Autowired
  private ContentRepository contentRepository;

  private MockMvc mockMvc;
  private User user;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    Mockito.reset(roomRepository, contentRepository);

    // Name and user need to match @WithMockUser annotation
    stubUserService.setUserAuthenticated(true, "TestUser", "1234");
    user = stubUserService.getCurrentUser();
  }

  @Test
  @WithMockUser("TestUser")
  public void shouldCreateRoom() throws Exception {
    final Room room = getRoomForUserWithoutDatabaseDetails(user);

    final String expectedRoomId = "TestId";
    final String expectedRoomRev = "TestRev";
    when(roomRepository.save(any(Room.class)))
        .thenAnswer(returnRoomWithDatabaseDetails(expectedRoomId, expectedRoomRev));

    mockMvc.perform(post("/room/")
        .content(new ObjectMapper().writeValueAsString(room))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(content().string(containsString(expectedRoomId)))
        .andExpect(content().string(containsString(expectedRoomRev)));
  }

  @Test
  @WithMockUser(value = "TestUser", userId = "1234", roles = {"USER", "OWNER__TestRoomID"})
  public void shouldDeleteRoom() throws Exception {
    final Room room = getRoomForUserWithDatabaseDetails("TestRoomID", user);

    when(roomRepository.findOne(room.getId())).thenReturn(room);

    mockMvc.perform(delete("/room/" + room.getId())
        .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string(emptyString()));
    verify(roomRepository).delete(argThat(r -> r.getId().equals(room.getId())));
  }

  @Test
  @WithMockUser(value = "TestUser")
  public void shouldGetStatsForRoom() throws Exception {
    final int groupSize = 3;
    final int contentSize = 2;
    final Room room = this.getRoomForUserWithDatabaseDetails("TestRoomID", user);
    final List<ContentGroup> listOfContentGroup = createContentGroupsWithContents(room.getId(), groupSize, contentSize);

    when(roomRepository.findOne(room.getId())).thenReturn(room);
    when(contentGroupService.getByRoomId(room.getId())).thenReturn(listOfContentGroup);

    final String requestBody = mockMvc.perform(get("/room/" + room.getId() + "/stats")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    final RoomStatistics stats = new ObjectMapper().readValue(requestBody, RoomStatistics.class);

    assertEquals(groupSize * contentSize, stats.getContentCount());
    assertEquals(groupSize, stats.getGroupStats().size());
    assertEquals(contentSize, stats.getGroupStats().get(0).getContentCount());
    assertEquals(listOfContentGroup.get(0).getName(), stats.getGroupStats().get(0).getGroupName());
  }

  private List<ContentGroup> createContentGroupsWithContents(
      final String roomId, final int numberOfGroups, final int numberOfContents) {
    final List<ContentGroup> listOfGroups = new ArrayList<>();
    for (int i = 0; i < numberOfGroups; i++) {
      final ContentGroup contentGroup = new ContentGroup();
      contentGroup.setName("ContentGroupNameTest-" + (i + 1));
      contentGroup.setRoomId(roomId);
      contentGroup.setRevision("ContentGroupRevID");
      contentGroup.setPublished(true);
      final List<String> listOfContentsGroups = new ArrayList<>();
      for (int ii = 0; ii < numberOfContents; ii++) {
        listOfContentsGroups.add("ID-Content-" + UUID.randomUUID());
      }
      contentGroup.setContentIds(listOfContentsGroups);
      listOfGroups.add(contentGroup);
    }
    return listOfGroups;
  }

  private Room getRoomForUserWithDatabaseDetails(final String roomId, final User user) {
    final Room room = getRoomForUserWithoutDatabaseDetails(user);
    room.setId(roomId);
    room.setRevision("Test-rev");
    return room;
  }

  private Room getRoomForUserWithoutDatabaseDetails(final User user) {
    final Room room = new Room();
    room.setOwnerId(user.getId());
    room.setName("TestRoom");
    room.setShortId("12345678");
    room.setExtensions(new HashMap<String, Map<String, Object>>() {
      {
        put("comments", new HashMap<String, Object>() {
          {
            put("enableModeration", true);
          }
        });
      }
    });
    return room;
  }

  private Answer<?> returnRoomWithDatabaseDetails(final String id, final String rev) {
    return (Answer<Room>) invocationOnMock -> {
      final Room r = invocationOnMock.getArgument(0, Room.class);
      r.setId(id);
      r.setRevision(rev);
      return r;
    };
  }
}
