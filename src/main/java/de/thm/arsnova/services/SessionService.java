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
import java.util.ArrayList;
import java.util.List;

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
	private final Database database = session.getDatabase("arsnova");
	
	public static final Logger logger = LoggerFactory.getLogger(SessionService.class);
	
	@Override
	public Session getSession(String keyword) {
		View view = new View("session/by_keyword");
		view.setKey(URLEncoder.encode("\""+keyword+"\""));
		ViewResults results = database.view(view);

		if (results.getJSONArray("rows").optJSONObject(0) == null) return null;
		
		Session result = (Session)JSONObject.toBean(results.getJSONArray("rows").optJSONObject(0).optJSONObject("value"), Session.class);
		
		if (result.isActive()) return result;
		
		return null;
	}

	@Override
	public List<Integer> getFeedback(String keyword) {
		String sessionId = this.getSessionId(keyword);
		if (sessionId == null) return null;
		
		View view = new View("understanding/by_session");
		view.setGroup(true);
		view.setStartKey(URLEncoder.encode("[\""+sessionId+"\"]"));
		view.setEndKey(URLEncoder.encode("[\""+sessionId+"\"],{}"));
		ViewResults results = database.view(view);
		
		//if (results.getJSONArray("rows").optJSONObject(0) == null) return null;
		
		logger.info("Feedback: {}", results.getJSONObject("rows"));
		
		return null;
	}
	
	@Override
	public void postFeedback(String keyword, int value) {
				
	}
	
	private String getSessionId(String keyword) {
		View view = new View("session/by_keyword");
		view.setKey(URLEncoder.encode("\""+keyword+"\""));
		ViewResults results = database.view(view);

		if (results.getJSONArray("rows").optJSONObject(0) == null) return null;
		
		return results.getJSONArray("rows").optJSONObject(0).optJSONObject("value").getString("_id");
	}
	
}
