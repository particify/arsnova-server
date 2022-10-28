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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.model.ContentGroup;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.RoomStatistics;
import de.thm.arsnova.service.ContentGroupService;
import de.thm.arsnova.service.RoomService;
import de.thm.arsnova.web.exceptions.BadRequestException;

@RestController
@RequestMapping(RoomController.REQUEST_MAPPING)
public class RoomController extends AbstractEntityController<Room> {
	protected static final String REQUEST_MAPPING = "/room";
	private static final String GET_MODERATORS_MAPPING = DEFAULT_ID_MAPPING + "/moderator";
	private static final String MODERATOR_MAPPING = DEFAULT_ID_MAPPING + "/moderator/{userId}";
	private static final String STATS_MAPPING = DEFAULT_ID_MAPPING + "/stats";
	private static final String TRANSFER_MAPPING = DEFAULT_ID_MAPPING + "/transfer";

	private RoomService roomService;
	private ContentGroupService contentGroupService;

	public RoomController(
			@Qualifier("securedRoomService") final RoomService roomService,
			@Qualifier("securedContentGroupService") final ContentGroupService contentGroupService) {
		super(roomService);
		this.roomService = roomService;
		this.contentGroupService = contentGroupService;
	}

	@Override
	protected String getMapping() {
		return REQUEST_MAPPING;
	}

	@Override
	protected String resolveAlias(final String shortId) {
		return roomService.getIdByShortId(shortId);
	}

	@GetMapping(GET_MODERATORS_MAPPING)
	public Set<Room.Moderator> getModerators(@PathVariable final String id) {
		return roomService.get(id).getModerators();
	}

	@PutMapping(MODERATOR_MAPPING)
	public void putModerator(@PathVariable final String id, @PathVariable final String userId,
			@RequestBody final Room.Moderator moderator, final HttpServletResponse httpServletResponse) {
		final Room room = roomService.get(id);
		if (room.getOwnerId().equals(userId)) {
			throw new BadRequestException("Room owner cannot be added as moderator.");
		}
		moderator.setUserId(userId);
		if (moderator.getRoles().isEmpty()) {
			moderator.getRoles().add(Room.Moderator.Role.EXECUTIVE_MODERATOR);
		}
		room.getModerators().removeIf(m -> m.getUserId().equals(userId));
		room.getModerators().add(moderator);
		roomService.update(room);
		httpServletResponse.setHeader(ENTITY_ID_HEADER, room.getId());
		httpServletResponse.setHeader(ENTITY_REVISION_HEADER, room.getRevision());
	}

	@DeleteMapping(MODERATOR_MAPPING)
	public void deleteModerator(@PathVariable final String id, @PathVariable final String userId,
			final HttpServletResponse httpServletResponse) {
		final Room room = roomService.get(id);
		room.getModerators().removeIf(m -> m.getUserId().equals(userId));
		roomService.update(room);
		httpServletResponse.setHeader(ENTITY_ID_HEADER, room.getId());
		httpServletResponse.setHeader(ENTITY_REVISION_HEADER, room.getRevision());
	}

	@GetMapping(STATS_MAPPING)
	public RoomStatistics getStats(@PathVariable final String id) {
		final RoomStatistics roomStatistics = new RoomStatistics();
		final List<ContentGroup> contentGroups = contentGroupService.getByRoomId(id);
		roomStatistics.setGroupStats(contentGroups.stream()
				.map(cg ->  new RoomStatistics.ContentGroupStatistics(cg)).collect(Collectors.toList()));
		roomStatistics.setContentCount(contentGroups.stream()
				.mapToInt(cg -> cg.getContentIds().size()).reduce((a, b) -> a + b).orElse(0));

		return roomStatistics;
	}

	@PostMapping(value = TRANSFER_MAPPING, params = "newOwnerId")
	public Room transferOwnership(
			@PathVariable final String id,
			@RequestParam final String newOwnerId,
			final HttpServletResponse httpServletResponse) {
		final Room room = roomService.get(id);
		final Room updatedRoom = roomService.transferOwnership(room, newOwnerId);
		httpServletResponse.setHeader(ENTITY_ID_HEADER, room.getId());
		httpServletResponse.setHeader(ENTITY_REVISION_HEADER, room.getRevision());
		return updatedRoom;
	}

	@PostMapping(value = TRANSFER_MAPPING, params = "newOwnerToken")
	public Room transferOwnershipThroughToken(
			@PathVariable final String id,
			@RequestParam final String newOwnerToken,
			final HttpServletResponse httpServletResponse) {
		final Room room = roomService.get(id);
		final Room updatedRoom =  roomService.transferOwnershipThroughToken(room, newOwnerToken);
		httpServletResponse.setHeader(ENTITY_ID_HEADER, room.getId());
		httpServletResponse.setHeader(ENTITY_REVISION_HEADER, room.getRevision());
		return updatedRoom;
	}
}
