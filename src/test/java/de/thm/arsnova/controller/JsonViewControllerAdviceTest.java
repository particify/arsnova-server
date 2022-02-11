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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import de.thm.arsnova.config.TestAppConfig;
import de.thm.arsnova.config.TestPersistanceConfig;
import de.thm.arsnova.config.TestSecurityConfig;
import de.thm.arsnova.model.Entity;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.service.EntityService;
import de.thm.arsnova.service.SecuredService;
import de.thm.arsnova.test.context.support.WithMockUser;

/**
 * This class tests {@link JsonViewControllerAdvice} which should be applied to
 * controllers which extend {@link AbstractEntityController}. It tests that the
 * correct {@link JsonView} is applied for serialization based on the
 * <code>view</code> query parameter and that unauthorized view requests are
 * rejected.
 *
 * @author Daniel Gerhardt
 */
@SpringBootTest
@Import({
		TestAppConfig.class,
		TestPersistanceConfig.class,
		TestSecurityConfig.class})
@ActiveProfiles({"test", "JsonViewControllerAdviceTest"})
public class JsonViewControllerAdviceTest {
	private static final Logger logger = LoggerFactory.getLogger(JsonViewControllerAdviceTest.class);

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@MockBean(extraInterfaces = SecuredService.class)
	private DummyEntityService dummyEntityService;

	@Autowired
	private DummyEntityController dummyEntityController;

	@BeforeEach
	public void setup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		when(dummyEntityService.get("1")).thenReturn(new DummyEntity());
	}

	@Test
	@WithMockUser("TestUser")
	public void testShouldNotSerializeAdminViewForRegularUser() throws Exception {
		logger.info("Auth: {}", SecurityContextHolder.getContext().getAuthentication());
		mockMvc.perform(get("/dummy/1?view=admin").accept(MediaType.APPLICATION_JSON))
				.andDo(h -> logger.info(h.getResponse().getContentAsString()))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(value = "TestAdmin", roles = {"USER", "ADMIN"})
	public void testShouldSerializeAdminViewForAdmin() throws Exception {
		logger.info("Auth: {}", SecurityContextHolder.getContext().getAuthentication());
		mockMvc.perform(get("/dummy/1?view=admin").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(h -> logger.info(h.getResponse().getContentAsString()))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(value = "TestAdmin", roles = {"USER", "ADMIN"})
	public void testShouldSerializeOwnerViewForAdmin() throws Exception {
		logger.info("Auth: {}", SecurityContextHolder.getContext().getAuthentication());
		mockMvc.perform(get("/dummy/1?view=owner").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(h -> logger.info(h.getResponse().getContentAsString()))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(value = "TestAdmin", roles = {"USER", "ADMIN"})
	public void testAdminViewShouldContainAdminProperties() throws Exception {
		logger.info("Auth: {}", SecurityContextHolder.getContext().getAuthentication());
		mockMvc.perform(get("/dummy/1?view=admin").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(h -> logger.info(h.getResponse().getContentAsString()))
				.andExpect(jsonPath("$.adminReadableString").exists())
				.andExpect(jsonPath("$.ownerReadableString").exists())
				.andExpect(jsonPath("$.publicReadableString").exists());
	}

	@Test
	@WithMockUser(value = "TestAdmin", roles = {"USER", "ADMIN"})
	public void testOwnerViewShouldContainOwnerProperties() throws Exception {
		logger.info("Auth: {}", SecurityContextHolder.getContext().getAuthentication());
		mockMvc.perform(get("/dummy/1?view=owner").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(h -> logger.info(h.getResponse().getContentAsString()))
				.andExpect(jsonPath("$.adminReadableString").doesNotExist())
				.andExpect(jsonPath("$.ownerReadableString").exists())
				.andExpect(jsonPath("$.publicReadableString").exists());
	}

	@Test
	@WithMockUser(value = "TestAdmin", roles = {"USER", "ADMIN"})
	public void testDefaultViewShouldContainPublicProperties() throws Exception {
		logger.info("Auth: {}", SecurityContextHolder.getContext().getAuthentication());
		mockMvc.perform(get("/dummy/1").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(h -> logger.info(h.getResponse().getContentAsString()))
				.andExpect(jsonPath("$.adminReadableString").doesNotExist())
				.andExpect(jsonPath("$.ownerReadableString").doesNotExist())
				.andExpect(jsonPath("$.publicReadableString").exists());
	}

	@RestController
	@RequestMapping(DummyEntityController.REQUEST_MAPPING)
	@Profile("JsonViewControllerAdviceTest")
	static class DummyEntityController extends AbstractEntityController<DummyEntity> {
		private static final String REQUEST_MAPPING = "/dummy";

		protected DummyEntityController(final DummyEntityService dummyEntityService) {
			super(dummyEntityService);
		}

		@Override
		protected String getMapping() {
			return REQUEST_MAPPING;
		}
	}

	static class DummyEntity extends Entity {
		@JsonView(View.Public.class) public String publicReadableString = "public";
		@JsonView(View.Owner.class) public String ownerReadableString = "owner";
		@JsonView(View.Admin.class) public String adminReadableString = "admin";
	}

	interface DummyEntityService extends EntityService<DummyEntity> {
	}
}
