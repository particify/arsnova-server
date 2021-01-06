/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2021 The ARSnova Team and Contributors
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

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Motd;
import de.thm.arsnova.entities.MotdList;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
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
public class MotdService implements IMotdService {

	@Autowired
	private IDatabaseDao databaseDao;

	@Autowired
	private IUserService userService;

	@Autowired
	private ISessionService sessionService;

	public void setDatabaseDao(final IDatabaseDao databaseDao) {
		this.databaseDao = databaseDao;
	}

  @Override
  @PreAuthorize("isAuthenticated()")
  public Motd getMotd(final String key) {
    return databaseDao.getMotdByKey(key);
  }

  @Override
  @PreAuthorize("isAuthenticated() and hasPermission(1,'motd','admin')")
  public List<Motd> getAdminMotds() {
    return databaseDao.getAdminMotds();
  }

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#sessionkey, 'session', 'owner')")
	public List<Motd> getAllSessionMotds(final String sessionkey) {
		return databaseDao.getMotdsForSession(sessionkey);
	}

	@Override
	public List<Motd> getCurrentMotds(final Date clientdate, final String audience, final String sessionkey) {
		final List<Motd> motds;
		switch (audience) {
			case "all": motds = databaseDao.getMotdsForAll(); break;
			case "loggedIn": motds = databaseDao.getMotdsForLoggedIn(); break;
			case "students": motds = databaseDao.getMotdsForStudents(); break;
			case "tutors": motds = databaseDao.getMotdsForTutors(); break;
			case "session": motds = databaseDao.getMotdsForSession(sessionkey); break;
			default: motds = databaseDao.getMotdsForAll(); break;
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
	@PreAuthorize("isAuthenticated() and hasPermission(1,'motd','admin')")
	public Motd saveMotd(final Motd motd) {
		return createOrUpdateMotd(motd);
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#sessionkey, 'session', 'owner')")
	public Motd saveSessionMotd(final String sessionkey, final Motd motd) {
		Session session = sessionService.getSession(sessionkey);
		motd.setSessionId(session.get_id());


		return createOrUpdateMotd(motd);
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(1,'motd','admin')")
	public Motd updateMotd(final Motd motd) {
		return createOrUpdateMotd(motd);
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#sessionkey, 'session', 'owner')")
	public Motd updateSessionMotd(final String sessionkey, final Motd motd) {
		return createOrUpdateMotd(motd);
	}

	private Motd createOrUpdateMotd(final Motd motd) {
		if (motd.getMotdkey() != null) {
			Motd oldMotd = databaseDao.getMotdByKey(motd.getMotdkey());
			if (!(motd.get_id().equals(oldMotd.get_id()) && motd.getSessionkey().equals(oldMotd.getSessionkey())
					&& motd.getAudience().equals(oldMotd.getAudience()))) {
				throw new BadRequestException();
			}
		}

		return databaseDao.createOrUpdateMotd(motd);
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(1,'motd','admin')")
	public void deleteMotd(Motd motd) {
		databaseDao.deleteMotd(motd);
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#sessionkey, 'session', 'owner')")
	public void deleteSessionMotd(final String sessionkey, Motd motd) {
		databaseDao.deleteMotd(motd);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public MotdList getMotdListForUser(final String username) {
		final User user = userService.getCurrentUser();
		if (username.equals(user.getUsername()) && !"guest".equals(user.getType())) {
			return databaseDao.getMotdListForUser(username);
		}
		return null;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public MotdList saveUserMotdList(MotdList motdList) {
		final User user = userService.getCurrentUser();
		if (user.getUsername().equals(motdList.getUsername())) {
			return databaseDao.createOrUpdateMotdList(motdList);
		}
		return null;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public MotdList updateUserMotdList(MotdList motdList) {
		final User user = userService.getCurrentUser();
		if (user.getUsername().equals(motdList.getUsername())) {
			return databaseDao.createOrUpdateMotdList(motdList);
		}
		return null;
	}
}
