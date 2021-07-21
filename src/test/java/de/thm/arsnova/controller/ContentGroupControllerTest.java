package de.thm.arsnova.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ActiveProfiles("test")
public class ContentGroupControllerTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private StubUserService stubUserService;

	@Autowired
	private ContentGroupService contentGroupService;

	@Autowired
	private ContentGroupRepository contentGroupRepository;

	@Autowired
	private RoomRepository roomRepository;

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
	@WithMockUser(value = "TestUser", userId = "1234")
	public void shouldGetContentGroup() throws Exception {
		final Room room = this.getRoomForUserWithDatabaseDetails("TestRoomID", user);
		final ContentGroup contentGroup = new ContentGroup();
		contentGroup.setId("Test-ID-ContentGroup");
		contentGroup.setName("ContentGroupNameTest");
		contentGroup.setRoomId(room.getId());
		contentGroup.setPublished(true);

		when(roomRepository.findOne(room.getId())).thenReturn(room);
		when(contentGroupRepository.findOne(contentGroup.getId())).thenReturn(contentGroup);
		mockMvc.perform(get("/contentgroup/" + contentGroup.getId())
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(contentGroup.getName())));
	}

	@Test
	@WithMockUser(value = "TestUser", userId = "1234", roles = {"USER", "OWNER__TestRoomID"})
	public void shouldCreateContentGroupWithContentWhenNoGroupExists() throws Exception {
		final Room room = this.getRoomForUserWithDatabaseDetails("TestRoomID", user);
		when(roomRepository.findOne(room.getId())).thenReturn(room);

		final String contentId = "Test-ID-ContentGroup";
		final Content content = new Content();
		content.setRoomId(room.getId());
		when(contentRepository.findOne(contentId)).thenReturn(content);

		final String contentGroupName = "Test-ContentGroupName";
		when(contentGroupRepository.findByRoomIdAndName(room.getId(), contentGroupName)).thenReturn(null);
		when(contentGroupRepository.save(any(ContentGroup.class))).thenAnswer(i -> i.getArgument(0));

		final ContentGroupController.AddContentToGroupRequestEntity requestBody =
				new ContentGroupController.AddContentToGroupRequestEntity();
		requestBody.setRoomId(room.getId());
		requestBody.setContentGroupName(contentGroupName);
		requestBody.setContentId(contentId);
		mockMvc.perform(post("/contentgroup/-/content/")
				.with(csrf())
				.content(new ObjectMapper().writeValueAsString(requestBody))
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().string(emptyString()));
		verify(contentGroupRepository).save(argThat(containsContentGroupWithRoomIdAndContentIds(room.getId(), contentId)));
	}

	@Test
	@WithMockUser(value = "TestUser", userId = "1234", roles = {"USER", "OWNER__TestRoomID"})
	public void shouldAddContentIdToGroupWhenGroupAlreadyExists() throws Exception {
		final Room room = this.getRoomForUserWithDatabaseDetails("TestRoomID", user);
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

		final ContentGroupController.AddContentToGroupRequestEntity requestBody =
				new ContentGroupController.AddContentToGroupRequestEntity();
		requestBody.setRoomId(room.getId());
		requestBody.setContentGroupName(contentGroup.getName());
		requestBody.setContentId(contentId2);
		mockMvc.perform(post("/contentgroup/-/content/")
				.with(csrf())
				.content(new ObjectMapper().writeValueAsString(requestBody))
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().string(emptyString()));
		verify(contentGroupRepository).save(argThat(
				containsContentGroupWithRoomIdAndContentIds(room.getId(), contentId1, contentId2)));
	}

	@Test
	@WithMockUser(value = "TestUser", userId = "1234", roles = {"USER", "OWNER__TestRoomID"})
	public void shouldUpdateContentGroupWhenContentIdsAreNotEmpty() throws Exception {
		final String contentId = "ID-Content-1";
		final Room room = this.getRoomForUserWithDatabaseDetails("TestRoomID", user);
		final Content content = new Content();
		content.setId(contentId);
		content.setRoomId(room.getId());
		when(roomRepository.findOne(room.getId())).thenReturn(room);
		when(contentRepository.findOne(any())).thenReturn(content);
		when(contentRepository.findAllById(any())).thenReturn(Collections.singletonList(content));

		final ContentGroup contentGroup = createContentGroupWithRoomIdAndContentIds(room.getId(), contentId);

		when(contentGroupRepository.findOne(contentGroup.getId())).thenReturn(contentGroup);
		when(contentGroupRepository.save(any(ContentGroup.class))).thenAnswer(i -> i.getArgument(0));

		mockMvc.perform(put("/contentgroup/" + contentGroup.getId())
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(contentGroup))
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		verify(contentGroupRepository).save(argThat(containsContentGroupWithRoomIdAndContentIds(room.getId(), contentId)));
	}

	private ContentGroup createContentGroupWithRoomIdAndContentIds(final String roomId, final String... contentIds) {
		final ContentGroup contentGroup = new ContentGroup();
		contentGroup.setId("Test-ContentGroupId");
		contentGroup.setRevision("Test-ContentGroupRev");
		contentGroup.setName("Test-ContentGroupName");
		contentGroup.setContentIds(Arrays.stream(contentIds).collect(Collectors.toList()));
		contentGroup.setRoomId(roomId);
		contentGroup.setPublished(true);
		return contentGroup;
	}

	private ArgumentMatcher<ContentGroup> containsContentGroupWithRoomIdAndContentIds(
			final String roomId, final String... contentIds) {
		return a -> a.getRoomId().equals(roomId) && a.getContentIds().containsAll(Arrays.asList(contentIds));
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
}
