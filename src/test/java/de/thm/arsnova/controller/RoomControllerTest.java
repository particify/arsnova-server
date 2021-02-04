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

package de.thm.arsnova.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.config.TestAppConfig;
import de.thm.arsnova.config.TestPersistanceConfig;
import de.thm.arsnova.config.TestSecurityConfig;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.ContentGroup;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.RoomStatistics;
import de.thm.arsnova.persistence.ContentGroupRepository;
import de.thm.arsnova.persistence.ContentRepository;
import de.thm.arsnova.persistence.RoomRepository;
import de.thm.arsnova.security.User;
import de.thm.arsnova.service.ContentGroupService;
import de.thm.arsnova.service.StubUserService;
import de.thm.arsnova.test.context.support.WithMockUser;

@SpringJUnitWebConfig({
		AppConfig.class,
		TestAppConfig.class,
		TestPersistanceConfig.class,
		TestSecurityConfig.class})
@ActiveProfiles("test")
public class RoomControllerTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private StubUserService stubUserService;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private ContentGroupRepository contentGroupRepository;

	@Autowired
	private ContentGroupService contentGroupService;

	@Autowired
	private ContentRepository contentRepository;

	private MockMvc mockMvc;
	private User user;

	@BeforeEach
	public void setup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
		Mockito.reset(roomRepository, contentGroupRepository, contentRepository);

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
	@WithMockUser(value = "TestUser", userId = "1234")
	public void shouldDeleteRoom() throws Exception {
		final Room room = getRoomForUserWithDatabaseDetails(user);

		when(roomRepository.findOne(room.getId())).thenReturn(room);

		mockMvc.perform(delete("/room/" + room.getId())
				.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string(emptyString()));
		verify(roomRepository).delete(argThat(r -> r.getId().equals(room.getId())));
	}

	@Test
	public void shouldReturnEmptyModeratorList() throws Exception {
		final Room room = getRoomForUserWithDatabaseDetails(user);

		when(roomRepository.findOne(room.getId())).thenReturn(room);

		mockMvc.perform(get("/room/" + room.getId() + "/moderator")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().string("[]"));
	}

	@Test
	@WithMockUser(value = "TestUser", userId = "1234")
	public void shouldAddModeratorForRoom() throws Exception {
		final Room room = getRoomForUserWithDatabaseDetails(user);
		final Room.Moderator moderator = createModerator();

		when(roomRepository.findOne(room.getId())).thenReturn(room);
		when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

		mockMvc.perform(put("/room/" + room.getId() + "/moderator/" + moderator.getUserId())
				.with(csrf())
				.content(new ObjectMapper().writeValueAsString(moderator))
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		verify(roomRepository).save(argThat(r -> r.getModerators().size() == 1));
	}

	@Test
	@WithMockUser(value = "TestUser", userId = "1234")
	public void shouldDeleteOneModerator() throws Exception {
		final Room room = getRoomForUserWithDatabaseDetails(user);
		final Set<Room.Moderator> moderatorList = createModerators(2);
		room.setModerators(moderatorList);
		final Iterator<Room.Moderator> iterator = room.getModerators().iterator();
		final Room.Moderator moderatorToDelete = iterator.next();

		when(roomRepository.findOne(room.getId())).thenReturn(room);
		when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

		mockMvc.perform(delete("/room/" + room.getId() + "/moderator/" + moderatorToDelete.getUserId())
				.with(csrf())
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().string(emptyString()));
		verify(roomRepository).save(argThat(r -> r.getModerators().size() == 1));
	}

	@Test
	@WithMockUser(value = "TestUser")
	public void shouldGetContentGroup() throws Exception {
		final Room room = new Room();
		room.setId("Test-RoomId");
		final ContentGroup contentGroup = new ContentGroup();
		contentGroup.setName("ContentGroupNameTest");

		when(roomRepository.findOne(room.getId())).thenReturn(room);
		when(contentGroupRepository.findByRoomIdAndName(room.getId(), contentGroup.getName())).thenReturn(contentGroup);
		mockMvc.perform(get("/room/" + room.getId() + "/contentgroup/" + contentGroup.getName())
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(contentGroup.getName())));
	}

	@Test
	@WithMockUser(value = "TestUser", userId = "1234")
	public void shouldCreateContentGroupWithContentWhenNoGroupExists() throws Exception {
		final Room room = this.getRoomForUserWithDatabaseDetails(user);
		when(roomRepository.findOne(room.getId())).thenReturn(room);

		final String contentId = "Test-ID-ContentGroup";
		final Content content = new Content();
		content.setRoomId(room.getId());
		when(contentRepository.findOne(contentId)).thenReturn(content);

		final String contentGroupName = "Test-ContentGroupName";
		when(contentGroupRepository.findByRoomIdAndName(room.getId(), contentGroupName)).thenReturn(null);
		when(contentGroupRepository.save(any(ContentGroup.class))).thenAnswer(i -> i.getArgument(0));

		mockMvc.perform(post("/room/" + room.getId() + "/contentgroup/" + contentGroupName + "/" + contentId)
				.with(csrf())
				.content(contentId)
				.contentType(MediaType.TEXT_PLAIN)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().string(emptyString()));
		verify(contentGroupRepository).save(argThat(containsContentGroupWithRoomIdAndContentIds(room.getId(), contentId)));
	}

	@Test
	@WithMockUser(value = "TestUser", userId = "1234")
	public void shouldAddContentIdToGroupWhenGroupAlreadyExists() throws Exception {
		final Room room = this.getRoomForUserWithDatabaseDetails(user);
		when(roomRepository.findOne(room.getId())).thenReturn(room);

		final String contentId1 = "pre-existing-content-id";
		final String contentId2 = "content-id-to-add";
		final Content content2 = new Content();
		content2.setRoomId(room.getId());
		when(contentRepository.findOne(contentId2)).thenReturn(content2);

		final ContentGroup contentGroup = createContentGroupWithRoomIdAndContentIds(room.getId(), contentId1);
		when(contentGroupRepository.findByRoomIdAndName(room.getId(), contentGroup.getName())).thenReturn(contentGroup);
		when(contentGroupRepository.findOne(any())).thenReturn(contentGroup);
		when(contentGroupRepository.save(any(ContentGroup.class))).thenAnswer(i -> i.getArgument(0));

		mockMvc.perform(post("/room/" + room.getId() + "/contentgroup/" + contentGroup.getName() + "/" + contentId2)
				.with(csrf())
				.content(contentId2)
				.contentType(MediaType.TEXT_PLAIN)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().string(emptyString()));
		verify(contentGroupRepository).save(argThat(
				containsContentGroupWithRoomIdAndContentIds(room.getId(), contentId1, contentId2)));
	}

	@Test
	@WithMockUser(value = "TestUser", userId = "1234")
	public void shouldUpdateContentGroupWhenContentIdsAreNotEmpty() throws Exception {
		final String contentId = "ID-Content-1";
		final Room room = this.getRoomForUserWithDatabaseDetails(user);
		final Content content = new Content();
		content.setId(contentId);
		content.setRoomId(room.getId());
		when(roomRepository.findOne(room.getId())).thenReturn(room);
		when(contentRepository.findOne(any())).thenReturn(content);
		when(contentRepository.findAllById(any())).thenReturn(Collections.singletonList(content));

		final ContentGroup contentGroup = createContentGroupWithRoomIdAndContentIds(room.getId(), contentId);

		when(contentGroupRepository.findOne(contentGroup.getId())).thenReturn(contentGroup);
		when(contentGroupRepository.save(any(ContentGroup.class))).thenAnswer(i -> i.getArgument(0));

		mockMvc.perform(put("/room/" + room.getId() + "/contentgroup/" + contentGroup.getName())
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(contentGroup))
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		verify(contentGroupRepository).save(argThat(containsContentGroupWithRoomIdAndContentIds(room.getId(), contentId)));
	}

	@Test
	@WithMockUser(value = "TestUser", userId = "1234")
	public void shouldDeleteContentGroupWhenGroupIsEmpty() throws Exception {
		final Room room = this.getRoomForUserWithDatabaseDetails(user);
		when(roomRepository.findOne(room.getId())).thenReturn(room);

		final ContentGroup contentGroup = new ContentGroup();
		contentGroup.setId("SOME_ID");
		contentGroup.setName("Test-ContentGroupName");
		contentGroup.setRoomId(room.getId());
		contentGroup.setContentIds(new LinkedHashSet<>()); // empty list of contents

		mockMvc.perform(put("/room/" + room.getId() + "/contentgroup/" + contentGroup.getName())
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(contentGroup))
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		verify(contentGroupRepository).delete(contentGroup);
	}

	@Test
	@WithMockUser(value = "TestUser")
	public void shouldGetStatsForRoom() throws Exception {
		final int groupSize = 3;
		final int contentSize = 2;
		final Room room = this.getRoomForUserWithDatabaseDetails(user);
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

	private ContentGroup createContentGroupWithRoomIdAndContentIds(final String roomId, final String... contentIds) {
		final ContentGroup contentGroup = new ContentGroup();
		contentGroup.setId("Test-ContentGroupId");
		contentGroup.setRevision("Test-ContentGroupRev");
		contentGroup.setName("Test-ContentGroupName");
		contentGroup.setContentIds(new LinkedHashSet<>(Arrays.asList(contentIds)));
		contentGroup.setRoomId(roomId);
		return contentGroup;
	}

	private List<ContentGroup> createContentGroupsWithContents(
			final String roomId, final int numberOfGroups, final int numberOfContents) {
		final List<ContentGroup> listOfGroups = new ArrayList<>();
		for (int i = 0; i < numberOfGroups; i++) {
			final ContentGroup contentGroup = new ContentGroup();
			contentGroup.setName("ContentGroupNameTest-" + (i + 1));
			contentGroup.setRoomId(roomId);
			contentGroup.setRevision("ContentGroupRevID");
			final Set<String> listOfContentsGroups = new LinkedHashSet<>();
			for (int ii = 0; ii < numberOfContents; ii++) {
				listOfContentsGroups.add("ID-Content-" + UUID.randomUUID());
			}
			contentGroup.setContentIds(listOfContentsGroups);
			listOfGroups.add(contentGroup);
		}
		return listOfGroups;
	}

	private ArgumentMatcher<ContentGroup> containsContentGroupWithRoomIdAndContentIds(
			final String roomId, final String... contentIds) {
		return a -> a.getRoomId().equals(roomId) && a.getContentIds().containsAll(Arrays.asList(contentIds));
	}

	private Set<Room.Moderator> createModerators(final int nb) {
		final Set<Room.Moderator> moderatorsList = new HashSet<>();
		for (int i = 0; i < nb; i++) {
			final Room.Moderator moderator = new Room.Moderator();
			moderator.setUserId("TestModerator-" + UUID.randomUUID().toString());
			final Set<Room.Moderator.Role> roles = new HashSet<>();
			roles.add(Room.Moderator.Role.EXECUTIVE_MODERATOR);
			moderator.setRoles(roles);
			moderatorsList.add(moderator);
		}
		return moderatorsList;
	}

	private Room.Moderator createModerator() {
		return createModerators(1).iterator().next();
	}

	private Room getRoomForUserWithDatabaseDetails(final User user) {
		final Room room = getRoomForUserWithoutDatabaseDetails(user);
		room.setId("Test-RoomID");
		room.setRevision("Test-rev");
		return room;
	}

	private Room getRoomForUserWithoutDatabaseDetails(final User user) {
		final Room room = new Room();
		room.setOwnerId(user.getId());
		room.setName("TestRoom");
		room.setAbbreviation("TR");
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
		room.setModerators(new HashSet<>()); // needed for moderatorsInitialized flag
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
