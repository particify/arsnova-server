/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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
package de.thm.arsnova.services;

import de.thm.arsnova.entities.Motd;
import de.thm.arsnova.entities.MotdList;

import java.util.Date;
import java.util.List;

/**
 * The functionality the motd service should provide.
 */
public interface MotdService {
	Motd getByKey(String keyword);

	List<Motd> getAdminMotds();  //all w/o the sessionmotds

	List<Motd> getAllSessionMotds(final String sessionkey);

	List<Motd> getCurrentMotds(final Date clientdate, final String audience);

	List<Motd> getCurrentSessionMotds(final Date clientdate, final String sessionkey);

	List<Motd> filterMotdsByDate(List<Motd> list, Date clientdate);

	List<Motd> filterMotdsByList(List<Motd> list, MotdList motdList);

	void delete(Motd motd);

	void deleteBySessionKey(final String sessionkey, Motd motd);

	Motd save(Motd motd);

	Motd save(final String sessionkey, final Motd motd);

	Motd update(Motd motd);

	Motd update(final String sessionkey, Motd motd);

	MotdList getMotdListByUsername(final String username);

	MotdList saveMotdList(MotdList motdList);

	MotdList updateMotdList(MotdList motdList);
}
