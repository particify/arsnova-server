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

package de.thm.arsnova.service;

import java.util.Date;
import java.util.List;

import de.thm.arsnova.model.Motd;

/**
 * The functionality the motd service should provide.
 */
public interface MotdService extends EntityService<Motd> {
	List<Motd> getAdminMotds();  //all w/o the sessionmotds

	List<Motd> getAllRoomMotds(String roomId);

	List<Motd> getCurrentMotds(Date clientdate, String audience);

	List<Motd> getCurrentRoomMotds(Date clientdate, String roomId);

	List<Motd> filterMotdsByDate(List<Motd> list, Date clientdate);

	List<Motd> filterMotdsByList(List<Motd> list, List<String> ids);

	Motd save(Motd motd);

	Motd save(String roomId, Motd motd);

	Motd update(Motd motd);

	Motd update(String roomId, Motd motd);
}
