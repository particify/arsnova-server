/*
 * Copyright (C) 2012 THM webMedia
 * 
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.services;

import java.net.URLEncoder;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fourspaces.couchdb.Database;
import com.fourspaces.couchdb.View;
import com.fourspaces.couchdb.ViewResults;

import de.thm.arsnova.entities.Session;

@Service
public class SessionService implements ISessionService {

	private final com.fourspaces.couchdb.Session session = new com.fourspaces.couchdb.Session("localhost", 5984);
	public static final Logger logger = LoggerFactory
			.getLogger(SessionService.class);

	@Override
	public Session getSession(String keyword) {
		Database db = session.getDatabase("arsnova");
		View view = new View("session/by_keyword");
		view.setKey(URLEncoder.encode("\""+keyword+"\""));
		ViewResults results = db.view(view);

		if (results.getJSONArray("rows").optJSONObject(0) == null) return null;
		
		Session result = (Session)JSONObject.toBean(results.getJSONArray("rows").optJSONObject(0).optJSONObject("value"), Session.class);
		
		if (result.isActive()) return result;
		
		return null;
	}

}
