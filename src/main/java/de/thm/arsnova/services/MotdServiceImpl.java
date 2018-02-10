/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
package de.thm.arsnova.services;

import de.thm.arsnova.entities.Motd;
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.persistance.MotdRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Performs all question, interposed question, and answer related operations.
 */
@Service
public class MotdServiceImpl extends DefaultEntityServiceImpl<Motd> implements MotdService {
	private UserService userService;

	private RoomService roomService;

	private MotdRepository motdRepository;

	public MotdServiceImpl(
			MotdRepository repository,
			UserService userService,
			RoomService roomService,
			@Qualifier("defaultJsonMessageConverter") MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
		super(Motd.class, repository, jackson2HttpMessageConverter.getObjectMapper());
		this.motdRepository = repository;
		this.userService = userService;
		this.roomService = roomService;
	}

	@Override
	@PreAuthorize("hasPermission('', 'motd', 'admin')")
	public List<Motd> getAdminMotds() {
    return motdRepository.findGlobalForAdmin();
  }

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'owner')")
	public List<Motd> getAllRoomMotds(final String roomId) {
		return motdRepository.findByRoomId(roomId);
	}

	@Override
	@Cacheable(cacheNames = "motds", key = "('session').concat(#roomId)")
	public List<Motd> getCurrentRoomMotds(final Date clientdate, final String roomId) {
		final List<Motd> motds = motdRepository.findByRoomId(roomId);
		return filterMotdsByDate(motds, clientdate);
	}

	@Override
	@Cacheable(cacheNames = "motds", key = "#audience")
	public List<Motd> getCurrentMotds(final Date clientdate, final String audience) {
		final List<Motd> motds;
		switch (audience) {
			case "all": motds = motdRepository.findGlobalForAll(); break;
			case "loggedIn": motds = motdRepository.findGlobalForLoggedIn(); break;
			case "students": motds = motdRepository.findForStudents(); break;
			case "tutors": motds = motdRepository.findGlobalForTutors(); break;
			default: throw new IllegalArgumentException("Invalid audience.");
		}

		return filterMotdsByDate(motds, clientdate);
	}

	@Override
	public List<Motd> filterMotdsByDate(List<Motd> list, Date clientdate) {
		List<Motd> returns = new ArrayList<>();
		for (Motd motd : list) {
			if (motd.getStartDate().before(clientdate) && motd.getEndDate().after(clientdate)) {
				returns.add(motd);
			}
		}
		return returns;
	}

	@Override
	public List<Motd> filterMotdsByList(List<Motd> list, List<String> ids) {
		return list.stream().filter(id -> ids.contains(id)).collect(Collectors.toList());
	}

	@Override
	@PreAuthorize("hasPermission('', 'motd', 'admin')")
	public Motd save(final Motd motd) {
		return createOrUpdateMotd(motd);
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'owner')")
	public Motd save(final String roomId, final Motd motd) {
		Room room = roomService.get(roomId);
		motd.setRoomId(room.getId());

		return createOrUpdateMotd(motd);
	}

	@Override
	@PreAuthorize("hasPermission(1,'motd','admin')")
	public Motd update(final Motd motd) {
		return createOrUpdateMotd(motd);
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'owner')")
	public Motd update(final String roomId, final Motd motd) {
		return createOrUpdateMotd(motd);
	}

	@CacheEvict(cacheNames = "motds", key = "#motd.audience.concat(#motd.roomId)")
	private Motd createOrUpdateMotd(final Motd motd) {
		if (motd.getId() != null) {
			Motd oldMotd = motdRepository.findOne(motd.getId());
			if (!(motd.getId().equals(oldMotd.getId()) && motd.getRoomId().equals(oldMotd.getRoomId())
					&& motd.getAudience().equals(oldMotd.getAudience()))) {
				throw new BadRequestException();
			}
		}

		if (null != motd.getId()) {
			Motd oldMotd = get(motd.getId());
			motd.setId(oldMotd.getId());
		}
		save(motd);

		return motdRepository.save(motd);
	}

	@Override
	@PreAuthorize("hasPermission('', 'motd', 'admin')")
	@CacheEvict(cacheNames = "motds", key = "#motd.audience.concat(#motd.roomId)")
	public void delete(Motd motd) {
		motdRepository.delete(motd);
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'owner')")
	public void deleteByRoomId(final String roomId, Motd motd) {
		motdRepository.delete(motd);
	}
}
