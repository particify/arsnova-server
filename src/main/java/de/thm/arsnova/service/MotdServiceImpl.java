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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.	 If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import de.thm.arsnova.model.Motd;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.persistence.MotdRepository;
import de.thm.arsnova.web.exceptions.BadRequestException;

/**
 * Performs all question, interposed question, and answer related operations.
 */
@Service
public class MotdServiceImpl extends DefaultEntityServiceImpl<Motd> implements MotdService {
	private UserService userService;

	private RoomService roomService;

	private MotdRepository motdRepository;

	public MotdServiceImpl(
			final MotdRepository repository,
			final UserService userService,
			final RoomService roomService,
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
			final Validator validator) {
		super(Motd.class, repository, jackson2HttpMessageConverter.getObjectMapper(), validator);
		this.motdRepository = repository;
		this.userService = userService;
		this.roomService = roomService;
	}

	@Override
	public List<Motd> filterMotdsByDate(final List<Motd> list, final Date clientdate) {
		final List<Motd> returns = new ArrayList<>();
		for (final Motd motd : list) {
			if (motd.getStartDate().before(clientdate) && motd.getEndDate().after(clientdate)) {
				returns.add(motd);
			}
		}
		return returns;
	}

	@Override
	@PreAuthorize("hasPermission('', 'motd', 'admin')")
	public Motd save(final Motd motd) {
		return createOrUpdateMotd(motd);
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'owner')")
	public Motd save(final String roomId, final Motd motd) {
		final Room room = roomService.get(roomId);
		motd.setRoomId(room.getId());

		return createOrUpdateMotd(motd);
	}

	@Override
	@PreAuthorize("hasPermission('', 'motd', 'admin')")
	public Motd update(final Motd motd) {
		return createOrUpdateMotd(motd);
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'owner')")
	public Motd update(final String roomId, final Motd motd) {
		return createOrUpdateMotd(motd);
	}

	@CacheEvict(cacheNames = "motds", key = "#motd.audience + #motd.roomId")
	private Motd createOrUpdateMotd(final Motd motd) {
		if (motd.getId() != null) {
			final Motd oldMotd = get(motd.getId());
			if (!(motd.getId().equals(oldMotd.getId()) && motd.getRoomId().equals(oldMotd.getRoomId())
					&& motd.getAudience().equals(oldMotd.getAudience()))) {
				throw new BadRequestException();
			}
		}

		if (null != motd.getId()) {
			final Motd oldMotd = get(motd.getId());
			motd.setId(oldMotd.getId());

			return super.update(oldMotd, motd);
		}

		return super.create(motd);
	}
}
