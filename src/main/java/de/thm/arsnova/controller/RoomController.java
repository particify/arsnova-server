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

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.RoomMembership;
import de.thm.arsnova.model.RoomStatistics;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.service.AccessTokenService;
import de.thm.arsnova.service.ContentGroupService;
import de.thm.arsnova.service.DataGenerationService;
import de.thm.arsnova.service.DuplicationService;
import de.thm.arsnova.service.RoomService;
import de.thm.arsnova.service.RoomStatisticsService;
import de.thm.arsnova.web.exceptions.ForbiddenException;
import de.thm.arsnova.web.exceptions.NotFoundException;

@RestController
@EntityRequestMapping(RoomController.REQUEST_MAPPING)
public class RoomController extends AbstractEntityController<Room> {
	protected static final String REQUEST_MAPPING = "/room";
	private static final String STATS_MAPPING = DEFAULT_ID_MAPPING + "/stats";
	private static final String PASSWORD_MAPPING = DEFAULT_ID_MAPPING + "/password";
	private static final String REQUEST_MEMBERSHIP_MAPPING = DEFAULT_ID_MAPPING + "/request-membership";
	private static final String DUPLICATE_MAPPING = DEFAULT_ID_MAPPING + "/duplicate";
	private static final String GENERATE_RANDOM_DATA_MAPPING = DEFAULT_ID_MAPPING + "/generate-random-data";

	private static final String ROOM_ROLE_HEADER = "ARS-Room-Role";

	private RoomService roomService;
	private ContentGroupService contentGroupService;
	private RoomStatisticsService roomStatisticsService;
	private DuplicationService duplicationService;
	private DataGenerationService dataGenerationService;
	private AccessTokenService accessTokenService;

	public RoomController(
			@Qualifier("securedRoomService") final RoomService roomService,
			@Qualifier("securedContentGroupService") final ContentGroupService contentGroupService,
			@Qualifier("securedRoomStatisticsService") final RoomStatisticsService roomStatisticsService,
			@Qualifier("securedDuplicationService") final DuplicationService duplicationService,
			@Qualifier("securedDataGenerationService") final DataGenerationService dataGenerationService,
			@Qualifier("securedAccessTokenService") final AccessTokenService accessTokenService) {
		super(roomService);
		this.roomService = roomService;
		this.contentGroupService = contentGroupService;
		this.roomStatisticsService = roomStatisticsService;
		this.duplicationService = duplicationService;
		this.dataGenerationService = dataGenerationService;
		this.accessTokenService = accessTokenService;
	}

	@Override
	protected String getMapping() {
		return REQUEST_MAPPING;
	}

	@Override
	protected String resolveAlias(final String shortId) {
		return roomService.getIdByShortId(shortId);
	}

	@GetMapping(STATS_MAPPING)
	public RoomStatistics getStats(
			@PathVariable final String id,
			@RequestParam(required = false) final String view) {
		final RoomStatistics roomStatistics = "read-extended".equals(view)
				? roomStatisticsService.getAllRoomStatistics(id)
				: roomStatisticsService.getPublicRoomStatistics(id);

		return roomStatistics;
	}

	@GetMapping(value = PASSWORD_MAPPING)
	public PasswordEntity getPassword(@PathVariable final String id) {
		final Room room = roomService.get(id);
		if (room == null) {
			throw new NotFoundException();
		}
		return new PasswordEntity(roomService.getPassword(room));
	}

	@PostMapping(value = PASSWORD_MAPPING)
	public void postPassword(
			@PathVariable final String id,
			@RequestBody final PasswordEntity passwordRequestEntity) {
		final Room room = roomService.get(id);
		if (room == null) {
			throw new NotFoundException();
		}
		roomService.setPassword(room, passwordRequestEntity.password);
	}

	@PostMapping(value = REQUEST_MEMBERSHIP_MAPPING)
	public Room requestMembership(
			@PathVariable final String id,
			@RequestBody final RequestMembershipRequestEntity requestMembershipRequestEntity,
			final HttpServletResponse httpServletResponse) {
		final Optional<RoomMembership> membership;
		if (requestMembershipRequestEntity.token != null) {
			membership = roomService.requestMembershipByToken(id, requestMembershipRequestEntity.token);
		} else {
			membership = roomService.requestMembership(
					id, requestMembershipRequestEntity.password != null ? requestMembershipRequestEntity.password : "");
		}
		membership.ifPresent(m -> {
			httpServletResponse.setHeader(ENTITY_ID_HEADER, m.getRoom().getId());
			httpServletResponse.setHeader(ENTITY_REVISION_HEADER, m.getRoom().getRevision());
			/* Sending of the role as a header is a temporary solution for
			 * now to allow accessing it without parsing the body. */
			httpServletResponse.setHeader(ROOM_ROLE_HEADER, m.getRole().toString());
		});

		return membership.orElseThrow(ForbiddenException::new).getRoom();
	}

	@PostMapping(DUPLICATE_MAPPING)
	public Room duplicateRoom(
			@PathVariable final String id,
			@RequestParam(defaultValue = "false") final boolean temporary,
			@RequestParam(defaultValue = "") final String name) {
		final Room room = roomService.get(id);
		if (room == null) {
			throw new NotFoundException();
		}
		return duplicationService.duplicateRoomCascading(room, temporary, name);
	}

	@PostMapping(GENERATE_RANDOM_DATA_MAPPING)
	public void generateRandomData(@PathVariable final String id) {
		final Room room = roomService.get(id);
		if (room == null) {
			throw new NotFoundException();
		}
		dataGenerationService.generateRandomAnswers(room);
	}

	private static class PasswordEntity {
		private String password;

		private PasswordEntity() {

		}

		private PasswordEntity(final String password) {
			this.password = password;
		}

		@JsonView(View.Public.class)
		public String getPassword() {
			return password;
		}

		@JsonView(View.Public.class)
		public void setPassword(final String password) {
			this.password = password;
		}
	}

	private static class RequestMembershipRequestEntity {
		private String password;
		private String token;

		@JsonView(View.Public.class)
		public void setPassword(final String password) {
			this.password = password;
		}

		@JsonView(View.Public.class)
		public void setToken(final String token) {
			this.token = token;
		}
	}
}
