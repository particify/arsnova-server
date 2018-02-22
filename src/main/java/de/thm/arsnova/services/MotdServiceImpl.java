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
import de.thm.arsnova.entities.MotdList;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.persistance.MotdListRepository;
import de.thm.arsnova.persistance.MotdRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
/**
 * Performs all question, interposed question, and answer related operations.
 */
@Service
public class MotdServiceImpl extends DefaultEntityServiceImpl<Motd> implements MotdService {
	private UserService userService;

	private SessionService sessionService;

	private MotdRepository motdRepository;

	private MotdListRepository motdListRepository;

	public MotdServiceImpl(
			MotdRepository repository,
			MotdListRepository motdListRepository,
			UserService userService,
			SessionService sessionService,
			@Qualifier("defaultJsonMessageConverter") MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
		super(Motd.class, repository, jackson2HttpMessageConverter.getObjectMapper());
		this.motdRepository = repository;
		this.motdListRepository = motdListRepository;
		this.userService = userService;
		this.sessionService = sessionService;
	}

  @Override
  @PreAuthorize("isAuthenticated()")
  public Motd getByKey(final String key) {
    return motdRepository.findByKey(key);
  }

  @Override
  @PreAuthorize("hasPermission('', 'motd', 'admin')")
  public List<Motd> getAdminMotds() {
    return motdRepository.findGlobalForAdmin();
  }

	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'owner')")
	public List<Motd> getAllSessionMotds(final String sessionkey) {
		return motdRepository.findBySessionKey(sessionkey);
	}

	@Override
	@Cacheable(cacheNames = "motds", key = "('session').concat(#sessionkey)")
	public List<Motd> getCurrentSessionMotds(final Date clientdate, final String sessionkey) {
		final List<Motd> motds = motdRepository.findBySessionKey(sessionkey);
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
			if (motd.getStartdate().before(clientdate) && motd.getEnddate().after(clientdate)) {
				returns.add(motd);
			}
		}
		return returns;
  }

	@Override
	public List<Motd> filterMotdsByList(List<Motd> list, MotdList motdlist) {
		if (motdlist != null && motdlist.getMotdkeys() != null && !motdlist.getMotdkeys().isEmpty()) {
			List<Motd> returns = new ArrayList<>();
			HashSet<String> keys = new HashSet<>(500);  // Or a more realistic size
			StringTokenizer st = new StringTokenizer(motdlist.getMotdkeys(), ",");
			while (st.hasMoreTokens()) {
				keys.add(st.nextToken());
			}
			for (Motd motd : list) {
				if (!keys.contains(motd.getMotdkey())) {
					returns.add(motd);
				}
			}
			return returns;
		} else {
			return list;
		}
	}

	@Override
	@PreAuthorize("hasPermission('', 'motd', 'admin')")
	public Motd save(final Motd motd) {
		return createOrUpdateMotd(motd);
	}

	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'owner')")
	public Motd save(final String sessionkey, final Motd motd) {
		Session session = sessionService.getByKey(sessionkey);
		motd.setSessionId(session.getId());

		return createOrUpdateMotd(motd);
	}

	@Override
	@PreAuthorize("hasPermission(1,'motd','admin')")
	public Motd update(final Motd motd) {
		return createOrUpdateMotd(motd);
	}

	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'owner')")
	public Motd update(final String sessionkey, final Motd motd) {
		return createOrUpdateMotd(motd);
	}

	@CacheEvict(cacheNames = "motds", key = "#motd.audience.concat(#motd.sessionkey)")
	private Motd createOrUpdateMotd(final Motd motd) {
		if (motd.getMotdkey() != null) {
			Motd oldMotd = motdRepository.findByKey(motd.getMotdkey());
			if (!(motd.getId().equals(oldMotd.getId()) && motd.getSessionkey().equals(oldMotd.getSessionkey())
					&& motd.getAudience().equals(oldMotd.getAudience()))) {
				throw new BadRequestException();
			}
		}

		if (null != motd.getId()) {
			Motd oldMotd = get(motd.getId());
			motd.setMotdkey(oldMotd.getMotdkey());
		} else {
			motd.setMotdkey(sessionService.generateKey());
		}
		save(motd);

		return motdRepository.save(motd);
	}

	@Override
	@PreAuthorize("hasPermission('', 'motd', 'admin')")
	@CacheEvict(cacheNames = "motds", key = "#motd.audience.concat(#motd.sessionkey)")
	public void delete(Motd motd) {
		motdRepository.delete(motd);
	}

	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'owner')")
	public void deleteBySessionKey(final String sessionkey, Motd motd) {
		motdRepository.delete(motd);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@Cacheable(cacheNames = "motdlist", key = "#username")
	public MotdList getMotdListByUsername(final String username) {
		final User user = userService.getCurrentUser();
		if (username.equals(user.getUsername()) && !"guest".equals(user.getType())) {
			return motdListRepository.findByUsername(username);
		}
		return null;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@CachePut(cacheNames = "motdlist", key = "#motdList.username")
	public MotdList saveMotdList(MotdList motdList) {
		final User user = userService.getCurrentUser();
		if (user.getUsername().equals(motdList.getUsername())) {
			return motdListRepository.save(motdList);
		}
		return null;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public MotdList updateMotdList(MotdList motdList) {
		final User user = userService.getCurrentUser();
		if (user.getUsername().equals(motdList.getUsername())) {
			return motdListRepository.save(motdList);
		}
		return null;
	}
}
